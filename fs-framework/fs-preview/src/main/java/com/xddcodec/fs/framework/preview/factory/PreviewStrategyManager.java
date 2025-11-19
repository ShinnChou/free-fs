package com.xddcodec.fs.framework.preview.factory;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 预览策略管理器
 */
@Slf4j
@Component
public class PreviewStrategyManager {

    private final List<PreviewStrategy> strategies;
    private final PreviewStrategy defaultStrategy;

    public PreviewStrategyManager(List<PreviewStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(PreviewStrategy::getPriority))
                .toList();
        
        // 找到兜底策略（UnsupportedPreviewStrategy，优先级最低）
        this.defaultStrategy = strategies.stream()
                .max(Comparator.comparingInt(PreviewStrategy::getPriority))
                .orElse(null);
        
        log.info("加载 {} 个预览策略", strategies.size());
        if (defaultStrategy != null) {
            log.info("默认策略: {}", defaultStrategy.getClass().getSimpleName());
        }
    }

    /**
     * 获取预览策略
     * 注意：此方法不应该抛异常，因为Service层已经检查过isPreviewable()
     */
    public PreviewStrategy getStrategy(FileTypeEnum fileType) {
        PreviewStrategy strategy = strategies.stream()
                .filter(s -> s.support(fileType))
                .findFirst()
                .orElse(defaultStrategy);
        
        if (strategy == null) {
            log.error("找不到预览策略: fileType={}", fileType.getName());
            throw new IllegalStateException("预览策略未正确初始化");
        }
        
        return strategy;
    }
}
