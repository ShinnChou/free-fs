package com.xddcodec.fs.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.system.domain.SysUserTransferSetting;
import com.xddcodec.fs.system.domain.dto.UserTransferSettingEditCmd;
import com.xddcodec.fs.system.mapper.SysUserTransferSettingMapper;
import com.xddcodec.fs.system.service.SysUserTransferSettingService;
import org.springframework.stereotype.Service;

import static com.xddcodec.fs.system.domain.table.SysUserTransferSettingTableDef.SYS_USER_TRANSFER_SETTING;

/**
 * 用户传输设置业务接口实现
 *
 * @Author: xddcode
 * @Date: 2025/11/11 14:35
 */
@Service
public class SysUserTransferSettingServiceImpl extends ServiceImpl<SysUserTransferSettingMapper, SysUserTransferSetting> implements SysUserTransferSettingService {

    @Override
    public void initUserTransferSetting(String userId) {
        SysUserTransferSetting transferSetting = new SysUserTransferSetting();
        transferSetting.setUserId(userId);
        this.save(transferSetting);
    }

    @Override
    public SysUserTransferSetting getByUser() {
        String userId = StpUtil.getLoginIdAsString();
        return this.getOne(new QueryWrapper().where(SYS_USER_TRANSFER_SETTING.USER_ID.eq(userId)));
    }

    @Override
    public void updateUserTransferSetting(UserTransferSettingEditCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        SysUserTransferSetting transferSetting = new SysUserTransferSetting();
        transferSetting.setUserId(userId);
        transferSetting.setDownloadLocation(cmd.getDownloadLocation());
        transferSetting.setIsDefaultDownloadLocation(cmd.getIsDefaultDownloadLocation());
        transferSetting.setConcurrentDownloadQuantity(cmd.getConcurrentDownloadQuantity());
        transferSetting.setConcurrentUploadQuantity(cmd.getConcurrentUploadQuantity());
        transferSetting.setDownloadSpeedLimit(cmd.getDownloadSpeedLimit());
        this.updateById(transferSetting);
    }

    @Override
    public void deleteUserTransferSetting(String userId) {
        this.remove(new QueryWrapper().where(SYS_USER_TRANSFER_SETTING.USER_ID.eq(userId)));
    }
}
