package com.xddcodec.fs.framework.common.constant;

/**
 * Redis Key 工具类
 */
public class RedisKey {

    private static final String BASE_KEY = "fs";
    private static final String SEPARATOR = ":";

    // 验证码
    private static final String VERIFY_CODE = "code";

    // 用户token
    private static final String USER_TOKEN = "token";

    /**
     * 上传任务信息缓存（Hash）
     * upload:task:{taskId}
     */
    public static final String UPLOAD_TASK_PREFIX = "upload:task:";

    /**
     * 已上传分片集合（Set）
     * upload:chunks:{taskId}
     */
    public static final String UPLOAD_CHUNKS_PREFIX = "upload:chunks:";

    /**
     * 已上传分片数计数器（String）
     * upload:count:{taskId}
     */
    public static final String UPLOAD_COUNT_PREFIX = "upload:count:";

    /**
     * 已上传字节数计数器（String）
     * upload:bytes:{taskId}
     */
    public static final String UPLOAD_BYTES_PREFIX = "upload:bytes:";

    /**
     * 上传任务锁（分布式锁）
     * upload:lock:{taskId}
     */
    public static final String UPLOAD_LOCK_PREFIX = "upload:lock:";

    /**
     * 上传开始时间（String）
     * upload:starttime:{taskId}
     */
    public static final String UPLOAD_START_TIME_PREFIX = "upload:starttime:";

    /**
     * 缓存过期时间（24小时）
     */
    public static final long CACHE_EXPIRE_SECONDS = 24 * 60 * 60;

    /**
     * 分布式锁过期时间（30秒）
     */
    public static final long LOCK_EXPIRE_SECONDS = 30;

    /**
     * 获取验证码key
     *
     * @param email 邮箱
     * @return fs:code:邮箱
     */
    public static String getVerifyCodeKey(String email) {
        return String.join(SEPARATOR, BASE_KEY, VERIFY_CODE, email);
    }

    /**
     * 获取用户token key
     *
     * @param userId 用户ID
     * @return fs:token:用户ID
     */
    public static String getUserTokenKey(Long userId) {
        return String.join(SEPARATOR, BASE_KEY, USER_TOKEN, String.valueOf(userId));
    }

    public static String getTaskKey(String taskId) {

        return String.join(SEPARATOR, BASE_KEY, UPLOAD_TASK_PREFIX, taskId);
    }

    public static String getChunksKey(String taskId) {
        return String.join(SEPARATOR, BASE_KEY, UPLOAD_CHUNKS_PREFIX, taskId);
    }

    public static String getCountKey(String taskId) {
        return String.join(SEPARATOR, BASE_KEY, UPLOAD_COUNT_PREFIX, taskId);
    }

    public static String getBytesKey(String taskId) {
        return String.join(SEPARATOR, BASE_KEY, UPLOAD_BYTES_PREFIX, taskId);
    }

    public static String getLockKey(String taskId) {
        return String.join(SEPARATOR, BASE_KEY, UPLOAD_LOCK_PREFIX, taskId);
    }

    public static String getStartTimeKey(String taskId) {
        return String.join(SEPARATOR, BASE_KEY, UPLOAD_START_TIME_PREFIX, taskId);
    }
}
