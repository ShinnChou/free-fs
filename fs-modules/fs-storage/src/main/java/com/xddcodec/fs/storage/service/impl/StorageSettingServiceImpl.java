package com.xddcodec.fs.storage.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.dto.StorageSettingEditCmd;
import com.xddcodec.fs.storage.mapper.StorageSettingMapper;
import com.xddcodec.fs.storage.plugin.boot.StoragePluginManager;
import com.xddcodec.fs.storage.service.StorageSettingService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.xddcodec.fs.storage.domain.table.StorageSettingTableDef.STORAGE_SETTING;

/**
 * 存储平台配置业务接口实现
 *
 * @Author: xddcode
 * @Date: 2024/10/25 14:38
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageSettingServiceImpl extends ServiceImpl<StorageSettingMapper, StorageSetting> implements StorageSettingService {

    private final StoragePluginManager pluginManager;

    @Override
    public StorageSetting getStorageSettingByPlatform(String storagePlatformIdentifier, String userId) {
        return this.getOne(
                new QueryWrapper()
                        .where(STORAGE_SETTING.PLATFORM_IDENTIFIER.eq(storagePlatformIdentifier))
                        .and(STORAGE_SETTING.USER_ID.eq(userId))
        );
    }

    @Override
    public void openOrCancelStoragePlatform(String storagePlatformIdentifier, Integer action) {
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting storageSetting = this.getStorageSettingByPlatform(storagePlatformIdentifier, userId);
        if (storageSetting == null) {
            storageSetting = new StorageSetting();
            storageSetting.setPlatformIdentifier(storagePlatformIdentifier);
            storageSetting.setUserId(userId);
            storageSetting.setEnabled(CommonConstant.Y);
            this.save(storageSetting);
        } else {
            storageSetting.setEnabled(action == 0 ? CommonConstant.N : CommonConstant.Y);
            this.updateById(storageSetting);
        }

        // 清除缓存，确保下次获取时重新初始化
        pluginManager.invalidateConfig(userId, storagePlatformIdentifier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateStorageSetting(StorageSettingEditCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting storageSetting = this.getStorageSettingByPlatform(cmd.getIdentifier(), userId);
        if (storageSetting != null) {
            storageSetting.setConfigData(cmd.getConfigData());
            this.updateById(storageSetting);
        } else {
            storageSetting = new StorageSetting();
            storageSetting.setPlatformIdentifier(cmd.getIdentifier());
            storageSetting.setUserId(userId);
            storageSetting.setConfigData(cmd.getConfigData());
            storageSetting.setEnabled(CommonConstant.Y);
            this.save(storageSetting);
        }

        // 清除缓存，确保下次获取时使用新配置重新初始化
        pluginManager.invalidateConfig(userId, cmd.getIdentifier());
    }

    @Override
    public List<StorageSetting> listByPlatformIdentifier(String platformIdentifier) {
        return this.list(
                new QueryWrapper()
                        .where(STORAGE_SETTING.PLATFORM_IDENTIFIER.eq(platformIdentifier))
                        .and(STORAGE_SETTING.ENABLED.eq(CommonConstant.Y))
        );
    }
}
