package com.xddcodec.fs.interceptor;

import cn.hutool.core.util.StrUtil;
import com.xddcodec.fs.framework.common.constant.CommonConstant;
import com.xddcodec.fs.framework.common.context.StoragePlatformContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 存储平台拦截器
 * 从请求头中提取存储平台标识并设置到上下文中
 *
 * @Author: xddcode
 * @Date: 2024/10/26
 */
@Slf4j
@Component
public class StoragePlatformInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String platform = request.getHeader(CommonConstant.X_STORAGE_PLATFORM);

        if (StrUtil.isNotBlank(platform)) {
            StoragePlatformContextHolder.setPlatform(platform);
            log.debug("从请求头获取存储平台: {}", platform);
        } else {
            log.debug("请求头中未包含存储平台标识，将使用默认平台");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求完成后清理ThreadLocal，防止内存泄漏
        StoragePlatformContextHolder.clear();
    }
}