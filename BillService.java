package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.common.ApiException;
import cn.edu.sdua.shequ.entity.Bill;
import cn.edu.sdua.shequ.entity.Contract;
import cn.edu.sdua.shequ.repository.BillRepository;
import cn.edu.sdua.shequ.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用管理服务（租金收取记录、押金处理、账单生成与推送）
 */
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    public List<Bill> list(String status, String type, Long tenantId) {
        List<Bill> bills = billRepository.findAll();
        if (status != null && !status.isBlank()) {
            bills = bills.stream().filter(b -> status.equals(b.getStatus())).toList();
        }
        if (type != null && !type.isBlank()) {
            bills = bills.stream().filter(b -> type.equals(b.getType())).toList();
        }
        if (tenantId != null) {
            bills = bills.stream().filter(b -> tenantId.equals(b.getTenantId())).toList();
        }
        return bills;
    }

    public Bill create(Bill bill) {
        Bill saved = billRepository.save(bill);
        notificationService.push(saved.getTenantName(), "新账单提醒",
                String.format("您有一笔新的%s账单：金额%s元，缴费截止日%s，请及时缴纳。",
                        typeLabel(saved.getType()), saved.getAmount(), saved.getDueDate()));
        return saved;
    }

    /** 租金收取记录：标记账单已缴费 */
    public Bill pay(Long id) {
        Bill bill = billRepository.findById(id).orElseThrow(() -> new ApiException("账单不存在"));
        if ("PAID".equals(bill.getStatus())) {
            throw new ApiException("该账单已缴费，请勿重复操作");
        }
        bill.setStatus("PAID");
        bill.setPayTime(LocalDateTime.now());
        Bill saved = billRepository.save(bill);
        contractRepository.findById(bill.getContractId()).ifPresent(c ->
                notificationService.push(c.getLandlordName(), "租金到账通知",
                        String.format("租客%s已缴纳%s账单 %s元（合同%s）。",
                                bill.getTenantName(), typeLabel(bill.getType()), bill.getAmount(), c.getContractNo())));
        return saved;
    }

    /** 押金处理：退还/扣除 */
    public Bill handleDeposit(Long contractId, String handle) {
        return billRepository.findByContractId(contractId).stream()
                .filter(b -> "DEPOSIT".equals(b.getType()))
                .findFirst()
                .map(b -> {
                    b.setDepositHandle(handle);
                    return billRepository.save(b);
                })
                .orElse(null);
    }

    /** 合同退租时作废未缴租金账单 */
    public void cancelUnpaidRent(Long contractId) {
        billRepository.findByContractId(contractId).stream()
                .filter(b -> "RENT".equals(b.getType()) && "UNPAID".equals(b.getStatus()))
                .forEach(b -> {
                    b.setStatus("CANCELLED");
                    b.setRemark("合同提前终止，账单作废");
                    billRepository.save(b);
                });
    }

    public void delete(Long id) {
        billRepository.deleteById(id);
    }

    /** 合同签约后按付款周期自动生成押金账单与分期租金账单，并推送给租客 */
    public void generateForContract(Contract c) {
        if (billRepository.findByContractId(c.getId()).stream().anyMatch(b -> "RENT".equals(b.getType()))) {
            return;
        }
        int cycle = c.getPayCycle() == null || c.getPayCycle() <= 0 ? 1 : c.getPayCycle();
        long totalMonths = LocalDate.parse(c.getStartDate().toString())
                .until(c.getEndDate(), java.time.temporal.ChronoUnit.MONTHS);
        int periods = (int) Math.max(1, Math.ceil(totalMonths / (double) cycle));

        // 押金账单
        if (c.getDeposit() != null && c.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
            Bill deposit = newBill(c, "DEPOSIT", c.getDeposit(), c.getStartDate());
            deposit.setRemark("签约押金，退租时按约定处理");
            billRepository.save(deposit);
        }
        // 分期租金账单
        BigDecimal amount = c.getRent().multiply(BigDecimal.valueOf(cycle));
        for (int i = 0; i < periods; i++) {
            LocalDate due = c.getStartDate().plusMonths((long) i * cycle);
            Bill rent = newBill(c, "RENT", amount, due);
            rent.setRemark(String.format("第%d期租金（%d个月）", i + 1, cycle));
            billRepository.save(rent);
        }
        notificationService.push(c.getTenantName(), "租金账单已推送",
                String.format("合同[%s]签约成功，已生成%d期租金账单，请按付款周期及时缴费。",
                        c.getContractNo(), periods));
    }

    /** 按生效合同手动重新生成账单 */
    public void generateByContractId(Long contractId) {
        Contract c = contractRepository.findById(contractId).orElseThrow(() -> new ApiException("合同不存在"));
        if (!"SIGNED".equals(c.getStatus())) {
            throw new ApiException("仅生效中的合同可生成账单");
        }
        if (billRepository.findByContractId(contractId).stream().anyMatch(b -> "RENT".equals(b.getType()))) {
            throw new ApiException("该合同已生成过租金账单，请勿重复生成");
        }
        generateForContract(c);
    }

    private Bill newBill(Contract c, String type, BigDecimal amount, LocalDate dueDate) {
        Bill b = new Bill();
        b.setContractId(c.getId());
        b.setContractNo(c.getContractNo());
        b.setTenantId(c.getTenantId());
        b.setTenantName(c.getTenantName());
        b.setType(type);
        b.setAmount(amount);
        b.setDueDate(dueDate);
        b.setStatus("UNPAID");
        return b;
    }

    private static String typeLabel(String type) {
        if (type == null) return "费用";
        return switch (type) {
            case "RENT" -> "租金";
            case "DEPOSIT" -> "押金";
            case "WATER" -> "水费";
            case "ELECTRIC" -> "电费";
            case "PROPERTY" -> "物业费";
            default -> "费用";
        };
    }
}
