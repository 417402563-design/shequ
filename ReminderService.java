package cn.edu.sdua.shequ.service;

import cn.edu.sdua.shequ.entity.Contract;
import cn.edu.sdua.shequ.entity.House;
import cn.edu.sdua.shequ.repository.ContractRepository;
import cn.edu.sdua.shequ.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 合同到期提醒定时任务：
 * 1. 已过期合同自动置为“已到期”，房源恢复为可租；
 * 2. 30天内到期合同向房东/租客推送提醒（仅提醒一次）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ContractRepository contractRepository;
    private final HouseRepository houseRepository;
    private final NotificationService notificationService;

    /** 每小时执行一次检查，应用启动10秒后先执行一次 */
    @Scheduled(fixedDelay = 3600_000, initialDelay = 10_000)
    public void checkContracts() {
        LocalDate today = LocalDate.now();
        for (Contract c : contractRepository.findByStatus("SIGNED")) {
            if (c.getEndDate().isBefore(today)) {
                c.setStatus("EXPIRED");
                contractRepository.save(c);
                houseRepository.findById(c.getHouseId())
                        .filter(h -> "RENTED".equals(h.getStatus()))
                        .ifPresent(h -> {
                            h.setStatus("ON");
                            houseRepository.save(h);
                        });
                notificationService.push(c.getLandlordName(), "合同到期通知",
                        "合同[" + c.getContractNo() + "]已于 " + c.getEndDate() + " 到期，房源("
                                + c.getHouseAddress() + ")已自动恢复为可租状态，请及时联系租客办理续租或退租。");
                notificationService.push(c.getTenantName(), "合同到期通知",
                        "您承租的房源(" + c.getHouseAddress() + ")租期已于 " + c.getEndDate() + " 届满，如需续租请联系房东或中介。");
                log.info("合同已自动置为到期：{}", c.getContractNo());
            } else if (!c.getEndDate().isAfter(today.plusDays(30))
                    && (c.getReminded() == null || c.getReminded() == 0)) {
                long left = java.time.temporal.ChronoUnit.DAYS.between(today, c.getEndDate());
                String msg = "合同[" + c.getContractNo() + "]将于 " + c.getEndDate() + " 到期（剩余" + left + "天），请及时协商续租事宜。";
                notificationService.push(c.getLandlordName(), "合同即将到期提醒", msg);
                notificationService.push(c.getTenantName(), "合同即将到期提醒", msg);
                c.setReminded(1);
                contractRepository.save(c);
                log.info("发送到期提醒：{}", c.getContractNo());
            }
        }
    }
}
