//package com.xddcodec.fs.framework.preview.strategy.impl;
//
//import com.xddcodec.fs.framework.common.enums.FileTypeEnum;
//import com.xddcodec.fs.framework.preview.core.PreviewContext;
//import com.xddcodec.fs.framework.preview.strategy.AbstractPreviewStrategy;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
///**
// * 图片预览策略
// */
//@Component
//public class ImagePreviewStrategy extends AbstractPreviewStrategy {
//
//    @Override
//    public boolean support(FileTypeEnum fileType) {
//        return fileType == FileTypeEnum.IMAGE;
//    }
//
//    @Override
//    protected void enhanceModel(PreviewContext context, Map<String, Object> model) {
//        model.put("imageUrl", buildFileUrl(context.getFileId()));
//    }
//
//    @Override
//    public int getPriority() {
//        return 10;
//    }
//}
