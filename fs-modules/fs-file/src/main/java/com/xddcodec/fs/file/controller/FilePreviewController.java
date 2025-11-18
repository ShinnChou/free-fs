package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.framework.preview.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FilePreviewController {

    private final PreviewService previewService;

    @GetMapping("/preview")
    public String preview(@RequestParam("filePath") String filePath, Model model) {
        log.info("收到预览请求: {}", filePath);
        return previewService.preview(filePath, model);
    }
}
