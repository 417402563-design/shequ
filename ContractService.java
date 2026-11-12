package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.common.ApiException;
import cn.edu.sdua.shequ.entity.Contract;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.entity.Tenant;
import cn.edu.sdua.shequ.repository.ContractRepository;
import cn.edu.sdua.shequ.repository.HouseRepository;
import cn.edu.sdua.shequ.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 合同管理服务（模板生成、电子签约、到期提醒查询）
 */
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final HouseRepository houseRepository;
    private final TenantRepository tenantRepository;
    private final BillService billService;
    private final NotificationService notificationService;

    public List<Contract> list(String status, String keyword) {
        List<Contract> list = contractRepository.findAll();
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(c -> status.equals(c.getStatus())).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            list = list.stream().filter(c ->
                    (c.getContractNo() != null && c.getContractNo().contains(kw))
                            || (c.getTenantName() != null && c.getTenantName().contains(kw))
                            || (c.getHouseAddress() != null && c.getHouseAddress().contains(kw))).toList();
        }
        return list;
    }

    /** 依据房屋/租客生成待签合同（合同模板生成） */
    public Contract createDraft(Contract contract) {
        House house = houseRepository.findById(contract.getHouseId())
                .orElseThrow(() -> new ApiException("房源不存在"));
        Tenant tenant = tenantRepository.findById(contract.getTenantId())
                .orElseThrow(() -> new ApiException("租客不存在"));
        if ("RENTED".equals(house.getStatus())) {
            throw new ApiException("该房源已被出租，无法重复签约");
        }
        contract.setContractNo(generateNo());
        contract.setHouseAddress(house.getAddress());
        contract.setTenantName(tenant.getName());
        contract.setLandlordId(house.getOwnerId());
        contract.setLandlordName(house.getOwnerName());
        if (contract.getRent() == null) contract.setRent(house.getRent());
        if (contract.getDeposit() == null) contract.setDeposit(house.getDeposit());
        contract.setStatus("DRAFT");
        return contractRepository.save(contract);
    }

    /** 渲染合同模板全文 */
    public String renderTemplate(Long id) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new ApiException("合同不存在"));
        String cycleText = switch (c.getPayCycle() == null ? 1 : c.getPayCycle()) {
            case 3 -> "季付（每3个月）";
            case 6 -> "半年付（每6个月）";
            case 12 -> "年付（每12个月）";
            default -> "月付";
        };
        return """
                房屋租赁合同（编号：%s）

                出租方（甲方）：%s
                承租方（乙方）：%s
                中介经办人：%s

                根据《中华人民共和国民法典》有关规定，甲、乙双方在自愿、平等、互利的基础上，经协商一致，就甲方将其合法拥有的房屋出租给乙方使用，乙方承租使用甲方房屋事宜，订立本合同。

                一、租赁房屋坐落于：%s。

                二、租赁期限：自 %s 起至 %s 止，共 %d 个月。

                三、租金及支付方式：月租金人民币 %s 元，押金人民币 %s 元；租金按%s结算，乙方应于每期到期前缴纳下期租金。

                四、相关费用：租赁期内水费、电费、燃气费、物业费、网络费等由乙方按实际使用承担。

                五、房屋使用：乙方不得擅自改变房屋结构及用途，不得转租、转借；爱护房屋及附属设施，如有损坏应照价赔偿。

                六、合同解除：任何一方提前解除合同，须提前30日书面通知对方，并按月租金的100%%向对方支付违约金。

                七、到期与续租：合同到期前30日，双方可协商续租事宜；乙方逾期未搬离的，甲方有权按双倍日租金收取房屋占用费。

                八、争议解决：本合同履行过程中发生争议，双方应友好协商解决；协商不成的，可向房屋所在地人民法院提起诉讼。

                九、本合同一式两份，甲乙双方各执一份，自双方签字（电子签约）之日起生效。

                甲方（签字）：____________        乙方（签字）：____________
                签订日期：______年______月______日
                """.formatted(
                c.getContractNo(),
                nullToEmpty(c.getLandlordName()),
                nullToEmpty(c.getTenantName()),
                nullToEmpty(c.getAgentName()),
                nullToEmpty(c.getHouseAddress()),
                c.getStartDate(), c.getEndDate(),
                java.time.temporal.ChronoUnit.MONTHS.between(c.getStartDate(), c.getEndDate()),
                str(c.getRent()), str(c.getDeposit()), cycleText);
    }

    /**
     * 电子签约集成：模拟第三方电子签章平台完成签约。
     * 签约成功后：房源自动转为已出租，并按付款周期自动生成租金/押金账单推送给租客。
     */
    public Contract sign(Long id, String signMethod) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new ApiException("合同不存在"));
        if (!"DRAFT".equals(c.getStatus())) {
            throw new ApiException("仅待签约状态的合同可以执行签约，当前状态：" + c.getStatus());
        }
        c.setStatus("SIGNED");
        c.setSignMethod(signMethod == null ? "ONLINE" : signMethod);
        c.setSignTime(LocalDateTime.now());
        contractRepository.save(c);
        // 房源状态联动更新
        houseRepository.findById(c.getHouseId()).ifPresent(house -> {
            house.setStatus("RENTED");
            houseRepository.save(house);
        });
        // 自动生成账单并推送
        billService.generateForContract(c);
        notificationService.push(c.getLandlordName(), "合同签约成功",
                "合同[" + c.getContractNo() + "]已完成电子签约，租金账单已自动推送给租客" + c.getTenantName() + "。");
        return c;
    }

    /** 提前退租：合同终止、房源恢复可租、未缴账单作废、押金处理提醒 */
    public Contract terminate(Long id, String depositHandle) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new ApiException("合同不存在"));
        if (!"SIGNED".equals(c.getStatus())) {
            throw new ApiException("仅生效中的合同可以终止，当前状态：" + c.getStatus());
        }
        c.setStatus("TERMINATED");
        contractRepository.save(c);
        houseRepository.findById(c.getHouseId()).ifPresent(house -> {
            house.setStatus("ON");
            houseRepository.save(house);
        });
        billService.cancelUnpaidRent(c.getId());
        billService.handleDeposit(c.getId(), depositHandle);
        notificationService.push(c.getTenantName(), "合同已终止",
                "合同[" + c.getContractNo() + "]已办理退租，押金处理方式为：" + depositLabel(depositHandle) + "。");
        return c;
    }

    /** 到期提醒查询：30天内即将到期且仍生效的合同 */
    public List<Contract> expiringWithin(int days) {
        LocalDate today = LocalDate.now();
        return contractRepository.findByStatus("SIGNED").stream()
                .filter(c -> !c.getEndDate().isBefore(today) && !c.getEndDate().isAfter(today.plusDays(days)))
                .toList();
    }

    public void delete(Long id) {
        Contract c = contractRepository.findById(id).orElseThrow(() -> new ApiException("合同不存在"));
        if ("SIGNED".equals(c.getStatus())) {
            throw new ApiException("生效中的合同不可删除，请先办理退租");
        }
        contractRepository.deleteById(id);
    }

    private String generateNo() {
        return "HT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private static String depositLabel(String handle) {
        if ("DEDUCTED".equals(handle)) return "扣除";
        if ("FORFEIT".equals(handle)) return "抵扣违约金";
        return "全额退还";
    }

    private static String str(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
