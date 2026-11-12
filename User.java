package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（中介/房东/租客账号）
 */
@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private String name;

    /** 角色：AGENT中介 / LANDLORD房东 / TENANT租客 */
    private String role;

    private String phone;

    private String email;

    /** 状态：1启用 0禁用 */
    private Integer status = 1;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
