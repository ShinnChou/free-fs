package com.xddcodec.fs.framework.preview.core;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class PreviewResult {

    /**
     * 模板名称
     */
    private String template;

    /**
     * 模板数据
     */
    private Map<String, Object> model;

    /**
     * 创建模板结果
     */
    public static PreviewResult ofTemplate(String template, Map<String, Object> model) {
        return PreviewResult.builder()
                .template(template)
                .model(model)
                .build();
    }
}
