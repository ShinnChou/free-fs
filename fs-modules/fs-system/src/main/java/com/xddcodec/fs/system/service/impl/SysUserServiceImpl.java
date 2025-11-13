package com.xddcodec.fs.system.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.constant.RedisExpire;
import com.xddcodec.fs.framework.common.constant.RedisKey;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.notify.mail.domain.Mail;
import com.xddcodec.fs.framework.notify.mail.event.MailEvent;
import com.xddcodec.fs.framework.redis.repository.RedisRepository;
import com.xddcodec.fs.system.domain.SysUser;
import com.xddcodec.fs.system.domain.dto.*;
import com.xddcodec.fs.system.domain.vo.SysUserVO;
import com.xddcodec.fs.system.mapper.SysUserMapper;
import com.xddcodec.fs.system.service.SysUserService;
import com.xddcodec.fs.system.service.SysUserTransferSettingService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.xddcodec.fs.system.domain.table.SysUserTableDef.SYS_USER;

/**
 * 用户表 服务实现类
 *
 * @Author: xddcode
 * @Date: 2024/6/7 11:14
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final Converter converter;

    private final RedisRepository redisRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final SysUserTransferSettingService userTransferSettingService;

    @Override
    public SysUser getByUsername(String username) {

        return this.getOne(new QueryWrapper().where(SYS_USER.USERNAME.eq(username)));
    }

    @Override
    @Cacheable(value = "user", keyGenerator = "userKeyGenerator")
    public SysUserVO getDetail() {
        String userId = StpUtil.getLoginIdAsString();
        SysUser user = this.getById(userId);
        return converter.convert(user, SysUserVO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterCmd cmd) {
        SysUser user = this.getByUsername(cmd.getUsername());
        if (user != null) {
            throw new BusinessException("用户名已存在");
        }
        if (!cmd.getPassword().equals(cmd.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        user = new SysUser();
        user.setUsername(cmd.getUsername());
        user.setPassword(SaSecureUtil.sha256(cmd.getPassword()));
        user.setEmail(cmd.getEmail());
        user.setNickname(cmd.getNickname());
        user.setAvatar(cmd.getAvatar());
        this.save(user);
        //初始化用户传输配置
        userTransferSettingService.initUserTransferSetting(user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    @CacheEvict(value = "user", keyGenerator = "userKeyGenerator")
    public void editUserInfo(UserEditInfoCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        SysUser existUser = this.getById(userId);
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        existUser.setNickname(cmd.getNickname());
        this.updateById(existUser);
    }

    @Override
    @CacheEvict(value = "user", keyGenerator = "userKeyGenerator")
    public void updatePassword(PasswordEditCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getPassword().equals(SaSecureUtil.sha256(cmd.getOldPassword()))) {
            throw new BusinessException("原密码错误");
        }
        if (!cmd.getNewPassword().equals(cmd.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        user.setPassword(SaSecureUtil.sha256(cmd.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public void sendForgetPasswordCode(String email) {
        SysUser user = this.getOne(new QueryWrapper().where(SYS_USER.EMAIL.eq(email)));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String code = RandomUtil.randomNumbers(CommonConstant.VERIFY_CODE_LENGTH);
        String redisKey = RedisKey.getVerifyCodeKey(email);
        redisRepository.setExpire(redisKey, code, RedisExpire.VERIFY_CODE);

        Mail mail = Mail.buildVerifyCodeMail(email, user.getNickname(), code);
        eventPublisher.publishEvent(new MailEvent(this, mail));
    }

    @Override
    public void updateForgetPassword(PasswordForgetEditCmd cmd) {
        String email = cmd.getMail();
        SysUser user = this.getOne(new QueryWrapper().where(SYS_USER.EMAIL.eq(email)));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String code = cmd.getCode();
        String redisKey = RedisKey.getVerifyCodeKey(email);
        String redisCode = (String) redisRepository.get(redisKey);
        if (!code.equals(redisCode)) {
            throw new BusinessException("验证码错误");
        }
        if (!cmd.getNewPassword().equals(cmd.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        user.setPassword(SaSecureUtil.sha256(cmd.getNewPassword()));
        this.updateById(user);
    }
}