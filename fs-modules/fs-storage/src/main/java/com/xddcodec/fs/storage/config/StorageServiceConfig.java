package com.xddcodec.fs.storage.config;

import com.xddcodec.fs.storage.provider.StorageOperationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 存储服务配置
 *
 * @Author: hao.ding@insentek.com
 * @Date: 2025/5/12 11:17
 */
@Slf4j
@Configuration
public class StorageServiceConfig {

    @Bean("storageServiceMap")
    public Map<String, StorageOperationService> storageServiceMap(List<StorageOperationService> services) {
        Map<String, StorageOperationService> map = services.stream()
                .collect(Collectors.toMap(
                        StorageOperationService::getPlatformIdentifier,
                        service -> service,
                        (existing, replacement) -> existing
                ));
        log.info("存储服务 Map 初始化完成，可用服务: {}", map.keySet());
        return map;
    }
}
