package com.xddcodec.fs.framework.common.context;

import com.xddcodec.fs.framework.common.enums.StoragePlatformIdentifierEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 存储平台上下文持有者
 * 使用ThreadLocal在请求生命周期内传递当前使用的存储平台标识
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
public class StoragePlatformContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程使用的存储平台标识
     */
    public static void setPlatform(String platform) {
        log.debug("设置存储平台: {}", platform);
        CONTEXT_HOLDER.set(platform);
    }

    /**
     * 获取当前线程使用的存储平台标识
     */
    public static String getPlatform() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前线程使用的存储平台标识，如果未设置则返回默认值
     */
    public static String getPlatformOrDefault() {
        String platform = CONTEXT_HOLDER.get();
        if (platform == null) {
            log.warn("未设置存储平台，使用默认平台: {}", StoragePlatformIdentifierEnum.LOCAL.getIdentifier());
            return StoragePlatformIdentifierEnum.LOCAL.getIdentifier();
        }
        return platform;
    }

    /**
     * 清除当前线程的存储平台标识
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
