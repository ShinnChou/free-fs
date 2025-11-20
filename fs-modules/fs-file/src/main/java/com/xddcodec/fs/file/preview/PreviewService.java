package com.xddcodec.fs.file.preview;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.config.FilePreviewConfig;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import com.xddcodec.fs.framework.preview.factory.PreviewStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

/**
 * 预览服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewService {
    private final FileInfoService fileInfoService;
    private final PreviewStrategyManager strategyManager;
    private final FilePreviewConfig previewConfig;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public String preview(String fileId, Model model) {
        if (fileId == null || fileId.trim().isEmpty()) {
            log.warn("预览失败: fileId 为空");
            return buildErrorPage(model, "文件ID无效", "文件ID不能为空");
        }

        FileInfo fileInfo = null;
        try {
            fileInfo = fileInfoService.getById(fileId);
        } catch (Exception e) {
            log.error("查询文件信息失败: fileId={}", fileId, e);
            return buildErrorPage(model, "查询文件失败", "无法查询文件信息");
        }

        if (fileInfo == null) {
            log.warn("预览失败: 文件不存在, fileId={}", fileId);
            return buildErrorPage(model, "文件不存在", "文件不存在或已被删除");
        }

        if (fileInfo.getSize() != null && fileInfo.getSize() > previewConfig.getMaxFileSize()) {
            log.warn("预览失败: 文件过大, fileId={}, size={}MB", 
                    fileId, fileInfo.getSize() / 1024 / 1024);
            return buildErrorPage(model, "文件过大", 
                    String.format("文件大小为 %dMB，超过预览限制（%dMB），请下载后查看", 
                            fileInfo.getSize() / 1024 / 1024, 
                            previewConfig.getMaxFileSize() / 1024 / 1024));
        }

        FileTypeEnum fileType = FileTypeEnum.fromFileName(fileInfo.getDisplayName());
        if (!fileType.isPreviewable()) {
            log.warn("预览失败: 文件类型不支持预览, fileName={}, fileType={}", 
                    fileInfo.getDisplayName(), fileType.getName());
            return buildErrorPage(model, "不支持的文件类型", 
                    "该文件类型暂不支持在线预览，请下载后查看");
        }

        try {
            String streamUrl = buildUrl("/api/file/stream/preview/", fileId);
            PreviewContext context = PreviewContext.builder()
                    .fileName(fileInfo.getDisplayName())
                    .streamUrl(streamUrl)
                    .fileSize(fileInfo.getSize())
                    .extension(fileInfo.getSuffix())
                    .fileType(fileType)
                    .needConvert(fileType.isNeedConvert())
                    .build();

            PreviewStrategy strategy = strategyManager.getStrategy(fileType);
            strategy.fillModel(context, model);

            log.info("预览成功: fileName={}, fileType={}", fileInfo.getDisplayName(), fileType.getName());
            return fileType.getPreviewTemplate();
        } catch (Exception e) {
            log.error("构建预览上下文失败: fileId={}, fileName={}", fileId, fileInfo.getDisplayName(), e);
            return buildErrorPage(model, "预览失败", "无法加载文件预览");
        }
    }

    /**
     * 构建错误页面
     */
    private String buildErrorPage(Model model, String errorMessage, String errorDetail) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorDetail", errorDetail);
        return "preview/error";
    }

    private String buildUrl(String path, String fileId) {
        return (contextPath.isEmpty() ? "" : contextPath) + path + fileId;
    }
}
