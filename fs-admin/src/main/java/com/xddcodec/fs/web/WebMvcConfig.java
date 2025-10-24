package com.xddcodec.fs.web;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.xddcodec.fs.framework.security.properties.SecurityProperties;
import com.xddcodec.fs.interceptor.StoragePlatformInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2024/11/18 13:53
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private StoragePlatformInterceptor storagePlatformInterceptor;

//    @Autowired
//    private FsServerProperties serverProperties;
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler(CommonConstant.LOCAL_DIRECTORY_MAPPING + "**")
//                .addResourceLocations("file:" + serverProperties.getLocal().getDirectory() + "/");
//    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 登录校验拦截器
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns(securityProperties.getPathPattern())
                .excludePathPatterns(securityProperties.getExcludes())
                .order(1);

        //注册存储平台切换拦截器
        registry.addInterceptor(storagePlatformInterceptor)
                .addPathPatterns(securityProperties.getPathPattern())
                .excludePathPatterns(securityProperties.getExcludes())
                .order(2);
    }
}
