package com.xddcodec.fs.storage.provider;

import com.xddcodec.fs.framework.common.context.StoragePlatformContextHolder;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 存储服务门面
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Service
public class StorageServiceFacade {

    @Autowired
    private StorageServiceFactory storageServiceFactory;

    /**
     * 获取当前上下文对应的存储服务
     * 从ThreadLocal中获取前端传递的存储平台标识，然后动态获取对应的存储服务实现
     */
    public StorageOperationService getCurrentService() {
        String platformIdentifier = StoragePlatformContextHolder.getPlatformOrDefault();
        log.debug("从上下文获取存储平台标识: {}", platformIdentifier);
        return storageServiceFactory.getService(platformIdentifier);
    }

    /**
     * 根据平台标识获取存储服务
     * 用于下载、删除等场景，此时需要根据文件记录中的平台标识获取对应服务
     */
    public StorageOperationService getService(String platformIdentifier) {
        if (StringUtils.isBlank(platformIdentifier)) {
            return getCurrentService();
        }
        log.debug("根据平台标识获取存储服务: {}", platformIdentifier);
        return storageServiceFactory.getService(platformIdentifier);
    }

}