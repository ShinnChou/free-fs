package com.xddcodec.fs.system.service;

import com.xddcodec.fs.system.domain.SysUser;
import com.xddcodec.fs.system.domain.dto.*;
import com.xddcodec.fs.system.domain.vo.SysUserVO;
import com.mybatisflex.core.service.IService;

/**
 * 用户服务接口
 *
 * @Author: xddcode
 * @Date: 2024/6/7 11:08
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名获取用户信息
     *
     * @param username
     * @return
     */
    SysUser getByUsername(String username);

    /**
     * 获取用户信息
     *
     * @return
     */
    SysUserVO getDetail();

    /**
     * 注册用户
     *
     * @param cmd
     */
    void register(UserRegisterCmd cmd);

    /**
     * 编辑用户个人信息
     *
     * @param cmd
     * @return
     */
    void editUserInfo(UserEditInfoCmd cmd);

    /**
     * 修改密码
     *
     * @param cmd
     * @return
     */
    void updatePassword(PasswordEditCmd cmd);


    /**
     * 忘记密码-发送验证码
     *
     * @param email
     */
    void sendForgetPasswordCode(String email);

    /**
     * 忘记密码-修改密码
     *
     * @param cmd
     */
    void updateForgetPassword(PasswordForgetEditCmd cmd);
}
