package com.xddcodec.fs.storage.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.JsonUtils;
import com.xddcodec.fs.storage.domain.StoragePlatform;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.cmd.StorageSettingAddCmd;
import com.xddcodec.fs.storage.domain.cmd.StorageSettingEditCmd;
import com.xddcodec.fs.storage.domain.vo.StorageActivePlatformsVO;
import com.xddcodec.fs.storage.domain.vo.StoragePlatformVO;
import com.xddcodec.fs.storage.domain.vo.StorageSettingUserVO;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.mapper.StorageSettingMapper;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import com.xddcodec.fs.storage.service.StoragePlatformService;
import com.xddcodec.fs.storage.service.StorageSettingService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private final StoragePlatformService storagePlatformService;

    private final StorageServiceFacade storageServiceFacade;

    @Override
    public List<StorageSettingUserVO> getStorageSettingsByUser() {
        String userId = StpUtil.getLoginIdAsString();
        List<StorageSetting> storageSettings = this.list(
                new QueryWrapper()
                        .where(STORAGE_SETTING.USER_ID
                                .eq(userId))
                        .orderBy(STORAGE_SETTING.ENABLED.desc()
                        )
        );
        return storageSettings.stream().map(storageSetting -> {
            StorageSettingUserVO vo = converter.convert(storageSetting, StorageSettingUserVO.class);
            StoragePlatform storagePlatform = storagePlatformService.getStoragePlatformByIdentifier(storageSetting.getPlatformIdentifier());
            StoragePlatformVO storagePlatformVO = converter.convert(storagePlatform, StoragePlatformVO.class);
            vo.setStoragePlatform(storagePlatformVO);
            return vo;
        }).toList();
    }

    @Override
    public List<StorageActivePlatformsVO> getActiveStoragePlatforms() {
        String userId = StpUtil.getLoginIdAsString();

        StorageSetting storageSetting = this.getOne(
                new QueryWrapper().where(STORAGE_SETTING.ENABLED.eq(CommonConstant.Y)
                        .and(STORAGE_SETTING.USER_ID.eq(userId))
                )
        );
        List<StorageActivePlatformsVO> result = new ArrayList<>();
        // 添加默认本地存储平台
        StorageActivePlatformsVO localInstance = new StorageActivePlatformsVO();
        localInstance.setSettingId(StoragePlatformIdentifierEnum.LOCAL.getIdentifier());
        localInstance.setPlatformIdentifier(StoragePlatformIdentifierEnum.LOCAL.getIdentifier());
        localInstance.setPlatformIcon(StoragePlatformIdentifierEnum.LOCAL.getIcon());
        localInstance.setPlatformName(StoragePlatformIdentifierEnum.LOCAL.getDescription());
        localInstance.setIsEnabled(true);
        localInstance.setRemark("系统默认");
        if (storageSetting != null) {
            localInstance.setIsEnabled(false);
            StoragePlatform storagePlatform = storagePlatformService.getStoragePlatformByIdentifier(storageSetting.getPlatformIdentifier());
            StorageActivePlatformsVO vo = new StorageActivePlatformsVO();
            vo.setSettingId(storageSetting.getId());
            vo.setPlatformIdentifier(storageSetting.getPlatformIdentifier());
            if (storagePlatform != null) {
                vo.setPlatformIcon(storagePlatform.getIcon());
                vo.setPlatformName(storagePlatform.getName());
            }
            vo.setRemark(storageSetting.getRemark());
            vo.setCreatedAt(storageSetting.getCreatedAt());
            vo.setUpdatedAt(storageSetting.getUpdatedAt());
            vo.setIsEnabled(true);
            result.add(vo);
        }
        result.add(localInstance);
        return result;
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

        Integer newStatus = action == 0 ? CommonConstant.N : CommonConstant.Y;

        //如果是启用保证只能启用一个配置
        if (newStatus.equals(CommonConstant.Y)) {
            //先把所有配置禁用
            List<StorageSetting> storageSettings = this.list(
                    new QueryWrapper()
                            .where(STORAGE_SETTING.USER_ID.eq(userId)
                                    .and(STORAGE_SETTING.ENABLED.eq(CommonConstant.Y)
                                    )
                            )
            );
            storageSettings.forEach(s -> s.setEnabled(CommonConstant.N));
            this.updateBatch(storageSettings);
        }
        storageSetting.setEnabled(newStatus);
        this.updateById(storageSetting);
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
        storageSetting.setEnabled(CommonConstant.N);
        storageSetting.setRemark(cmd.getRemark());
        this.save(storageSetting);
        log.info("新增存储配置成功: settingId={}, platform={}, userId={}",
                storageSetting.getId(),
                cmd.getPlatformIdentifier(),
                userId);
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
        storageSetting.setRemark(cmd.getRemark());
        this.updateById(storageSetting);
        // 刷新缓存
        storageServiceFacade.refreshInstance(cmd.getSettingId());
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
        String userId = StpUtil.getLoginIdAsString();
        StorageSetting storageSetting = this.getById(id);

        if (storageSetting == null) {
            throw new BusinessException("存储配置不存在");
        }
        if (!storageSetting.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此配置");
        }
        //判断当前配置是否被使用
        String cacheSettingId = StoragePlatformContextHolder.getConfigId();
        if (id.equals(cacheSettingId)) {
            throw new BusinessException("当前配置正在使用中，无法删除");
        }

        // 逻辑删除
        this.removeById(id);
        // 移除缓存
        storageServiceFacade.removeInstance(id);

        log.info("存储配置已删除并移除缓存: settingId={}, userId={}", id, userId);
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
