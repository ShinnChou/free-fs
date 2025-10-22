package com.xddcodec.fs.plan.controller;

import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.framework.common.domain.Result;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanAddCmd;
import com.xddcodec.fs.plan.domain.cmd.SubscriptionPlanEditCmd;
import com.xddcodec.fs.plan.domain.qry.SubscriptionPlanPageQry;
import com.xddcodec.fs.plan.domain.vo.SubscriptionPlanVO;
import com.xddcodec.fs.plan.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/apis/plan")
@RequiredArgsConstructor
@Tag(name = "套餐管理")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @Operation(summary = "分页获取套餐列表")
    @GetMapping("/pages")
    public PageResult<SubscriptionPlanVO> getPages(SubscriptionPlanPageQry qry) {
        return subscriptionPlanService.getPages(qry);
    }

    @Operation(summary = "获取套餐详细信息")
    @GetMapping("/info/{id}")
    public Result<SubscriptionPlanVO> getDetail(@PathVariable("id") Long id) {
        SubscriptionPlanVO subscriptionPlanVO = subscriptionPlanService.getDetail(id);
        return Result.ok(subscriptionPlanVO);
    }

    @Operation(summary = "添加套餐")
    @PostMapping()
    public Result<?> add(@Validated @RequestBody SubscriptionPlanAddCmd cmd) {
        subscriptionPlanService.addSubscriptionPlan(cmd);
        return Result.ok();
    }

    @Operation(summary = "编辑套餐")
    @PutMapping()
    public Result<?> edit(@Validated @RequestBody SubscriptionPlanEditCmd cmd) {
        subscriptionPlanService.editSubscriptionPlan(cmd);
        return Result.ok();
    }

    @Operation(summary = "根据ID删除套餐")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") Long id) {
        subscriptionPlanService.removeSubscriptionPlan(id);
        return Result.ok();
    }
}
