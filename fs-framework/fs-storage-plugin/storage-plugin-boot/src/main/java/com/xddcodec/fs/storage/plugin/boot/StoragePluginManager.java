package com.xddcodec.fs.storage.plugin.boot;

import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import com.xddcodec.fs.storage.plugin.core.utils.StorageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

/**
 * 存储插件管理器（门面）
 * 职责：
 * 1. 组合各个管理组件
 * 2. 提供统一的对外接口
 * 3. 协调 Local 和用户配置的处理逻辑
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoragePluginManager implements DisposableBean {

    private final StoragePluginRegistry pluginRegistry;
    private final StorageInstanceFactory instanceFactory;
    private final StorageInstanceCache instanceCache;
    private final LocalStorageManager localStorageManager;

    /**
     * 获取当前上下文的存储实例（仅从缓存获取）
     *
     * @param configId 配置ID
     * @return 存储服务实例
     * @throws StorageOperationException 如果实例未初始化
     */
    public IStorageOperationService getCurrentInstance(String configId) {
        // Local 存储
        if (StorageUtils.isLocalConfig(configId)) {
            return localStorageManager.getLocalInstance();
        }

        // 用户配置存储（仅从缓存获取）
        IStorageOperationService instance = instanceCache.get(configId);

        if (instance == null) {
            throw new StorageOperationException(
                    String.format("存储实例未初始化，请检查配置: %s", configId)
            );
        }

        return instance;
    }

    /**
     * 获取或创建存储实例（支持延迟加载配置）
     *
     * @param configId     配置ID
     * @param configLoader 配置加载器（缓存未命中时调用）
     * @return 存储服务实例
     */
    public IStorageOperationService getOrCreateInstance(
            String configId,
            Supplier<StorageConfig> configLoader) {

        // Local 存储
        if (StorageUtils.isLocalConfig(configId)) {
            return localStorageManager.getLocalInstance();
        }

        // 用户配置存储（带缓存）
        return instanceCache.getOrCreate(configId, () -> {
            // 加载配置
            log.debug("缓存未命中，开始加载配置: configId={}", configId);
            StorageConfig config = configLoader.get();

            // 创建实例
            return instanceFactory.createInstance(config);
        });
    }

    /**
     * 获取 Local 实例
     *
     * @return Local 存储实例
     */
    public IStorageOperationService getLocalInstance() {
        return localStorageManager.getLocalInstance();
    }

    /**
     * 使配置失效（从缓存中移除并关闭实例）
     *
     * @param configId 配置ID
     */
    public void invalidateConfig(String configId) {
        if (StorageUtils.isLocalConfig(configId)) {
            log.debug("Local 平台为全局单例，不支持失效操作");
            return;
        }

        instanceCache.invalidate(configId);
    }

    /**
     * 批量失效配置
     *
     * @param configIds 配置ID列表
     */
    public void invalidateConfigs(List<String> configIds) {
        if (configIds == null || configIds.isEmpty()) {
            log.debug("批量失效配置列表为空，跳过");
            return;
        }

        // 过滤掉 Local 配置
        List<String> filteredConfigIds = configIds.stream()
                .filter(configId -> !StorageUtils.isLocalConfig(configId))
                .toList();

        instanceCache.invalidateBatch(filteredConfigIds);
    }

    /**
     * 清除用户的所有实例
     *
     * @param configIds 用户的配置ID列表
     */
    public void clearUserInstances(List<String> configIds) {
        log.info("清除用户的所有存储实例，共 {} 个配置",
                configIds == null ? 0 : configIds.size());
        invalidateConfigs(configIds);
    }

    /**
     * 获取插件原型实例
     *
     * @param platformIdentifier 平台标识符
     * @return 原型实例
     */
    public IStorageOperationService getPrototype(String platformIdentifier) {
        return pluginRegistry.getPrototype(platformIdentifier);
    }

    /**
     * 获取所有可用平台
     *
     * @return 平台标识符集合
     */
    public Set<String> getAvailablePlatforms() {
        return pluginRegistry.getAvailablePlatforms();
    }

    /**
     * 清空所有缓存（危险操作，仅用于测试或维护）
     */
    public void clearAllCache() {
        log.warn("清空所有存储实例缓存");
        instanceCache.clear();
    }

    /**
     * 销毁：关闭所有实例
     */
    @Override
    public void destroy() {
        log.info("开始关闭所有存储实例...");

        // 关闭 Local 实例
        localStorageManager.destroy();

        // 关闭所有缓存实例
        instanceCache.clear();

        log.info("所有存储实例已关闭");
    }
}
