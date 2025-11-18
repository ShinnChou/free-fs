package com.xddcodec.fs.file.preview;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
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
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public String preview(String fileId, Model model) {
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            model.addAttribute("errorMessage", "文件不存在");
            return "error";
        }
        FileTypeEnum fileType = FileTypeEnum.fromFileName(fileInfo.getDisplayName());
        if (!fileType.isPreviewable()) {
            model.addAttribute("errorMessage", "该文件类型暂不支持预览");
            return "error/unsupported";
        }
        String streamUrl = buildUrl("/api/file/stream/preview/", fileId);
        PreviewContext context = PreviewContext.builder()
                .fileName(fileInfo.getDisplayName())
                .filePath(streamUrl)
                .fileSize(fileInfo.getSize())
                .extension(fileInfo.getSuffix())
                .fileType(fileType)
                .needConvert(fileType.isNeedConvert())
                .build();
        model.addAttribute("fileId", fileId);
        model.addAttribute("streamUrl", streamUrl);
        PreviewStrategy strategy = strategyManager.getStrategy(fileType);
        strategy.fillModel(context, model);
        return fileType.getPreviewTemplate();
    }

    private String buildUrl(String path, String fileId) {
        return (contextPath.isEmpty() ? "" : contextPath) + path + fileId;
    }
}
