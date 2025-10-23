package com.xddcodec.fs.storage.plugin.boot;

import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContext;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 存储插件管理器
 */
@Slf4j
@Component
public class StoragePluginManager implements DisposableBean {

    /**
     * 原型实例：platformIdentifier -> 原型
     */
    private final Map<String, IStorageOperationService> prototypes = new ConcurrentHashMap<>();

    /**
     * 配置化实例缓存：cacheKey -> 实例
     * cacheKey格式：userId_platformIdentifier
     */
    private final Map<String, IStorageOperationService> instanceCache = new ConcurrentHashMap<>();

    /**
     * 创建实例的锁（防止并发创建重复实例）
     */
    private final Map<String, Lock> creationLocks = new ConcurrentHashMap<>();

    /**
     * 初始化：加载SPI插件
     */
    @PostConstruct
    public void init() {
        ServiceLoader<IStorageOperationService> loader =
                ServiceLoader.load(IStorageOperationService.class);

        int loadedCount = 0;
        for (IStorageOperationService prototype : loader) {
            String identifier = prototype.getPlatformIdentifier();
            if (prototypes.containsKey(identifier)) {
                log.warn("发现重复的平台标识符，跳过: {}", identifier);
                continue;
            }
            prototypes.put(identifier, prototype);
            log.info("注册存储插件: {}", identifier);
            loadedCount++;
        }

        log.info("存储插件管理器初始化完成，共加载 {} 个插件", loadedCount);

        if (loadedCount == 0) {
            log.warn("未加载到任何存储插件，请检查 META-INF/services 配置");
        }
    }

    /**
     * 获取当前上下文的存储实例
     *
     * @return 存储服务实例
     * @throws StorageOperationException 如果实例未初始化
     */
    public IStorageOperationService getCurrentInstance() {
        StoragePlatformContext context = StoragePlatformContextHolder.getContext();
        String cacheKey = buildCacheKey(context.getUserId(), context.getPlatformIdentifier());

        IStorageOperationService instance = instanceCache.get(cacheKey);
        if (instance != null) {
            return instance;
        }

        // 缓存未命中，应该由 Factory 调用 getOrCreateInstance 创建
        throw new StorageOperationException(
                "存储实例未初始化，请检查配置: " + cacheKey
        );
    }

