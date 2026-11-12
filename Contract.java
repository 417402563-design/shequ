package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁合同
 */
@Data
@Entity
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 合同编号 */
    @Column(unique = true)
    private String contractNo;

    private Long houseId;

    private String houseAddress;

    private Long tenantId;

    private String tenantName;

    private Long landlordId;

    private String landlordName;

    /** 中介经办人 */
    private String agentName;

    private LocalDate startDate;

    private LocalDate endDate;

    /** 月租金 */
    private BigDecimal rent;

    /** 押金 */
    private BigDecimal deposit;

    /** 付款周期（月）：1月付/3季付/6半年付/12年付 */
    private Integer payCycle = 1;

    /** 状态：DRAFT待签约 / SIGNED生效中 / EXPIRED已到期 / TERMINATED已退租 */
    private String status = "DRAFT";

    /** 签约方式：ONLINE电子签约 / OFFLINE线下签约 */
    private String signMethod;

    private LocalDateTime signTime;

    /** 是否已发送到期提醒 0否 1是 */
    private Integer reminded = 0;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
