package com.xddcodec.fs.framework.preview.office;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.preview.office", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JodConverterConfiguration {

    private final OfficeToPdfConfig config;

    @Bean
    public OfficeManager officeManager() {
        // 自动创建工作目录
        File workingDir = new File(config.getCachePath());
        if (!workingDir.exists()) {
            boolean created = workingDir.mkdirs();
            if (!created) {
                throw new IllegalStateException("无法创建工作目录: " + config.getCachePath());
            }
            log.info("创建工作目录: {}", workingDir.getAbsolutePath());
        }
        LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                .officeHome(config.getOfficeHome())
//                .poolSize(config.getPoolSize())
                .taskExecutionTimeout(config.getTaskExecutionTimeout())
                .taskQueueTimeout(config.getTaskQueueTimeout())
                .maxTasksPerProcess(config.getMaxTasksPerProcess())
                .workingDir(new File(config.getCachePath()));
        OfficeManager manager = builder.build();

        try {
            manager.start();
            log.info("LibreOffice 进程池启动成功: home={}, poolSize={}",
                    config.getOfficeHome(), config.getPoolSize());
        } catch (Exception e) {
            log.error("LibreOffice 进程池启动失败", e);
            throw new IllegalStateException("无法启动 LibreOffice，请检查安装路径: " + config.getOfficeHome(), e);
        }

        return manager;
    }
}