    /**
     * 获取或创建存储实例（支持延迟加载配置）
     *
     * @param configBuilder 配置构建器（仅包含 userId 和 platformIdentifier）
     * @param configLoader  配置加载器（缓存未命中时从数据库加载完整配置）
     * @return 存储服务实例
     */
    public IStorageOperationService getOrCreateInstance(
            StorageConfig configBuilder,
            Supplier<StorageConfig> configLoader) {

        String cacheKey = configBuilder.getCacheKey();

        // 缓存命中
        IStorageOperationService instance = instanceCache.get(cacheKey);
        if (instance != null) {
            return instance;
        }

        // 需要创建实例（首次访问或配置变更后）
        Lock lock = creationLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            instance = instanceCache.get(cacheKey);
            if (instance != null) {
                return instance;
            }

            // 仅在缓存未命中时加载完整配置（避免不必要的数据库查询）
            StorageConfig fullConfig = configLoader.get();

            // 验证配置
            validateConfig(fullConfig);

            // 获取原型并创建实例
            IStorageOperationService prototype = prototypes.get(fullConfig.getPlatformIdentifier());
            if (prototype == null) {
                throw new StorageOperationException(
                        "不支持的存储平台: " + fullConfig.getPlatformIdentifier()
                );
            }

            instance = prototype.createConfiguredInstance(fullConfig);

            // 放入缓存（永久缓存，不淘汰）
            instanceCache.put(cacheKey, instance);

            log.info("创建存储实例: {} (当前缓存: {})", cacheKey, instanceCache.size());
            return instance;

        } catch (Exception e) {
            log.error("创建存储实例失败: {}", cacheKey, e);
            throw new StorageOperationException(
                    "创建存储实例失败: " + e.getMessage(), e
            );
        } finally {
            lock.unlock();
            // 清理锁对象（避免内存泄漏）
            creationLocks.remove(cacheKey);
        }
    }

    /**
     * 获取或创建存储实例
     */
    public IStorageOperationService getOrCreateInstance(StorageConfig config) {
        return getOrCreateInstance(config, () -> config);
    }

    /**
     * 使配置失效
     *
     * @param userId             用户ID
     * @param platformIdentifier 平台标识符
     */
    public void invalidateConfig(String userId, String platformIdentifier) {
        String cacheKey = buildCacheKey(userId, platformIdentifier);

        IStorageOperationService instance = instanceCache.remove(cacheKey);

        if (instance != null) {
            closeInstanceSafely(instance, cacheKey);
            log.info("使配置失效并关闭实例: {} (剩余缓存: {})", cacheKey, instanceCache.size());
        } else {
            log.debug("缓存中无实例需要失效: {}", cacheKey);
        }
    }

    /**
     * 清除用户的所有实例
     *
     * @param userId 用户ID
     */
    public void clearUserInstances(String userId) {
        String prefix = userId + "_";

        List<String> keysToRemove = instanceCache.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .toList();

        if (keysToRemove.isEmpty()) {
            log.debug("用户 {} 没有需要清除的实例", userId);
            return;
        }

        keysToRemove.forEach(key -> {
            IStorageOperationService instance = instanceCache.remove(key);
            if (instance != null) {
                closeInstanceSafely(instance, key);
            }
        });

        log.info("清除用户 {} 的 {} 个存储实例 (剩余缓存: {})",
                userId, keysToRemove.size(), instanceCache.size());
    }

    /**
     * 获取原型实例（用于获取配置Schema）
     *
     * @param platformIdentifier 平台标识符
     * @return 原型实例
     */
    public IStorageOperationService getPrototype(String platformIdentifier) {
        IStorageOperationService prototype = prototypes.get(platformIdentifier);
        if (prototype == null) {
            throw new StorageOperationException("不支持的存储平台: " + platformIdentifier);
        }
        return prototype;
    }

    /**
     * 获取所有可用平台
     */
    public Set<String> getAvailablePlatforms() {
        return Collections.unmodifiableSet(prototypes.keySet());
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Integer> platformCount = new HashMap<>();

        // 统计每个平台的实例数
        instanceCache.keySet().forEach(key -> {
            String platform = key.substring(key.indexOf('_') + 1);
            platformCount.merge(platform, 1, Integer::sum);
        });

        return Map.of(
                "totalPrototypes", prototypes.size(),
                "cachedInstances", instanceCache.size(),
                "platformDistribution", platformCount,
                "cacheKeys", new ArrayList<>(instanceCache.keySet())
        );
    }

    /**
     * 销毁：关闭所有实例
     */
    @Override
    public void destroy() {
        log.info("关闭所有存储实例，当前实例数: {}", instanceCache.size());

        instanceCache.forEach((key, instance) -> {
            closeInstanceSafely(instance, key);
        });

        instanceCache.clear();
        creationLocks.clear();

        log.info("所有存储实例已关闭");
    }

    /**
     * 构建缓存键
     * 格式：userId_platformIdentifier
     */
    private String buildCacheKey(String userId, String platformIdentifier) {
        return String.format("%s_%s", userId, platformIdentifier);
    }

    /**
     * 验证配置
     */
    private void validateConfig(StorageConfig config) {
        if (config == null) {
            throw new StorageOperationException("存储配置不能为空");
        }
        if (config.getUserId() == null || config.getUserId().isBlank()) {
            throw new StorageOperationException("用户ID不能为空");
        }
        if (config.getPlatformIdentifier() == null || config.getPlatformIdentifier().isBlank()) {
            throw new StorageOperationException("平台标识符不能为空");
        }
        if (config.getEnabled() != null && !config.getEnabled()) {
            throw new StorageOperationException(
                    String.format("存储平台已禁用: %s", config.getPlatformIdentifier())
            );
        }
    }

    /**
     * 安全关闭实例
     *
     * <p>即使关闭失败也要确保从缓存中移除，避免继续使用错误的实例
     */
    private void closeInstanceSafely(IStorageOperationService instance, String cacheKey) {
        try {
            instance.close();
            log.debug("成功关闭实例: {}", cacheKey);
        } catch (IOException e) {
            log.error("关闭实例失败: {}, 错误: {}", cacheKey, e.getMessage());
            // 🔥 即使关闭失败，也已经从缓存中移除了
        } catch (Exception e) {
            log.error("关闭实例时发生未知错误: {}", cacheKey, e);
        }
    }
}
