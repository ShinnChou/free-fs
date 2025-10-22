package com.xddcodec.fs.storage.provider;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import com.xddcodec.fs.storage.config.LocalStorageProperties;
import com.xddcodec.fs.storage.domain.StorageSetting;
import com.xddcodec.fs.storage.provider.impl.AliyunOssStorageServiceImpl;
import com.xddcodec.fs.storage.provider.impl.DefaultStorageServiceImpl;
// import com.xddcodec.fs.storage.provider.impl.AliyunOssStorageServiceImpl;
// import com.xddcodec.fs.storage.provider.impl.MinioStorageServiceImpl;
import com.xddcodec.fs.storage.service.StorageSettingService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储服务工厂
 * 负责管理和获取不同平台的存储服务实现
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Component
public class StorageServiceFactory {
    /**
     * 平台标识 -> 服务实例原型
     */
    private final Map<String, Class<? extends StorageOperationService>> servicePrototypeMap = new ConcurrentHashMap<>();

    /**
     * 用户ID_平台标识 -> 已初始化的服务实例缓存
     */
    private final Map<String, StorageOperationService> serviceCache = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocalStorageProperties localStorageProperties;

    @Autowired
    private StorageSettingService storageSettingService;

    /**
     * 注册所有存储服务实现类
     */
    @PostConstruct
    public void init() {
        // 注册本地存储
        registerStorageService(StoragePlatformIdentifierEnum.LOCAL, DefaultStorageServiceImpl.class);

        // 注册MinIO存储（需要添加MinIO SDK依赖）
        // registerStorageService(StoragePlatformIdentifierEnum.MINIO, MinioStorageServiceImpl.class);

        // 注册阿里云OSS（需要添加阿里云OSS SDK依赖）
         registerStorageService(StoragePlatformIdentifierEnum.ALIYUN_OSS, AliyunOssStorageServiceImpl.class);

        log.info("存储服务工厂初始化完成，已注册 {} 个存储平台", servicePrototypeMap.size());
    }

    /**
     * 注册存储服务实现类
     */
    private void registerStorageService(StoragePlatformIdentifierEnum platform, 
                                       Class<? extends StorageOperationService> serviceClass) {
        servicePrototypeMap.put(platform.getIdentifier(), serviceClass);
        log.debug("注册存储平台: {} -> {}", platform.getIdentifier(), serviceClass.getSimpleName());
    }

    /**
     * 根据平台标识获取存储服务（从上下文获取平台标识，从Sa-Token获取用户ID）
     * 这是对外的主要接口
     */
    public StorageOperationService getService(String platformIdentifier) {
        // 获取当前用户ID（如果用户已登录）
        String userId = null;
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsString();
            }
        } catch (Exception e) {
            log.warn("获取用户ID失败: {}", e.getMessage());
        }

        // 生成缓存Key
        String cacheKey = buildCacheKey(userId, platformIdentifier);

        // 先从缓存获取
        StorageOperationService cachedService = serviceCache.get(cacheKey);
        if (cachedService != null && cachedService.isInitialized()) {
            log.debug("从缓存获取存储服务: cacheKey={}", cacheKey);
            return cachedService;
        }

        // 缓存未命中，创建新实例
        synchronized (this) {
            // 双重检查锁定
            cachedService = serviceCache.get(cacheKey);
            if (cachedService != null && cachedService.isInitialized()) {
                return cachedService;
            }

            StorageOperationService service = createAndInitService(userId, platformIdentifier);

            // 放入缓存
            serviceCache.put(cacheKey, service);
            log.info("创建并缓存存储服务: cacheKey={}", cacheKey);

            return service;
        }
    }

    /**
     * 创建并初始化存储服务
     */
    private StorageOperationService createAndInitService(String userId, String platformIdentifier) {
        // 1. 创建服务实例
        StorageOperationService service = createServiceInstance(platformIdentifier);

        // 2. 加载配置
        String configData = loadConfigData(userId, platformIdentifier);

        // 3. 初始化服务
        service.init(configData);

        log.info("创建存储服务成功: userId={}, platform={}", userId, platformIdentifier);

        return service;
    }

    /**
     * 创建服务实例
     */
    private StorageOperationService createServiceInstance(String platformIdentifier) {
        Class<? extends StorageOperationService> serviceClass = servicePrototypeMap.get(platformIdentifier);

        if (serviceClass == null) {
            throw new StorageOperationException("不支持的存储平台: " + platformIdentifier);
        }

        try {
            return serviceClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("创建存储服务实例失败: {}", platformIdentifier, e);
            throw new StorageOperationException("创建存储服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加载配置数据
     */
    private String loadConfigData(String userId, String platformIdentifier) {
        // 如果是本地存储，使用配置文件中的配置
        if (StoragePlatformIdentifierEnum.LOCAL.getIdentifier().equals(platformIdentifier)) {
            return buildLocalConfigJson();
        }

        // 其他存储平台从数据库加载
        return loadConfigFromDatabase(userId, platformIdentifier);
    }

    /**
     * 构建本地存储配置JSON
     */
    private String buildLocalConfigJson() {
        try {
            Map<String, String> config = Map.of(
                    "basePath", localStorageProperties.getBasePath(),
                    "baseUrl", localStorageProperties.getBaseUrl()
            );
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new StorageOperationException("构建本地存储配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从数据库加载配置
     */
    private String loadConfigFromDatabase(String userId, String platformIdentifier) {
        if (StringUtils.isBlank(userId)) {
            throw new StorageOperationException("用户ID不能为空，无法加载存储配置");
        }

        // 使用服务查询数据库配置
        StorageSetting settings = storageSettingService.getStorageSettingByPlatform(platformIdentifier, userId);

        if (settings == null) {
            throw new StorageOperationException(
                    String.format("未找到存储配置: userId=%s, platform=%s", userId, platformIdentifier)
            );
        }

        // enabled 字段是 Integer 类型，1表示启用，0表示禁用
        if (settings.getEnabled() == null || settings.getEnabled() != 1) {
            throw new StorageOperationException(
                    String.format("存储配置未启用: userId=%s, platform=%s", userId, platformIdentifier)
            );
        }

        return settings.getConfigData();
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(String userId, String platformIdentifier) {
        // 本地存储不区分用户，所有用户共享同一个本地存储实例
        if (StoragePlatformIdentifierEnum.LOCAL.getIdentifier().equals(platformIdentifier)) {
            return "GLOBAL_LOCAL";
        }
        // 其他存储平台需要区分用户
        if (StringUtils.isBlank(userId)) {
            throw new StorageOperationException("非本地存储平台需要用户ID: " + platformIdentifier);
        }
        return userId + "_" + platformIdentifier;
    }

    /**
     * 清除指定用户的缓存
     */
    public void clearUserCache(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        serviceCache.keySet().removeIf(key -> key.startsWith(userId + "_"));
        log.info("清除用户存储服务缓存: userId={}", userId);
    }

    /**
     * 清除指定用户指定平台的缓存
     */
    public void clearCache(String userId, String platformIdentifier) {
        String cacheKey = buildCacheKey(userId, platformIdentifier);
        serviceCache.remove(cacheKey);
        log.info("清除存储服务缓存: cacheKey={}", cacheKey);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        serviceCache.clear();
        log.info("清除所有存储服务缓存");
    }

    /**
     * 检查是否支持指定平台
     */
    public boolean isSupported(String platformIdentifier) {
        return servicePrototypeMap.containsKey(platformIdentifier);
    }

    /**
     * 获取所有支持的平台
     */
    public List<String> getSupportedPlatforms() {
        return List.copyOf(servicePrototypeMap.keySet());
    }
}
