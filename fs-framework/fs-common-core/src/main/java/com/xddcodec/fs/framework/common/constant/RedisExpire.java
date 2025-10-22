package com.xddcodec.fs.framework.common.constant;

/**
 * Redis 过期时间常量
 */
public class RedisExpire {

    /**
     * 验证码过期时间：5分钟
     */
    public static final long VERIFY_CODE = 5 * 60;

    /**
     * 用户token过期时间：7天
     */
    public static final long USER_TOKEN = 7 * 24 * 60 * 60;
}

