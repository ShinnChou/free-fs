//package com.xddcodec.fs.framework.preview.strategy.impl;
//
//import com.github.junrar.rarfile.FileHeader;
//import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
//import com.xddcodec.fs.framework.preview.core.PreviewContext;
//import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.compress.archivers.ArchiveEntry;
//import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
//import org.apache.commons.compress.archivers.sevenz.SevenZFile;
//import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
//import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
//import org.springframework.stereotype.Component;
//import org.springframework.ui.Model;
//
//import java.io.BufferedInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardCopyOption;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Component
//public class ArchivePreviewStrategy extends AbstractPreviewStrategy {
//
//    @Override
//    public boolean support(FileTypeEnum fileType) {
//        return fileType == FileTypeEnum.ARCHIVE;
//    }
//
//    @Override
//    public String getTemplatePath() {
//        return "preview/archive";
//    }
//
//    @Override
//    protected void fillSpecificModel(PreviewContext context, Model model) {
//        InputStream inputStream = null;
//        try {
//            // 从 streamUrl 获取文件流
//            String streamUrl = context.getStreamUrl();
//            log.info("开始解析压缩包: {}, streamUrl: {}", context.getFileName(), streamUrl);
//
//            // 打开文件流
//            inputStream = new BufferedInputStream(new URL(streamUrl).openStream());
//
//            // 解析压缩包
//            List<ArchiveFileInfo> files = ArchiveUtil.parseArchive(inputStream, context.getFileName());
//
//            // 获取统计信息
//            Map<String, Object> stats = ArchiveUtil.getArchiveStats(files);
//
//            // 检测压缩包类型
//            String archiveType = ArchiveUtil.detectArchiveType(context.getFileName());
//
//            // 传递给模板
//            model.addAttribute("archiveFiles", files);
//            model.addAttribute("stats", stats);
//            model.addAttribute("archiveType", archiveType);
//
//            log.info("压缩包解析完成 - 文件数: {}, 文件夹数: {}, 总大小: {}bytes",
//                    stats.get("fileCount"),
//                    stats.get("folderCount"),
//                    stats.get("totalSize"));
//
//        } catch (Exception e) {
//            log.error("解析压缩包失败: {}", context.getFileName(), e);
//
//            // 传递错误信息给模板
//            model.addAttribute("error", "压缩包解析失败: " + e.getMessage());
//            model.addAttribute("archiveFiles", Collections.emptyList());
//            model.addAttribute("stats", Map.of(
//                    "fileCount", 0L,
//                    "folderCount", 0L,
//                    "totalSize", 0L
//            ));
//            model.addAttribute("archiveType", "UNKNOWN");
//
//        } finally {
//            // 关闭流
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (Exception e) {
//                    log.warn("关闭文件流失败", e);
//                }
//            }
//        }
//    }
//
//    @Override
//    public int getPriority() {
//        return 11;
//    }
//}
