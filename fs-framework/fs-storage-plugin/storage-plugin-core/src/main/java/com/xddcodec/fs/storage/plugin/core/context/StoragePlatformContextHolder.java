package com.xddcodec.fs.storage.plugin.core.context;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 存储平台上下文持有者（ThreadLocal）
 * 用于在同一请求线程中传递存储上下文信息
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
public class StoragePlatformContextHolder {

    /**
     * 使用阿里 TTL（TransmittableThreadLocal）
     * 支持线程池场景下的上下文传递
     */
    private static final TransmittableThreadLocal<StoragePlatformContext> CONTEXT_HOLDER =
            new TransmittableThreadLocal<>();

    /**
     * 设置上下文
     *
     * @param context 存储平台上下文
     * @throws IllegalArgumentException 如果 context 为 null
     */
    public static void setContext(StoragePlatformContext context) {
        if (context == null) {
            throw new IllegalArgumentException("存储平台上下文不能为空");
        }

        log.debug("设置存储平台上下文: userId={}, configId={}, isLocal={}",
                context.getUserId(),
                context.getConfigId(),
                context.isLocal());

        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取上下文
     *
     * @return 存储平台上下文
     * @throws StorageOperationException 如果上下文未设置
     */
    public static StoragePlatformContext getContext() {
        StoragePlatformContext context = CONTEXT_HOLDER.get();

        if (context == null) {
            throw new StorageOperationException(
                    "存储平台上下文未设置，请检查拦截器配置或确保在 HTTP 请求中访问"
            );
        }

        return context;
    }

    /**
     * 获取上下文（不抛异常）
     *
     * @return 存储平台上下文，如果未设置返回 null
     */
    public static StoragePlatformContext getContextOrNull() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     * @throws StorageOperationException 如果上下文未设置
     */
    public static String getUserId() {
        return getContext().getUserId();
    }

    /**
     * 获取用户ID（不抛异常）
     *
     * @return 用户ID，如果上下文未设置返回 null
     */
    public static String getUserIdOrNull() {
        StoragePlatformContext context = getContextOrNull();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取配置ID
     *
     * @return 配置ID（Local 存储返回 null）
     * @throws StorageOperationException 如果上下文未设置
     */
    public static String getConfigId() {
        return getContext().getConfigId();
    }

    /**
     * 获取配置ID（不抛异常）
     *
     * @return 配置ID，如果上下文未设置返回 null
     */
    public static String getConfigIdOrNull() {
        StoragePlatformContext context = getContextOrNull();
        return context != null ? context.getConfigId() : null;
    }

    /**
     * 获取规范化的配置ID
     *
     * @return 规范化后的配置ID（Local 返回 null）
     * @throws StorageOperationException 如果上下文未设置
     */
    public static String getNormalizedConfigId() {
        return getContext().getNormalizedConfigId();
    }

    /**
     * 判断当前是否为 Local 存储
     *
     * @return true-Local 存储
     * @throws StorageOperationException 如果上下文未设置
     */
    public static boolean isLocal() {
        return getContext().isLocal();
    }

    /**
     * 判断当前是否为 Local 存储（不抛异常）
     *
     * @return true-Local 存储，如果上下文未设置返回 false
     */
    public static boolean isLocalOrDefault() {
        StoragePlatformContext context = getContextOrNull();
        return context != null && context.isLocal();
    }

    /**
     * 清除上下文
     * 必须在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        StoragePlatformContext context = CONTEXT_HOLDER.get();

        if (context != null) {
            log.debug("清除存储平台上下文: userId={}, configId={}",
                    context.getUserId(),
                    context.getConfigId());
        }

        CONTEXT_HOLDER.remove();
    }

    /**
     * 检查上下文是否存在
     *
     * @return true-上下文已设置
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }

    /**
     * 手动设置上下文（用于测试或异步任务）
     *
     * @param userId   用户ID
     * @param configId 配置ID
     */
    public static void setContext(String userId, String configId) {
        StoragePlatformContext context = StoragePlatformContext.builder()
                .userId(userId)
                .configId(configId)
                .build();

        setContext(context);
    }

    /**
     * 在指定上下文中执行操作（自动清理）
     * 适用于异步任务或测试场景
     *
     * @param userId   用户ID
     * @param configId 配置ID
     * @param runnable 要执行的操作
     */
    public static void runInContext(String userId, String configId, Runnable runnable) {
        try {
            setContext(userId, configId);
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * 获取上下文摘要信息（用于日志）
     *
     * @return 上下文摘要字符串
     */
    public static String getContextSummary() {
        StoragePlatformContext context = getContextOrNull();

        if (context == null) {
            return "[上下文未设置]";
        }

        return String.format("[userId=%s, configId=%s, isLocal=%s]",
                StrUtil.emptyToDefault(context.getUserId(), "null"),
                StrUtil.emptyToDefault(context.getConfigId(), "null"),
                context.isLocal());
    }
}
