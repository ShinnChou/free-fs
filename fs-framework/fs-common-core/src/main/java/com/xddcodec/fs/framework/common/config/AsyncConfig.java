package com.xddcodec.fs.framework.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 用于文件上传异步处理
 *
 * @Author: xddcode
 * @Date: 2025/11/07
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 文件上传任务线程池
     * 支持批量上传：5个文件 × 3个分片/文件 = 15个并发
     */
    @Bean("uploadTaskExecutor")
    public ThreadPoolTaskExecutor uploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);          // 核心线程数（支持15个并发分片）
        executor.setMaxPoolSize(20);           // 最大线程数
        executor.setQueueCapacity(100);        // 队列容量
        executor.setThreadNamePrefix("upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

