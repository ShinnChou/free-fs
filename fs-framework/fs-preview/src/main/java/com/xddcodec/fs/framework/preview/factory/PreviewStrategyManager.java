package com.xddcodec.fs.framework.preview.factory;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预览策略管理器
 */
@Slf4j
@Component
public class PreviewStrategyManager {

    private final List<PreviewStrategy> strategies;

    public PreviewStrategyManager(List<PreviewStrategy> strategies) {
        // 按优先级排序
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(PreviewStrategy::getPriority))
                .collect(Collectors.toList());

        log.info("加载了 {} 个预览策略", strategies.size());
        strategies.forEach(s -> log.info("  - {} (优先级: {})",
                s.getClass().getSimpleName(), s.getPriority()));
    }

    /**
     * 根据文件类型获取对应的策略
     */
    public PreviewStrategy getStrategy(FileTypeEnum fileType) {
        return strategies.stream()
                .filter(strategy -> strategy.support(fileType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("没有找到支持该文件类型的预览策略: " + fileType.getName()));
    }
}
