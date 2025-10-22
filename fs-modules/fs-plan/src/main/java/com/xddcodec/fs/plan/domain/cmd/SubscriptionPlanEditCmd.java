package com.xddcodec.fs.plan.domain.cmd;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 套餐编辑对象
 *
 * @Author: xddcodec
 * @Date: 2025/9/28 14:54
 */
@Data
public class SubscriptionPlanEditCmd {

    /**
     * 套餐id
     */
    @NotNull(message = "planId不能为空")
    private Long id;

    /**
     * 套餐代码
     */
    @NotBlank(message = "planCode不能为空")
    private String planCode;

    /**
     * 套餐名称
     */
    @NotBlank(message = "planName不能为空")
    private String planName;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 存储配额(GB)
     */
    @NotNull(message = "storageQuotaGb不能为空")
    private Integer storageQuotaGb;

    /**
     * 最大文件数
     */
    @NotNull(message = "maxFiles不能为空")
    private Integer maxFiles;

    /**
     * 单个文件最大大小(字节)
     */
    @NotNull(message = "maxFileSize不能为空")
    private Long maxFileSize;

    /**
     * 每月带宽配额(字节)
     */
    @NotNull(message = "bandwidthQuota不能为空")
    private Long bandwidthQuota;

    /**
     * 价格/月
     */
    @NotNull(message = "price不能为空")
    private Double price;

    /**
     * 是否启用0否1是
     */
    @NotNull(message = "isActive不能为空")
    @Min(value = 0, message = "isActive只能是0或1")
    @Max(value = 1, message = "isActive只能是0或1")
    private Integer isActive;

    /**
     * 是否为默认套餐 0否1是
     */
    @NotNull(message = "isDefault不能为空")
    @Min(value = 0, message = "isActive只能是0或1")
    @Max(value = 1, message = "isActive只能是0或1")
    private Integer isDefault;

    /**
     * 排序
     */
    @NotNull(message = "sortOrder不能为空")
    private Integer sortOrder;
}
