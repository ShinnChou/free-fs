package com.xddcodec.fs.system.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.constant.RedisExpire;
import com.xddcodec.fs.framework.common.constant.RedisKey;
import com.xddcodec.fs.framework.common.domain.PageResult;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.notify.mail.domain.Mail;
import com.xddcodec.fs.framework.notify.mail.event.MailEvent;
import com.xddcodec.fs.framework.redis.repository.RedisRepository;
import com.xddcodec.fs.system.domain.SysUser;
import com.xddcodec.fs.system.domain.dto.*;
import com.xddcodec.fs.system.domain.vo.SysUserVO;
import com.xddcodec.fs.system.mapper.SysUserMapper;
import com.xddcodec.fs.system.service.SysUserService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

    @Override
    public SysUser getByUsername(String username) {

        return this.getOne(new QueryWrapper().where(SYS_USER.USERNAME.eq(username)));
    }

    @Override
    public SysUserVO getByUsernameVo(String username) {
        SysUser user = this.getByUsername(username);
        return converter.convert(user, SysUserVO.class);
    }

    @Override
    public SysUserVO getDetail() {
        String userId = StpUtil.getLoginIdAsString();
        SysUser user = this.getById(userId);
        return converter.convert(user, SysUserVO.class);
    }

    @Override
    public PageResult<SysUserVO> getPages(UserPageQry pageQry) {
        Page<SysUser> page = new Page<>(pageQry.getPage(), pageQry.getPageSize());
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.orderBy(SYS_USER.CREATED_AT.desc());
        if (StringUtils.isNotEmpty(pageQry.getUsername())) {
            queryWrapper.where(SYS_USER.USERNAME.like(pageQry.getUsername() + "%"));
        }
        if (StringUtils.isNotEmpty(pageQry.getNickname())) {
            queryWrapper.and(SYS_USER.NICKNAME.like(pageQry.getNickname() + "%"));
        }
        if (StringUtils.isNotEmpty(pageQry.getEmail())) {
            queryWrapper.and(SYS_USER.EMAIL.eq(pageQry.getEmail()));
        }
        if (pageQry.getStatus() != null) {
            queryWrapper.and(SYS_USER.STATUS.eq(pageQry.getStatus()));
        }
        this.page(page, queryWrapper);
        Long total = page.getTotalRow();
        List<SysUser> users = page.getRecords();
        //把admin用户排在最前面
        users.sort(Comparator.comparing(user -> !CommonConstant.DEFAULT_SUPER_ADMIN.equals(user.getUsername())));
        List<SysUserVO> userVOS = converter.convert(users, SysUserVO.class);
        PageResult.PageRecord<SysUserVO> pageRecord = new PageResult.PageRecord<>();
        pageRecord.setRecords(userVOS);
        pageRecord.setTotal(total);
        return PageResult.<SysUserVO>builder().data(pageRecord).code(200).build();
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
    }

    @Override
    public void updateUserStatus(UserStatusEditCmd cmd) {
        SysUser user = this.getById(cmd.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(cmd.getStatus());
        this.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeUser(String id) {
        //删除用户
        this.removeById(id);
    }

    @Override
    public void resetPassword(String id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(SaSecureUtil.sha256(CommonConstant.DEFAULT_PASSWORD));
        this.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addUser(UserAddCmd cmd) {
        SysUser user = this.getByUsername(cmd.getUsername());
        if (user != null) {
            throw new BusinessException("用户名已存在");
        }
        user = new SysUser();
        user.setUsername(cmd.getUsername());
        user.setEmail(cmd.getEmail());
        user.setNickname(cmd.getNickname());
        user.setPassword(SaSecureUtil.sha256(CommonConstant.DEFAULT_PASSWORD));
        this.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editUser(UserEditCmd cmd) {
        SysUser existUser = this.getById(cmd.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        if (!existUser.getUsername().equals(cmd.getUsername())) {
            SysUser user = this.getByUsername(cmd.getUsername());
            if (user != null) {
                throw new BusinessException("用户名已存在");
            }
        }
        existUser.setUsername(cmd.getUsername());
        existUser.setEmail(cmd.getEmail());
        existUser.setNickname(cmd.getNickname());
        this.updateById(existUser);
    }

    @Override
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
        String redisCode = redisRepository.get2Str(redisKey);
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