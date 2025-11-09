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
     * 分片上传线程池
     * 核心线程数 = CPU核心数 * 2
     * 最大线程数 = CPU核心数 * 4
     */
    @Bean("chunkUploadExecutor")
    public ThreadPoolTaskExecutor chunkUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();

        executor.setCorePoolSize(processors * 2);
        executor.setMaxPoolSize(processors * 4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("chunk-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * 文件合并线程池
     */
    @Bean("fileMergeExecutor")
    public ThreadPoolTaskExecutor fileMergeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("file-merge-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();

        return executor;
    }
}

