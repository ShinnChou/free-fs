package com.xddcodec.fs.plan.domain.qry;

import com.xddcodec.fs.framework.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 套餐分页查询对象
 *
 * @Author: xddcodec
 * @Date: 2025/9/24 13:39
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SubscriptionPlanPageQry extends PageQuery {

    /**
     * 套餐名称
     */
    private String planName;
}
