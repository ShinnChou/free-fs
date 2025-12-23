package com.xddcodec.fs.framework.preview.office;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "fs.preview.office")
public class OfficeToPdfConfig {

    /**
     * LibreOffice 安装路径
     * Linux: /usr/lib/libreoffice
     * Windows: C:/Program Files/LibreOffice
     * Mac: /Applications/LibreOffice.app/Contents
     */
    private String officeHome = "C:/Program Files/LibreOffice";

    /**
     * 进程池大小
     */
    private Integer poolSize = 2;

    /**
     * 任务执行超时（毫秒）
     */
    private Long taskExecutionTimeout = 120000L;

    /**
     * 任务队列超时（毫秒）
     */
    private Long taskQueueTimeout = 30000L;

    /**
     * 最大任务数
     */
    private Integer maxTasksPerProcess = 200;

    /**
     * 是否启用转换
     */
    private Boolean enabled = true;

    /**
     * 转换缓存目录
     */
    private String cachePath = "/tmp/office-convert";
}
