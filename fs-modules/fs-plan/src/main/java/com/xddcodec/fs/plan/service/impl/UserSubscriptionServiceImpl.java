package com.xddcodec.fs.plan.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.plan.domain.UserSubscription;
import com.xddcodec.fs.plan.mapper.UserSubscriptionMapper;
import com.xddcodec.fs.plan.service.UserSubscriptionService;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.xddcodec.fs.plan.domain.table.UserSubscriptionTableDef.USER_SUBSCRIPTION;

/**
 * 用户套餐订阅表Service Impl
 *
 * @Author: xddcodec
 * @Date: 2025/9/28 13:57
 */
@Service
public class UserSubscriptionServiceImpl extends ServiceImpl<UserSubscriptionMapper, UserSubscription> implements UserSubscriptionService {

    @Override
    public List<UserSubscription> getListByPlanId(Long planId) {
        return this.list(new QueryWrapper()
                .where(USER_SUBSCRIPTION.PLAN_ID.eq(planId))
        );
    }

    @Override
    public boolean hasSubscription(Long planId) {
        List<UserSubscription> list = this.getListByPlanId(planId);
        return CollUtil.isNotEmpty(list) &&
               list.stream().anyMatch(sub -> sub.getStatus() == 0);
    }
}
