package com.xddcodec.fs.storage.cache;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("storageSettingKeyGenerator")
public class StorageSettingKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String userId = StpUtil.getLoginIdAsString();
        return userId;
    }
}
