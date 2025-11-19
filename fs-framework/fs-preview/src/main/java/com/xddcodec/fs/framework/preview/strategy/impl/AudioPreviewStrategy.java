package com.xddcodec.fs.framework.preview.strategy.impl;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 音频预览策略
 */
@Slf4j
@Component
public class AudioPreviewStrategy extends AbstractPreviewStrategy {

    @Override
    public boolean support(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.AUDIO;
    }

    @Override
    protected void fillSpecificModel(PreviewContext context, Model model) {
        log.info("音频预览策略填充完成 - 文件名: {}, 格式: {}, 大小: {}",
                context.getFileName(),
                context.getExtension(),
                context.getFileSize());
    }

    @Override
    public int getPriority() {
        return 16;
    }
}

