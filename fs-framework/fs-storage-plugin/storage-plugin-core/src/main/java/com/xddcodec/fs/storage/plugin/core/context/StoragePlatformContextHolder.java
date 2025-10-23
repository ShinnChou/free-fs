package com.xddcodec.fs.storage.plugin.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 上下文持有者（ThreadLocal）
 */
@Slf4j
public class StoragePlatformContextHolder {

    private static final TransmittableThreadLocal<StoragePlatformContext> CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置上下文
     */
    public static void setContext(StoragePlatformContext context) {
        log.debug("设置存储平台上下文: userId={}, platform={}",
                context.getUserId(), context.getPlatformIdentifier());
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取上下文
     */
    public static StoragePlatformContext getContext() {
        StoragePlatformContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            throw new StorageOperationException("存储平台上下文未设置，请检查拦截器配置");
        }
        return context;
    }

    /**
     * 获取用户ID
     */
    public static String getUserId() {
        return getContext().getUserId();
    }

    /**
     * 获取平台标识符
     */
    public static String getPlatformIdentifier() {
        return getContext().getPlatformIdentifier();
    }


    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 检查上下文是否存在
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}
