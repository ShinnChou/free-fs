package com.xddcodec.fs.file.controller;

import com.xddcodec.fs.file.preview.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FilePreviewController {

    private final PreviewService previewService;

    @GetMapping("/preview/{fileId}")
    public String preview(@PathVariable String fileId, Model model) {
        try {
            return previewService.preview(fileId, model);
        } catch (Exception e) {
            log.error("预览失败: fileId={}", fileId, e);
            model.addAttribute("errorMessage", "预览失败: " + e.getMessage());
            return "error/500";
        }
    }
}
