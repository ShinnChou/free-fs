package com.xddcodec.fs.storage.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.JsonUtils;
import com.xddcodec.fs.storage.domain.StoragePlatform;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.cmd.StorageSettingAddCmd;
import com.xddcodec.fs.storage.domain.cmd.StorageSettingEditCmd;
import com.xddcodec.fs.storage.domain.vo.StorageActivePlatformsVO;
import com.xddcodec.fs.storage.domain.vo.StoragePlatformVO;
import com.xddcodec.fs.storage.domain.vo.StorageSettingUserVO;
import com.xddcodec.fs.storage.mapper.StorageSettingMapper;
import com.xddcodec.fs.storage.plugin.boot.StoragePluginManager;
import com.xddcodec.fs.storage.service.StoragePlatformService;
import com.xddcodec.fs.storage.service.StorageSettingService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private final Converter converter;

    private final StoragePluginManager pluginManager;

    private final StoragePlatformService storagePlatformService;

    @Override
    public List<StorageSettingUserVO> getStorageSettingsByUser() {
        String userId = StpUtil.getLoginIdAsString();
        List<StorageSetting> storageSettings = this.list(new QueryWrapper().where(STORAGE_SETTING.USER_ID.eq(userId)));
        return storageSettings.stream().map(storageSetting -> {
            StorageSettingUserVO vo = converter.convert(storageSetting, StorageSettingUserVO.class);
            StoragePlatform storagePlatform = storagePlatformService.getStoragePlatformByIdentifier(storageSetting.getPlatformIdentifier());
            StoragePlatformVO storagePlatformVO = converter.convert(storagePlatform, StoragePlatformVO.class);
            vo.setStoragePlatform(storagePlatformVO);
            return vo;
        }).toList();
    }

    @Override
    public StorageSetting getStorageSettingByPlatform(String storagePlatformIdentifier, String userId) {
        return this.getOne(
                new QueryWrapper()
                        .where(STORAGE_SETTING.PLATFORM_IDENTIFIER.eq(storagePlatformIdentifier))
                        .and(STORAGE_SETTING.USER_ID.eq(userId))
        );
    }

    @Override
    public List<StorageActivePlatformsVO> getActiveStoragePlatforms() {
        String userId = StpUtil.getLoginIdAsString();
        List<StorageSetting> storageSettings = this.list(
                new QueryWrapper().where(STORAGE_SETTING.ENABLED.eq(CommonConstant.Y)
                        .and(STORAGE_SETTING.USER_ID.eq(userId))
                )
        );
        return storageSettings.stream().map(storageSetting -> {
            StoragePlatform storagePlatform = storagePlatformService.getStoragePlatformByIdentifier(storageSetting.getPlatformIdentifier());
            StorageActivePlatformsVO vo = new StorageActivePlatformsVO();
            vo.setSettingId(storageSetting.getId());
            vo.setPlatformIdentifier(storageSetting.getPlatformIdentifier());
            if (storagePlatform != null) {
                vo.setPlatformIcon(storagePlatform.getIcon());
                vo.setPlatformName(storagePlatform.getName());

            }
            return vo;
        }).toList();
    }

    @Override
    public void enableOrDisableStoragePlatform(String settingId, Integer action) {
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting storageSetting = this.getById(settingId);
        if (storageSetting == null) {
            throw new BusinessException("存储配置不存在");
        }
        if (!storageSetting.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此配置");
        }
        storageSetting.setEnabled(action == 0 ? CommonConstant.N : CommonConstant.Y);
        this.updateById(storageSetting);
        // 清除缓存，确保下次获取时重新初始化
//        pluginManager.invalidateConfig(userId, storagePlatformIdentifier);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addStorageSetting(StorageSettingAddCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        boolean exists = this.checkDuplicateConfig(
                cmd.getPlatformIdentifier(),
                userId,
                cmd.getConfigData()
        );
        if (exists) {
            throw new BusinessException("该存储配置已存在，请勿重复添加");
        }
        StorageSetting storageSetting = new StorageSetting();
        storageSetting.setPlatformIdentifier(cmd.getPlatformIdentifier());
        storageSetting.setUserId(userId);
        storageSetting.setConfigData(cmd.getConfigData());
        storageSetting.setEnabled(CommonConstant.Y);
        this.save(storageSetting);
        // 清除缓存，确保下次获取时使用新配置重新初始化
//        pluginManager.invalidateConfig(userId, cmd.getIdentifier());
    }

    /**
     * 检查是否存在重复配置
     */
    private boolean checkDuplicateConfig(String platformIdentifier,
                                         String userId,
                                         String configData) {
        List<StorageSetting> existingSettings = this.list(new QueryWrapper()
                .where(STORAGE_SETTING.USER_ID.eq(userId)
                        .and(STORAGE_SETTING.PLATFORM_IDENTIFIER.eq(platformIdentifier))
                )
        );
        // 将新配置转为标准JSON格式
        String normalizedNewConfig = JsonUtils.normalizeJson(configData);
        // 遍历现有配置，比较JSON内容
        return existingSettings.stream()
                .anyMatch(setting -> {
                    String normalizedExisting = JsonUtils.normalizeJson(setting.getConfigData());
                    return normalizedNewConfig.equals(normalizedExisting);
                });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editStorageSetting(StorageSettingEditCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting storageSetting = this.getById(cmd.getSettingId());
        if (storageSetting == null) {
            throw new BusinessException("存储配置不存在");
        }
        if (!storageSetting.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此配置");
        }
        boolean exists = this.checkDuplicateConfigForUpdate(
                storageSetting.getPlatformIdentifier(),
                userId,
                cmd.getConfigData(),
                cmd.getSettingId()
        );
        if (exists) {
            throw new BusinessException("该存储配置已存在，请勿重复添加");
        }
        storageSetting.setConfigData(cmd.getConfigData());
        storageSetting.setUpdatedAt(LocalDateTime.now());
        this.updateById(storageSetting);
        // 清除缓存，确保下次获取时使用新配置重新初始化
//        pluginManager.invalidateConfig(userId, cmd.getIdentifier());
    }

    /**
     * 检查更新时是否存在重复配置（排除自身）
     */
    private boolean checkDuplicateConfigForUpdate(String platformIdentifier,
                                                  String userId,
                                                  String configData,
                                                  String excludeId) {
        List<StorageSetting> existingSettings = this.list(new QueryWrapper()
                .where(STORAGE_SETTING.USER_ID.eq(userId)
                        .and(STORAGE_SETTING.PLATFORM_IDENTIFIER.eq(platformIdentifier)
                                .and(STORAGE_SETTING.ID.ne(excludeId))
                        )
                )
        );
        String normalizedNewConfig = JsonUtils.normalizeJson(configData);
        return existingSettings.stream()
                .anyMatch(setting -> {
                    String normalizedExisting = JsonUtils.normalizeJson(setting.getConfigData());
                    return normalizedNewConfig.equals(normalizedExisting);
                });
    }

    @Override
    public void deleteStorageSettingById(String id) {

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
