package com.xddcodec.fs.framework.preview.strategy.impl;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * ✅ PDF预览策略
 */
@Slf4j
@Component
public class PdfPreviewStrategy extends AbstractPreviewStrategy {

    @Override
    public boolean support(FileTypeEnum fileType) {
        // 支持 PDF 本身，以及需要转换为 PDF 的类型（Word/Excel/PPT）
        return fileType == FileTypeEnum.PDF
                || fileType == FileTypeEnum.WORD
                || fileType == FileTypeEnum.EXCEL
                || fileType == FileTypeEnum.PPT;
    }

    @Override
    protected void fillSpecificModel(PreviewContext context, Model model) {
        FileTypeEnum fileType = context.getFileType();

        Boolean needConvert = context.getNeedConvert();
        if (needConvert != null && needConvert) {
            model.addAttribute("needConvert", true);
            model.addAttribute("originalType", fileType.getName());
            model.addAttribute("convertStatus", "pending");

            log.info("Office 文档需要转换 - 文件: {}, 类型: {} -> PDF",
                    context.getFileName(), fileType.getName());
        } else {
            model.addAttribute("needConvert", false);
            log.info("PDF 文档直接预览 - 文件: {}", context.getFileName());
        }

        log.info("PDF 预览策略填充完成 - 文件名: {}", context.getFileName());
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
