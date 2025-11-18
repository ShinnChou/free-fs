package com.xddcodec.fs.framework.preview.core;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import org.springframework.ui.Model;

public interface PreviewStrategy {

    /**
     * 是否支持该类型
     */
    boolean support(FileTypeEnum type);

    /**
     * 填充模板数据
     */
    void fillModel(PreviewContext context, Model model);

    /**
     * 优先级（数字越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}
