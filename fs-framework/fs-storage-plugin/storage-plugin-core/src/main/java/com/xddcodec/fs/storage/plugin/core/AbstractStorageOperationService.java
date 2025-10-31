package com.xddcodec.fs.storage.plugin.core;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.storage.plugin.core.config.StorageConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * 抽象存储操作服务
 * 提供公共的方法实现和初始化状态管理
 */
public abstract class AbstractStorageOperationService implements IStorageOperationService {

    /**
     * 配置（不可变）
     */
    protected final StorageConfig config;

    /**
     * 配置化构造函数
     */
    protected AbstractStorageOperationService(StorageConfig config) {
        this.config = Objects.requireNonNull(config, "StorageConfig cannot be null");
        validateConfig(config);
        initialize(config);
    }

    /**
     * 原型构造函数（SPI加载用）
     */
    protected AbstractStorageOperationService() {
        this.config = null;
    }

    /**
     * 验证配置（子类实现）
     * 职责：验证插件特有字段（如 endpoint、bucket、accessKey）
     */
    protected abstract void validateConfig(StorageConfig config);

    /**
     * 初始化资源（子类实现）
     */
    protected abstract void initialize(StorageConfig config);

    /**
     * 工厂方法默认实现
     */
    @Override
    public IStorageOperationService createConfiguredInstance(StorageConfig config) {
        try {
            Constructor<? extends IStorageOperationService> constructor =
                    this.getClass().getConstructor(StorageConfig.class);
            return constructor.newInstance(config);
        } catch (Exception e) {
            throw new StorageOperationException(
                    "Failed to create instance for platform: " + getPlatformIdentifier(), e
            );
        }
    }

    /**
     * 检查是否为原型实例
     */
    protected boolean isPrototype() {
        return config == null;
    }

    /**
     * 确保不是原型实例
     */
    protected void ensureNotPrototype() {
        if (isPrototype()) {
            throw new StorageOperationException(
                    "Cannot invoke business methods on prototype instance"
            );
        }
    }

    /**
     * MultipartFile上传的默认实现
     */
    public String uploadFile(MultipartFile file, String objectKeyPrefix) {
        ensureNotPrototype();

        if (file == null || file.isEmpty()) {
            throw new StorageOperationException("Upload file cannot be empty");
        }

        try {
            String originalFileName = file.getOriginalFilename();
            if (StrUtil.isBlank(originalFileName)) {
                originalFileName = "unknown-" + IdUtil.fastSimpleUUID();
            }

            // 构建对象键
            String prefix = StrUtil.isBlank(objectKeyPrefix) ? "" :
                    (objectKeyPrefix.endsWith("/") ? objectKeyPrefix : objectKeyPrefix + "/");

            String safeFileName = originalFileName.replaceAll("\\.\\./", "")
                    .replaceAll("\\.\\\\", "");
            String objectKey = prefix + IdUtil.fastSimpleUUID() + "-" + safeFileName;

            return uploadFile(file.getInputStream(), objectKey);
        } catch (IOException e) {
            throw new StorageOperationException("Upload file failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取日志前缀
     */
    protected String getLogPrefix() {
        if (config == null) {
            return "[Prototype]";
        }
        return String.format("[%s|%s]", config.getUserId(), config.getPlatformIdentifier());
    }
}