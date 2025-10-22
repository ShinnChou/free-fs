package com.xddcodec.fs.plan.service;

import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.plan.domain.SubscriptionPlan;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanAddCmd;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanEditCmd;
import com.xddcodec.fs.plan.domain.qry.SubscriptionPlanPageQry;
import com.xddcodec.fs.plan.domain.vo.SubscriptionPlanVO;

/**
 * 套餐表业务服务接口
 *
 * @Author: xddcodec
 * @Date: 2025/9/24 11:26
 */
public interface SubscriptionPlanService extends IService<SubscriptionPlan> {

    /**
     * 分页获取套餐列表
     *
     * @param pageQry
     * @return
     */
    PageResult<SubscriptionPlanVO> getPages(SubscriptionPlanPageQry pageQry);


    /**
     * 根据套餐编码获取套餐信息
     *
     * @param planCode
     * @return
     */
    SubscriptionPlan getByCode(String planCode);

    /**
     * 根据套餐名称获取套餐信息
     *
     * @param planName
     * @return
     */
    SubscriptionPlan getByName(String planName);

    /**
     * 获取默认套餐
     *
     * @return
     */
    SubscriptionPlan getDefaultPlan();

    /**
     * 获取套餐详情
     *
     * @param planId
     * @return
     */
    SubscriptionPlanVO getDetail(Long planId);

    /**
     * 添加套餐
     *
     * @param cmd
     */
    void addSubscriptionPlan(SubscriptionPlanAddCmd cmd);

    /**
     * 编辑套餐
     *
     * @param cmd
     */
    void editSubscriptionPlan(SubscriptionPlanEditCmd cmd);

    /**
     * 删除套餐
     *
     * @param planId
     */
    void removeSubscriptionPlan(Long planId);

}
