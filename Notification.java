package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知（账单推送、到期提醒等）
 */
@Data
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收人姓名 */
    private String target;

    private String title;

    @Column(length = 2000)
    private String content;

    /** 是否已读 0否 1是 */
    private Integer readFlag = 0;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
