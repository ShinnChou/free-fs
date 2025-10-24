package com.xddcodec.fs.storage.plugin.boot;

import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件注册器
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Component
public class StoragePluginRegistry {
    /**
     * 插件原型实例缓存
     * Key: platformIdentifier (如: local, aliyun_oss, minio)
     * Value: 原型实例（用于获取 Schema 和创建新实例）
     */
    private final Map<String, IStorageOperationService> prototypes = new ConcurrentHashMap<>();

    /**
     * 初始化：通过 SPI 加载插件
     */
    @PostConstruct
    public void loadPlugins() {
        log.info("开始加载存储插件...");

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

        log.info("存储插件加载完成，共加载 {} 个插件: {}", loadedCount, prototypes.keySet());

        if (loadedCount == 0) {
            log.warn("未加载到任何存储插件，请检查 META-INF/services 配置");
        }
    }

    /**
     * 获取插件原型实例
     *
     * @param platformIdentifier 平台标识符
     * @return 原型实例
     * @throws StorageOperationException 插件不存在时抛出
     */
    public IStorageOperationService getPrototype(String platformIdentifier) {
        IStorageOperationService prototype = prototypes.get(platformIdentifier);

        if (prototype == null) {
            throw new StorageOperationException(
                    String.format("不支持的存储平台: %s，可用平台: %s",
                            platformIdentifier, prototypes.keySet())
            );
        }

        return prototype;
    }

    /**
     * 检查插件是否存在
     *
     * @param platformIdentifier 平台标识符
     * @return true-存在
     */
    public boolean hasPlugin(String platformIdentifier) {
        return prototypes.containsKey(platformIdentifier);
    }

    /**
     * 获取所有可用平台标识符
     *
     * @return 平台标识符集合（不可修改）
     */
    public Set<String> getAvailablePlatforms() {
        return Collections.unmodifiableSet(prototypes.keySet());
    }

    /**
     * 获取插件数量
     *
     * @return 插件数量
     */
    public int getPluginCount() {
        return prototypes.size();
    }
}
