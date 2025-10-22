package com.xddcodec.fs.system.service;

import com.xddcodec.fs.system.domain.dto.LoginCmd;
import com.xddcodec.fs.system.domain.vo.LoginUserVO;

/**
 * 认证服务接口
 *
 * @Author: xddcode
 * @Date: 2024/10/16 14:25
 */
public interface AuthService {

    /**
     * 登录
     *
     * @param cmd
     * @return
     */
    LoginUserVO doLogin(LoginCmd cmd);

    /**
     * 退出登录
     */
    void logout();
}
