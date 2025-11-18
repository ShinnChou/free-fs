package com.xddcodec.fs.framework.preview.service;

import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import com.xddcodec.fs.framework.preview.factory.PreviewStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * 预览服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewService {

    private final PreviewStrategyManager strategyManager;

    /**
     * 预览文件
     *
     * @param filePath 文件 URL
     * @param model    模板数据
     * @return 模板名称
     */
    public String preview(String filePath, Model model) {
        log.info("========== 开始预览文件 ==========");
        log.info("文件路径: {}", filePath);

        // 提取文件名
        String fileName = extractFileName(filePath);
        log.info("文件名: {}", fileName);

        // 判断文件类型
        FileTypeEnum fileType = FileTypeEnum.fromFileName(fileName);
        log.info("文件类型: {} ({})", fileType.getName(), fileType.getCode());
        log.info("是否支持预览: {}", fileType.isPreviewable());
        log.info("是否需要转换: {}", fileType.isNeedConvert());

        // 构建预览上下文
        PreviewContext context = PreviewContext.builder()
                .filePath(filePath)
                .fileName(fileName)
                .fileType(fileType)
                .build();

        // 获取对应策略
        PreviewStrategy strategy = strategyManager.getStrategy(fileType);
        log.info("选择策略: {}", strategy.getClass().getSimpleName());

        // 填充模板数据
        strategy.fillModel(context, model);

        // 返回模板路径
        String template = fileType.getPreviewTemplate();
        log.info("返回模板: {}", template);
        log.info("========== 预览处理完成 ==========");

        return template;
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileName(String url) {
        int lastSlash = url.lastIndexOf('/');
        int queryStart = url.indexOf('?');

        if (lastSlash != -1) {
            int endIndex = (queryStart > 0) ? queryStart : url.length();
            return url.substring(lastSlash + 1, endIndex);
        }

        return "未命名文件";
    }
}
