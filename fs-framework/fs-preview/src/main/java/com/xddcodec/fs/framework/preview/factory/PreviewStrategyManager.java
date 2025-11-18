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
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(PreviewStrategy::getPriority))
                .toList();
        log.info("加载 {} 个预览策略", strategies.size());
    }

    public PreviewStrategy getStrategy(FileTypeEnum fileType) {
        return strategies.stream()
                .filter(s -> s.support(fileType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "不支持的文件类型: " + fileType.getName()
                ));
    }
}
