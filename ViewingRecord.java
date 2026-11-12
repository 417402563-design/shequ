package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 看房记录
 */
@Data
@Entity
public class ViewingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private String tenantName;

    private Long houseId;

    private String houseTitle;

    private LocalDateTime viewTime;

    /** 看房反馈 */
    @Column(length = 1000)
    private String feedback;

    /** 意向：1有意向 0无意向 */
    private Integer intention = 1;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
