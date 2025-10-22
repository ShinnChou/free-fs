package com.xddcodec.fs.storage.service;

import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.domain.dto.StorageSettingEditCmd;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 存储平台配置业务接口
 *
 * @Author: xddcode
 * @Date: 2024/10/25 14:37
 */
public interface StorageSettingService extends IService<StorageSetting> {

    /**
     * 根据存储平台标识和用户ID获取存储平台配置信息
     *
     * @param storagePlatformIdentifier 存储平台标识
     * @param userId                    用户ID
     * @return
     */
    StorageSetting getStorageSettingByPlatform(String storagePlatformIdentifier, String userId);

    /**
     * 开通或关闭存储平台
     *
     * @param storagePlatformIdentifier 存储平台标识
     * @param action                    0: 关闭 1: 开通
     */
    void openOrCancelStoragePlatform(String storagePlatformIdentifier, Integer action);

    /**
     * 新增或更新存储平台配置信息
     *
     * @param cmd
     */
    void saveOrUpdateStorageSetting(StorageSettingEditCmd cmd);

    /**
     * 根据存储平台标识查询所有配置（用于检查是否有用户开通）
     *
     * @param platformIdentifier 存储平台标识
     * @return
     */
    List<StorageSetting> listByPlatformIdentifier(String platformIdentifier);
}
