package com.xddcodec.fs.system.controller;

import com.xddcodec.fs.framework.common.domain.Result;
import com.xddcodec.fs.system.domain.SysUserTransferSetting;
import com.xddcodec.fs.system.domain.dto.*;
import com.xddcodec.fs.system.domain.vo.SysUserVO;
import com.xddcodec.fs.system.service.SysUserService;
import com.xddcodec.fs.system.service.SysUserTransferSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * @Author: xddcode
 * @Date: 2024/6/18 8:51
 */
@Validated
@RestController
@RequestMapping("/apis/user")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final SysUserService userService;

    private final SysUserTransferSettingService userTransferSettingService;

    @Operation(summary = "获取用户详细信息")
    @GetMapping("/info")
    public Result<SysUserVO> getDetail() {
        SysUserVO user = userService.getDetail();
        return Result.ok(user);
    }

    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody UserRegisterCmd cmd) {
        userService.register(cmd);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/info")
    public Result<?> editUserInfo(@Validated @RequestBody UserEditInfoCmd cmd) {
        userService.editUserInfo(cmd);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<?> resetPassword(@Validated @RequestBody PasswordEditCmd cmd) {
        userService.updatePassword(cmd);
        return Result.ok();
    }

    @Operation(summary = "忘记密码-发送验证码")
    @GetMapping("/forget-password/code/{mail}")
    public Result<?> sendForgetPasswordCode(@PathVariable String mail) {
        userService.sendForgetPasswordCode(mail);
        return Result.ok();
    }

    @Operation(summary = "忘记密码-修改密码")
    @PutMapping("/forget-password")
    public Result<?> checkForgetPasswordCode(@Validated @RequestBody PasswordForgetEditCmd cmd) {
        userService.updateForgetPassword(cmd);
        return Result.ok();
    }

    @Operation(summary = "获取用户传输配置")
    @GetMapping("/transfer/setting")
    public Result<SysUserTransferSetting> sendForgetPasswordCode() {
        SysUserTransferSetting userTransferSetting = userTransferSettingService.getByUser();
        return Result.ok(userTransferSetting);
    }
}
