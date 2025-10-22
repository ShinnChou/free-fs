package com.xddcodec.fs.plan.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xddcodec.fs.framework.common.utils.DateUtils;
import com.xddcodec.fs.plan.domain.SubscriptionPlan;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套餐分页VO对象
 *
 * @Author: xddcodec
 * @Date: 2025/9/24 13:36
 */
@Data
@AutoMapper(target = SubscriptionPlan.class)
@Schema(description = "套餐")
public class SubscriptionPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 789644388977160676L;

    @Schema(description = "套餐ID")
    private Long id;

    @Schema(description = "套餐代码")
    private String planCode;

    @Schema(description = "套餐名称")
    private String planName;

    @Schema(description = "套餐描述")
    private String description;

    @Schema(description = "存储配额(GB)")
    private Integer storageQuotaGb;

    @Schema(description = "最大文件数量")
    private Integer maxFiles;

    @Schema(description = "最大文件大小(GB)")
    private Long maxFileSize;

    @Schema(description = "带宽配额(GB/月)")
    private Long bandwidthQuota;

    @Schema(description = "套餐价格(元)")
    private Double price;

    @Schema(description = "是否为激活套餐 0否1是")
    private Integer isActive;


    @Schema(description = "是否为默认套餐 0否1是")
    private Integer isDefault;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtils.DATE_TIME_PATTERN)
    private LocalDateTime updatedAt;
}
