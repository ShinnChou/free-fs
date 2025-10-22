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
}
