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

    /**
     * 文件预览入口
     */
    @GetMapping("/preview/{fileId}")
    public String preview(@PathVariable String fileId, Model model) {
        log.info("收到预览请求: fileId={}", fileId);
        
        try {
            return previewService.preview(fileId, model);
        } catch (Exception e) {
            log.error("预览过程发生未捕获异常: fileId={}", fileId, e);
            model.addAttribute("errorMessage", "系统错误");
            model.addAttribute("errorDetail", "预览过程中发生了意外错误");
            return "preview/error";
        }
    }
}
