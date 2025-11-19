package com.xddcodec.fs.framework.preview.strategy.impl;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 图片预览策略
 */
@Slf4j
@Component
public class ImagePreviewStrategy extends AbstractPreviewStrategy {

    @Override
    public boolean support(FileTypeEnum fileType) {
        return fileType == FileTypeEnum.IMAGE;
    }

    @Override
    protected void fillSpecificModel(PreviewContext context, Model model) {
        log.info("图片预览策略填充完成 - 文件名: {}, 格式: {}, 大小: {}",
                context.getFileName(),
                context.getExtension(),
                context.getFileSize());
    }

    @Override
    public int getPriority() {
        return 10;
    }

}
