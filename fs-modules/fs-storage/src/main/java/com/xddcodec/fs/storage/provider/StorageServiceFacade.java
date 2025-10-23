package com.xddcodec.fs.storage.provider;

import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 存储服务门面
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceFacade {

    private final StorageServiceFactory storageServiceFactory;

    /**
     * 获取当前上下文的存储服务实例
     * 从ThreadLocal自动获取userId、platformIdentifier、configId
     *
     * @return 存储服务实例
     */
    public IStorageOperationService getCurrentStorageService() {
        return storageServiceFactory.getCurrentInstance();
    }

    /**
     * 根据用户ID和平台标识获取存储服务
     *
     * @param userId 用户ID
     * @param platformIdentifier 平台标识符
     * @return 存储服务实例
     */
    public IStorageOperationService getStorageService(String userId, String platformIdentifier) {
        return storageServiceFactory.getInstance(userId, platformIdentifier);
    }
}