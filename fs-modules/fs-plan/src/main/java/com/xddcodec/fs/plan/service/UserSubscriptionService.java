package com.xddcodec.fs.plan.service;

import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.plan.domain.UserSubscription;

import java.util.List;

/**
 * 用户套餐订阅表Service
 *
 * @Author: xddcodec
 * @Date: 2025/9/28 13:57
 */
public interface UserSubscriptionService extends IService<UserSubscription> {

    /**
     * 根据套餐ID获取订阅信息
     *
     * @param planId
     * @return
     */
    List<UserSubscription> getListByPlanId(Long planId);

    /**
     * 根据套餐ID，判断当前套餐是否还存在未过期的订阅记录
     *
     * @param planId
     * @return
     */
    boolean hasSubscription(Long planId);
}
