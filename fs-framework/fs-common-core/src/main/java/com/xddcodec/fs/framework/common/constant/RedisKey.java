package com.xddcodec.fs.framework.common.constant;

/**
 * Redis Key 工具类
 */
public class RedisKey {

    private static final String BASE_KEY = "fs";
    private static final String SEPARATOR = ":";

    // 验证码
    private static final String VERIFY_CODE = "code";

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
}
