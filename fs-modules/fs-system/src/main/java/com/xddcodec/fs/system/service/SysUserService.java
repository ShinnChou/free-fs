package com.xddcodec.fs.system.service;

import com.xddcodec.fs.framework.common.domain.PageResult;
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
     * 根据用户名获取用户VO信息
     *
     * @param username
     * @return
     */
    SysUserVO getByUsernameVo(String username);

    /**
     * 获取用户信息
     *
     * @return
     */
    SysUserVO getDetail();

    /**
     * 分页获取用户列表
     *
     * @param pageQry
     * @return
     */
    PageResult<SysUserVO> getPages(UserPageQry pageQry);

    /**
     * 注册用户
     *
     * @param cmd
     */
    void register(UserRegisterCmd cmd);

    /**
     * 修改用户状态
     *
     * @param cmd
     */
    void updateUserStatus(UserStatusEditCmd cmd);

    /**
     * 删除用户
     *
     * @param id
     */
    void removeUser(String id);

    /**
     * 重置用户密码
     *
     * @param id
     */
    void resetPassword(String id);

    /**
     * 新增用户
     *
     * @param cmd
     * @return
     */
    void addUser(UserAddCmd cmd);

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
