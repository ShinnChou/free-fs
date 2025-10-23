package com.xddcodec.fs.storage.plugin.core.config;

import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 存储配置
 */
@Data
@Builder
public class StorageConfig {

    /**
     * 平台标识符
     */
    private String platformIdentifier;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 配置属性（JSON映射）
     */
    private Map<String, Object> properties;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 类型安全的属性获取
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        // 基础类型转换
        if (type == String.class) {
            return (T) value.toString();
        }
        if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (type == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        if (type == Boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        }

        throw new StorageOperationException(
                String.format("Property '%s' cannot be cast to %s", key, type.getSimpleName())
        );
    }

    /**
     * 获取必需属性（不存在则抛异常）
     */
    public <T> T getRequiredProperty(String key, Class<T> type) {
        T value = getProperty(key, type);
        if (value == null) {
            throw new StorageOperationException(
                    String.format("Required property '%s' is missing", key)
            );
        }
        return value;
    }

    /**
     * 获取属性（带默认值）
     */
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 生成缓存键
     * 格式：userId_platformIdentifier_configId
     */
    public String getCacheKey() {
        return String.format("%s_%s", userId, platformIdentifier);
    }
}
