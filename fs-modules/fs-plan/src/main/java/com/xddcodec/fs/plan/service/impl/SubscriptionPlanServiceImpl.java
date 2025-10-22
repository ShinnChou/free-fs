package com.xddcodec.fs.plan.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.plan.domain.SubscriptionPlan;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanAddCmd;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanEditCmd;
import com.xddcodec.fs.plan.domain.qry.SubscriptionPlanPageQry;
import com.xddcodec.fs.plan.domain.vo.SubscriptionPlanVO;
import com.xddcodec.fs.plan.mapper.SubscriptionPlanMapper;
import com.xddcodec.fs.plan.service.SubscriptionPlanService;
import com.xddcodec.fs.plan.service.UserSubscriptionService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.xddcodec.fs.plan.domain.table.SubscriptionPlanTableDef.SUBSCRIPTION_PLAN;

/**
 * 套餐表业务服务实现类
 *
 * @Author: xddcodec
 * @Date: 2025/9/24 11:26
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl extends ServiceImpl<SubscriptionPlanMapper, SubscriptionPlan> implements SubscriptionPlanService {

    private final Converter converter;

    private final UserSubscriptionService userSubscriptionService;

    @Override
    public PageResult<SubscriptionPlanVO> getPages(SubscriptionPlanPageQry pageQry) {
        Page<SubscriptionPlan> page = new Page<>(pageQry.getPage(), pageQry.getPageSize());
        QueryWrapper queryWrapper = new QueryWrapper();
        if (StringUtils.isNotEmpty(pageQry.getPlanName())) {
            queryWrapper.where(SUBSCRIPTION_PLAN.PLAN_NAME.eq(pageQry.getPlanName()));
        }
        queryWrapper.orderBy(SUBSCRIPTION_PLAN.SORT_ORDER.asc());
        this.page(page, queryWrapper);
        Long total = page.getTotalRow();
        List<SubscriptionPlan> records = page.getRecords();
        List<SubscriptionPlanVO> roleVOS = converter.convert(records, SubscriptionPlanVO.class);
        return PageResult.success(roleVOS, total);
    }

    @Override
    public SubscriptionPlan getByCode(String planCode) {
        return this.getOne(
                new QueryWrapper()
                        .where(SUBSCRIPTION_PLAN.PLAN_CODE.eq(planCode))
        );
    }

    @Override
    public SubscriptionPlan getByName(String planName) {
        return this.getOne(
                new QueryWrapper()
                        .where(SUBSCRIPTION_PLAN.PLAN_NAME.eq(planName))
        );
    }

    @Override
    public SubscriptionPlan getDefaultPlan() {
        return this.getOne(
                new QueryWrapper()
                        .where(SUBSCRIPTION_PLAN.IS_DEFAULT.eq(1))
        );
    }

    @Override
    public SubscriptionPlanVO getDetail(Long planId) {
        SubscriptionPlan subscriptionPlan = this.getById(planId);
        return converter.convert(subscriptionPlan, SubscriptionPlanVO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addSubscriptionPlan(SubscriptionPlanAddCmd cmd) {
        SubscriptionPlan existCode = getByCode(cmd.getPlanCode());
        if (existCode != null) {
            throw new BusinessException("当前套餐编码已存在！");
        }
        SubscriptionPlan existName = getByName(cmd.getPlanName());
        if (existName != null) {
            throw new BusinessException("当前套餐名称已存在！");
        }
        SubscriptionPlan sp = new SubscriptionPlan();
        sp.setPlanCode(cmd.getPlanCode());
        sp.setPlanName(cmd.getPlanName());
        sp.setDescription(cmd.getDescription());
        sp.setStorageQuotaGb(cmd.getStorageQuotaGb());
        sp.setMaxFiles(cmd.getMaxFiles());
        sp.setMaxFileSize(cmd.getMaxFileSize());
        sp.setBandwidthQuota(cmd.getBandwidthQuota());
        sp.setPrice(cmd.getPrice());
        sp.setIsActive(cmd.getIsActive());
        sp.setIsDefault(cmd.getIsDefault());
        sp.setSortOrder(cmd.getSortOrder());
        sp.setCreatedAt(LocalDateTime.now());
        sp.setUpdatedAt(LocalDateTime.now());
        this.save(sp);
    }

    @Override
    public void editSubscriptionPlan(SubscriptionPlanEditCmd cmd) {
        SubscriptionPlan sp = this.getById(cmd.getId());
        if (sp == null) {
            throw new BusinessException("当前套餐不存在！");
        }
        if (!sp.getPlanCode().equals(cmd.getPlanCode())) {
            SubscriptionPlan existCode = getByCode(cmd.getPlanCode());
            if (existCode != null) {
                throw new BusinessException("当前套餐编码已存在！");
            }
        }
        if (!sp.getPlanName().equals(cmd.getPlanName())) {
            SubscriptionPlan existName = getByName(cmd.getPlanName());
            if (existName != null) {
                throw new BusinessException("当前套餐名称已存在！");
            }
        }
        sp.setPlanCode(cmd.getPlanCode());
        sp.setPlanName(cmd.getPlanName());
        sp.setDescription(cmd.getDescription());
        sp.setStorageQuotaGb(cmd.getStorageQuotaGb());
        sp.setMaxFiles(cmd.getMaxFiles());
        sp.setMaxFileSize(cmd.getMaxFileSize());
        sp.setBandwidthQuota(cmd.getBandwidthQuota());
        sp.setPrice(cmd.getPrice());
        sp.setIsActive(cmd.getIsActive());
        sp.setIsDefault(cmd.getIsDefault());
        sp.setSortOrder(cmd.getSortOrder());
        sp.setUpdatedAt(LocalDateTime.now());
        this.updateById(sp);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeSubscriptionPlan(Long planId) {
        if (userSubscriptionService.hasSubscription(planId)) {
            throw new BusinessException("当前套餐还存在生效的订阅，无法删除！");
        }
        this.removeById(planId);
    }
}
