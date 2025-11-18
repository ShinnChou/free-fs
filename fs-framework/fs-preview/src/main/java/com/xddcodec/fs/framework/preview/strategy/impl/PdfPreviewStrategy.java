package com.xddcodec.fs.framework.preview.strategy.impl;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.Map;

/**
 * PDF预览策略
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

        // 判断是否需要转换
        if (fileType.isNeedConvert()) {
            log.info("文件需要转换: {} -> PDF", fileType.getName());

            // 🔄 TODO: 这里调用文件转换服务，将 Office 文档转为 PDF
            // String convertedPdfUrl = fileConvertService.convertToPdf(context.getFilePath());
            // model.addAttribute("pdfUrl", convertedPdfUrl);

            // 临时：直接使用原文件路径（实际应用中需要替换为转换后的 PDF URL）
            model.addAttribute("pdfUrl", context.getFilePath());
            model.addAttribute("needConvert", true);
            model.addAttribute("originalType", fileType.getName());
        } else {
            // PDF 文件直接预览
            model.addAttribute("pdfUrl", context.getFilePath());
            model.addAttribute("needConvert", false);
        }

        model.addAttribute("usePdfJs", true);
        model.addAttribute("toolbarEnabled", true);
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }
}
