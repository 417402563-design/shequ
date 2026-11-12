package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租客档案（信息登记与信用评估）
 */
@Data
@Entity
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String gender;

    /** 身份证号 */
    private String idCard;

    private String phone;

    private Integer age;

    private String occupation;

    /** 信用分 0-100 */
    private Integer creditScore;

    /** 信用等级：优秀/良好/一般/较差（依据信用分自动评定） */
    private String creditLevel;

    @Column(length = 1000)
    private String remark;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (creditScore != null) {
            if (creditScore >= 90) creditLevel = "优秀";
            else if (creditScore >= 75) creditLevel = "良好";
            else if (creditScore >= 60) creditLevel = "一般";
            else creditLevel = "较差";
        }
    }
}
