package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用账单（租金/押金/水电气/物业费）
 */
@Data
@Entity
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contractId;

    private String contractNo;

    private Long tenantId;

    private String tenantName;

    /** 类型：RENT租金 / DEPOSIT押金 / WATER水费 / ELECTRIC电费 / PROPERTY物业费 */
    private String type;

    private BigDecimal amount;

    /** 缴费到期日 */
    private LocalDate dueDate;

    /** 状态：UNPAID待缴费 / PAID已缴费 */
    private String status = "UNPAID";

    /** 押金处理：REFUNDED已退还 / DEDUCTED已扣除 */
    private String depositHandle;

    private LocalDateTime payTime;

    @Column(length = 1000)
    private String remark;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
