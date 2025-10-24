package com.xddcodec.fs.storage.plugin.boot;

import com.google.common.util.concurrent.Striped;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.utils.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * 存储实例缓存管理器
 * 职责：
 * 1. 缓存已创建的存储实例
 * 2. 提供线程安全的缓存操作
 * 3. 管理实例生命周期
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Component
public class StorageInstanceCache {

    /**
     * 实例缓存
     * Key: configId (规范化后，Local 为 null)
     * Value: 存储实例
     */
    private final Map<String, IStorageOperationService> cache = new ConcurrentHashMap<>();

    /**
     * 分段锁（用于并发控制）
     * 使用 Guava Striped 提供细粒度锁
     */
    private final Striped<Lock> locks = Striped.lock(128);

    /**
     * 获取缓存实例
     *
     * @param configId 配置ID
     * @return 存储实例，不存在返回 null
     */
    public IStorageOperationService get(String configId) {
        String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
        String cacheKey = generateCacheKey(normalizedConfigId);

        IStorageOperationService instance = cache.get(cacheKey);

        if (instance != null) {
            log.debug("缓存命中: configId={}, cacheKey={}", configId, cacheKey);
        }

        return instance;
    }

    /**
     * 放入缓存
     *
     * @param configId 配置ID
     * @param instance 存储实例
     */
    public void put(String configId, IStorageOperationService instance) {
        String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
        String cacheKey = generateCacheKey(normalizedConfigId);

        cache.put(cacheKey, instance);

        log.debug("放入缓存: configId={}, cacheKey={}, 当前缓存数: {}",
                configId, cacheKey, cache.size());
    }

    /**
     * 获取或创建实例（线程安全）
     *
     * @param configId 配置ID
     * @param creator  实例创建器
     * @return 存储实例
     */
    public IStorageOperationService getOrCreate(
            String configId,
            Supplier<IStorageOperationService> creator) {

        String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
        String cacheKey = generateCacheKey(normalizedConfigId);

        // 第一次检查（无锁）
        IStorageOperationService instance = cache.get(cacheKey);
        if (instance != null) {
            log.debug("缓存命中（快速路径）: configId={}", configId);
            return instance;
        }

        // 获取分段锁
        Lock lock = locks.get(cacheKey);
        lock.lock();
        try {
            // 第二次检查（持锁）
            instance = cache.get(cacheKey);
            if (instance != null) {
                log.debug("缓存命中（等待锁期间）: configId={}", configId);
                return instance;
            }

            // 创建实例
            log.debug("缓存未命中，开始创建实例: configId={}", configId);
            instance = creator.get();

            // 放入缓存
            cache.put(cacheKey, instance);

            log.info("实例创建并缓存成功: configId={}, 当前缓存数: {}",
                    configId, cache.size());

            return instance;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 使缓存失效（移除并关闭实例）
     *
     * @param configId 配置ID
     */
    public void invalidate(String configId) {
        String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
        String cacheKey = generateCacheKey(normalizedConfigId);

        IStorageOperationService instance = cache.remove(cacheKey);

        if (instance != null) {
            closeInstanceSafely(instance, configId);
            log.info("缓存失效: configId={}, 剩余缓存数: {}", configId, cache.size());
        } else {
            log.debug("缓存中无实例需要失效: configId={}", configId);
        }
    }

    /**
     * 批量失效
     *
     * @param configIds 配置ID列表
     */
    public void invalidateBatch(Collection<String> configIds) {
        if (configIds == null || configIds.isEmpty()) {
            log.debug("批量失效列表为空，跳过");
            return;
        }

        int count = 0;
        for (String configId : configIds) {
            String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
            String cacheKey = generateCacheKey(normalizedConfigId);

            IStorageOperationService instance = cache.remove(cacheKey);
            if (instance != null) {
                closeInstanceSafely(instance, configId);
                count++;
            }
        }

        log.info("批量失效完成，共失效 {} 个实例，剩余缓存数: {}",
                count, cache.size());
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        log.warn("清空所有缓存，当前缓存数: {}", cache.size());

        cache.forEach((cacheKey, instance) -> {
            closeInstanceSafely(instance, cacheKey);
        });

        cache.clear();

        log.info("所有缓存已清空");
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 检查缓存中是否存在指定实例
     *
     * @param configId 配置ID
     * @return true-存在
     */
    public boolean contains(String configId) {
        String normalizedConfigId = StorageUtils.normalizeConfigId(configId);
        String cacheKey = generateCacheKey(normalizedConfigId);
        return cache.containsKey(cacheKey);
    }

    /**
     * 生成缓存键
     *
     * @param normalizedConfigId 规范化后的配置ID
     * @return 缓存键
     */
    private String generateCacheKey(String normalizedConfigId) {
        return StorageUtils.generateCacheKey(normalizedConfigId);
    }

    /**
     * 安全关闭实例
     *
     * @param instance 存储实例
     * @param configId 配置ID（用于日志）
     */
    private void closeInstanceSafely(IStorageOperationService instance, String configId) {
        try {
            instance.close();
            log.debug("成功关闭实例: configId={}", configId);
        } catch (IOException e) {
            log.error("关闭实例失败: configId={}, error={}", configId, e.getMessage());
        } catch (Exception e) {
            log.error("关闭实例时发生未知错误: configId={}", configId, e);
        }
    }
}
