package com.xddcodec.fs.framework.preview.office;

import com.xddcodec.fs.framework.preview.converter.impl.OfficeToPdfConverter;
import com.xddcodec.fs.framework.preview.strategy.impl.OfficePreviewStrategy;
import jakarta.annotation.PreDestroy;
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
@ConditionalOnProperty(prefix = "fs.preview.office", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JodConverterConfiguration {

    private final OfficeToPdfConfig config;

    private OfficeManager officeManager;

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
                .taskExecutionTimeout(config.getTaskExecutionTimeout())
                .taskQueueTimeout(config.getTaskQueueTimeout())
                .maxTasksPerProcess(config.getMaxTasksPerProcess())
                .workingDir(new File(config.getCachePath()));
        officeManager = builder.build();

        try {
            officeManager.start();
            log.info("LibreOffice 进程池启动成功: home={}, poolSize={}",
                    config.getOfficeHome(), config.getPoolSize());
        } catch (Exception e) {
            log.error("LibreOffice 进程池启动失败", e);
        }

        return officeManager;
    }

    @Bean
    public OfficeToPdfConverter officeToPdfConverter(OfficeManager officeManager, OfficeToPdfConfig config) {
        return new OfficeToPdfConverter(officeManager, config);
    }

    @Bean
    public OfficePreviewStrategy officePreviewStrategy(OfficeToPdfConverter officeToPdfConverter) {
        return new OfficePreviewStrategy(officeToPdfConverter);
    }

    /**
     * 确保项目关闭时，LibreOffice 也能关闭
     */
    @PreDestroy
    public void destroy() {
        if (officeManager != null && officeManager.isRunning()) {
            log.info("正在关闭 LibreOffice 进程...");
            try {
                officeManager.stop();
            } catch (Exception e) {
                log.error("关闭 LibreOffice 异常", e);
            }
        }
    }
}
