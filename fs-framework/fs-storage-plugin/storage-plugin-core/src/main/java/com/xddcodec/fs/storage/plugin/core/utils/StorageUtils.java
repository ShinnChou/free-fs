package com.xddcodec.fs.storage.plugin.core.utils;

import cn.hutool.core.util.StrUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;

/**
 * 存储工具类
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
public class StorageUtils {

    /**
     * 判断是否为 Local 存储配置
     *
     * @param configId 配置ID
     * @return true-是 Local 存储
     */
    public static boolean isLocalConfig(String configId) {
        return configId == null
                || StoragePlatformIdentifierEnum.LOCAL.getIdentifier().equals(configId);
    }

    /**
     * 规范化配置ID（Local 统一转为 null）
     *
     * @param configId 原始配置ID
     * @return 规范化后的配置ID
     */
    public static String normalizeConfigId(String configId) {
        return isLocalConfig(configId) ? null : configId;
    }

    /**
     * 规范化路径（去除末尾分隔符）
     *
     * @param path      路径
     * @param separator 分隔符
     * @return 规范化后的路径
     */
    public static String normalizePath(String path, String separator) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        String trimmed = path.trim();
        return trimmed.endsWith(separator)
                ? trimmed.substring(0, trimmed.length() - separator.length())
                : trimmed;
    }

    /**
     * 生成缓存键
     *
     * @param configId 配置ID
     * @return 缓存键
     */
    public static String generateCacheKey(String configId) {
        return isLocalConfig(configId)
                ? StoragePlatformIdentifierEnum.LOCAL.getIdentifier()
                : configId;
    }
}
