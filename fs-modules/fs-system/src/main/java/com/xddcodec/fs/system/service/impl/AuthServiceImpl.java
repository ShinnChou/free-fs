package com.xddcodec.fs.system.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.log.annotation.LoginLog;
import com.xddcodec.fs.system.domain.SysUser;
import com.xddcodec.fs.system.domain.dto.LoginCmd;
import com.xddcodec.fs.system.domain.vo.LoginUserVO;
import com.xddcodec.fs.system.service.AuthService;
import com.xddcodec.fs.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务实现
 *
 * @Author: xddcode
 * @Date: 2024/10/16 14:26
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService userService;

    @Override
    @LoginLog("用户登录")
    public LoginUserVO doLogin(LoginCmd cmd) {
        SysUser user = userService.getByUsername(cmd.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 1) {
            throw new BusinessException("用户已被禁用");
        }
        if (!SaSecureUtil.sha256(cmd.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("密码不正确");
        }
        StpUtil.login(user.getId(), cmd.getIsRemember());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUsername(user.getUsername());
        loginUserVO.setAccessToken(tokenInfo.getTokenValue());
        //修改最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userService.updateById(user);
        return loginUserVO;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}
