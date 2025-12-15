package com.xddcodec.fs.file.preview;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
import com.xddcodec.fs.framework.preview.config.FilePreviewConfig;
import com.xddcodec.fs.framework.preview.core.PreviewContext;
import com.xddcodec.fs.framework.preview.core.PreviewStrategy;
import com.xddcodec.fs.framework.preview.factory.PreviewStrategyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;

/**
 * 预览服务
 */
@Service
@RequiredArgsConstructor
public class PreviewService {
    private final FileInfoService fileInfoService;
    private final PreviewStrategyManager strategyManager;
    private final FilePreviewConfig previewConfig;

    public String preview(String fileId, Model model) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return buildErrorPage(model, "文件ID无效", "文件ID不能为空");
        }
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            return buildErrorPage(model, "文件不存在", "文件不存在或已被删除");
        }

        if (fileInfo.getSize() > previewConfig.getMaxFileSize()) {
            return buildErrorPage(model, "文件过大",
                    String.format("文件大小超过预览限制（%dMB）",
                            previewConfig.getMaxFileSize() / 1024 / 1024));
        }

        FileTypeEnum fileType = FileTypeEnum.fromFileName(fileInfo.getDisplayName());
        PreviewStrategy strategy = strategyManager.getStrategy(fileType);
        if (strategy == null) {
            return buildErrorPage(model, "不支持的文件类型", "该文件类型暂不支持在线预览");
        }

        PreviewContext context = PreviewContext.builder()
                .fileName(fileInfo.getDisplayName())
                .streamUrl(previewConfig.getStreamApi() + "/" + fileId)
                .fileSize(fileInfo.getSize())
                .extension(fileInfo.getSuffix())
                .fileType(fileType)
                .build();
        strategy.fillModel(context, model);

        //修改文件访问记录
        fileInfo.setLastAccessTime(LocalDateTime.now());
        fileInfoService.updateById(fileInfo);
        return strategy.getTemplatePath();
    }

    /**
     * 构建错误页面
     */
    private String buildErrorPage(Model model, String errorMessage, String errorDetail) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorDetail", errorDetail);
        return "preview/error";
    }
}
