package cn.edu.sdua.shequ.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源信息
 */
@Data
@Entity
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 房源标题 */
    private String title;

    /** 详细地址 */
    private String address;

    /** 所属区域 */
    private String area;

    /** 户型，如 三室两厅 */
    private String layout;

    private Integer floor;

    private Integer totalFloors;

    /** 月租金 */
    private BigDecimal rent;

    /** 押金 */
    private BigDecimal deposit;

    /** 状态：ON出租中(空置可租) / RENTED已出租 / OFF已下架 */
    private String status = "ON";

    /** 房东用户ID */
    private Long ownerId;

    /** 房东姓名（冗余，便于展示） */
    private String ownerName;

    /** 配套设施 */
    private String facilities;

    /** 房源描述 */
    @Column(length = 2000)
    private String description;

    /** 浏览量（热门区域分析依据） */
    private Integer viewCount = 0;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }
}
