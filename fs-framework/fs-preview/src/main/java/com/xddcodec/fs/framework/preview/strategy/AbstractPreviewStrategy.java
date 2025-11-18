package com.xddcodec.fs.framework.preview.strategy;

import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;


/**
 * 抽象预览策略（提供通用功能）
 */
@Slf4j
public abstract class AbstractPreviewStrategy implements PreviewStrategy {

    @Override
    public void fillModel(PreviewContext context, Model model) {
        // 填充基础数据
        model.addAttribute("filePath", context.getFilePath());
        model.addAttribute("fileName", context.getFileName());
        model.addAttribute("fileType", context.getFileType().getName());

        // 子类填充特定数据
        fillSpecificModel(context, model);

        log.info("策略 [{}] 填充完成", this.getClass().getSimpleName());
    }

    /**
     * 子类实现：填充特定数据
     */
    protected abstract void fillSpecificModel(PreviewContext context, Model model);
}

