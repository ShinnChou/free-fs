//package com.xddcodec.fs.framework.preview.strategy.impl;
//
//import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
//import com.xddcodec.fs.framework.preview.core.PreviewContext;
//import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.ui.Model;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.File;
//
///**
// * 图片预览策略
// */
//@Slf4j
//@Component
//public class ImagePreviewStrategy extends AbstractPreviewStrategy {
//
//    @Override
//    public boolean support(FileTypeEnum fileType) {
//        return fileType == FileTypeEnum.IMAGE;
//    }
//
//    @Override
//    protected void fillSpecificModel(PreviewContext context, Model model) {
//        // 文件大小（格式化显示）
//        if (context.getFileSize() != null) {
//            model.addAttribute("fileSizeBytes", context.getFileSize());
//            model.addAttribute("fileSizeFormatted", formatFileSize(context.getFileSize()));
//        }
//        // 图片格式信息
//        if (context.getExtension() != null) {
//            model.addAttribute("imageFormat", context.getExtension().toUpperCase());
//            model.addAttribute("imageType", context.getExtension().toLowerCase());
//        }
//        // 尝试读取图片尺寸信息（如果 filePath 是本地文件路径）
//        readImageDimensions(context, model);
//        // 预览功能配置
//        model.addAttribute("enableZoom", true);
//        model.addAttribute("enableRotate", true);
//        model.addAttribute("enableDownload", true);
//
//        log.info("图片预览策略填充完成 - 文件名: {}, 格式: {}, 大小: {}",
//                context.getFileName(),
//                context.getExtension(),
//                context.getFileSize());
//    }
//
//    /**
//     * 读取图片尺寸信息
//     * 注意：仅当 filePath 为本地文件路径时有效
//     */
//    private void readImageDimensions(PreviewContext context, Model model) {
//        try {
//            File imageFile = new File(context.getFilePath());
//
//            // 仅当文件真实存在时才读取
//            if (imageFile.exists() && imageFile.isFile()) {
//                BufferedImage bufferedImage = ImageIO.read(imageFile);
//
//                if (bufferedImage != null) {
//                    int width = bufferedImage.getWidth();
//                    int height = bufferedImage.getHeight();
//
//                    model.addAttribute("imageWidth", width);
//                    model.addAttribute("imageHeight", height);
//                    model.addAttribute("imageDimensions", width + " × " + height);
//
//                    log.debug("成功读取图片尺寸: {}x{}", width, height);
//                } else {
//                    log.debug("ImageIO 无法解析图片: {}", context.getFileName());
//                    setDefaultDimensions(model);
//                }
//            } else {
//                // filePath 是 URL 路径，无法读取本地文件
//                log.debug("文件路径非本地文件，跳过尺寸读取: {}", context.getFilePath());
//                setDefaultDimensions(model);
//            }
//        } catch (Exception e) {
//            log.warn("读取图片尺寸失败: {}, 原因: {}", context.getFileName(), e.getMessage());
//            setDefaultDimensions(model);
//        }
//    }
//
//    /**
//     * 设置默认尺寸信息
//     */
//    private void setDefaultDimensions(Model model) {
//        model.addAttribute("imageWidth", null);
//        model.addAttribute("imageHeight", null);
//        model.addAttribute("imageDimensions", "未知");
//    }
//
//    /**
//     * 格式化文件大小
//     */
//    private String formatFileSize(long sizeInBytes) {
//        if (sizeInBytes < 1024) {
//            return sizeInBytes + " B";
//        } else if (sizeInBytes < 1024 * 1024) {
//            return String.format("%.2f KB", sizeInBytes / 1024.0);
//        } else if (sizeInBytes < 1024 * 1024 * 1024) {
//            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
//        } else {
//            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
//        }
//    }
//
//    @Override
//    public int getPriority() {
//        return 10;
//    }
//
//}
