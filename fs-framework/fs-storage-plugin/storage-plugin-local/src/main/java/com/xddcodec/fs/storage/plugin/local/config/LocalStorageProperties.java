package com.xddcodec.fs.storage.plugin.local.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地存储配置（从application.yml读取）
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.local")
public class LocalStorageProperties {

    /**
     * 存储基础路径
     */
    private String basePath = "/data/files";

    /**
     * 访问基础URL
     */
    private String baseUrl = "http://localhost:8080/files";
}
