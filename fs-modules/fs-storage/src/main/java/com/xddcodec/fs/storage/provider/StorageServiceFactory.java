package com.xddcodec.fs.storage.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.plugin.boot.StoragePluginManager;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContext;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.service.StorageSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 存储服务工厂
 * 负责管理和获取不同平台的存储服务实现
 * 使用插件化架构，通过StoragePluginManager管理存储插件
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */

/**
 * 存储服务工厂
 * 负责创建和缓存存储实例
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorageServiceFactory {

    private final StoragePluginManager pluginManager;

    private final StorageSettingService storageSettingService;

    /**
     * 获取当前上下文的存储实例
     * 从ThreadLocal自动获取 userId + platformIdentifier
     */
    public IStorageOperationService getCurrentInstance() {
        StoragePlatformContext context = StoragePlatformContextHolder.getContext();
        return getInstance(context.getUserId(), context.getPlatformIdentifier());
    }

    /**
     * 根据用户ID和平台标识获取实例
     */
    public IStorageOperationService getInstance(String userId, String platformIdentifier) {
        // 直接调用 PluginManager，利用其内部缓存
        return pluginManager.getOrCreateInstance(
                // computeIfAbsent 中才查数据库
                StorageConfig.builder()
                        .userId(userId)
                        .platformIdentifier(platformIdentifier)
                        .build(),
                // 缓存未命中时的加载逻辑
                () -> loadConfigFromDatabase(userId, platformIdentifier)
        );
    }

    /**
     * 从数据库加载配置（仅在缓存未命中时调用）
     */
    private StorageConfig loadConfigFromDatabase(String userId, String platformIdentifier) {
        StorageSetting settings = storageSettingService.getStorageSettingByPlatform(
                platformIdentifier,
                userId
        );
        if (settings == null) {
            throw new BusinessException(
                    String.format("未找到用户 %s 的 %s 存储配置", userId, platformIdentifier)
            );
        }
        if (settings.getEnabled() == 0) {
            throw new BusinessException("存储配置已禁用");
        }
        return buildConfig(settings);
    }

    /**
     * 构建 StorageConfig
     */
    private StorageConfig buildConfig(StorageSetting settings) {
        Map<String, Object> properties;
        try {
            ObjectMapper mapper = new ObjectMapper();
            properties = mapper.readValue(
                    settings.getConfigData(),
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new BusinessException("配置数据解析失败: " + e.getMessage());
        }
        return StorageConfig.builder()
                .platformIdentifier(settings.getPlatformIdentifier())
                .userId(settings.getUserId())
                .properties(properties)
                .enabled(settings.getEnabled() == 1)
                .build();
    }
}
