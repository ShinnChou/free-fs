package com.xddcodec.fs.plan.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户套餐订阅表实体
 *
 * @Author: xddcodec
 * @Date: 2025/9/28 13:49
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table("user_subscription")
public class UserSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 订阅状态 0-生效中，1-已过期
     */
    private Integer status;

    /**
     * 订阅时间
     */
    private LocalDateTime subscriptionDate;

    /**
     * 订阅到期时间
     */
    private LocalDateTime expireDate;
}
