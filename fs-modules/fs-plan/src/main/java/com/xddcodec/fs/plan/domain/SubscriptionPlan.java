package com.xddcodec.fs.plan.domain;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xddcodec.fs.framework.orm.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 套餐表实体
 *
 * @Author: xddcodec
 * @Date: 2025/9/24 11:24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table("subscription_plan")
public class SubscriptionPlan extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 套餐代码
     */
    private String planCode;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 存储配额(GB)
     */
    private Integer storageQuotaGb;

    /**
     * 最大文件数
     */
    private Integer maxFiles;

    /**
     * 单个文件最大大小(字节)
     */
    private Long maxFileSize;

    /**
     * 每月带宽配额(字节)
     */
    private Long bandwidthQuota;

    /**
     * 价格/月
     */
    private Double price;

    /**
     * 是否启用0否1是
     */
    private Integer isActive;

    /**
     * 是否为默认套餐 0否1是
     */
    private Integer isDefault;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 删除标记 0-未删除 1-已删除
     */
    @Column(isLogicDelete = true)
    private Integer delFlag;
}
