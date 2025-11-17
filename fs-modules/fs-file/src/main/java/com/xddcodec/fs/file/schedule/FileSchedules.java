package com.xddcodec.fs.file.schedule;

import com.xddcodec.fs.file.service.FileInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FileSchedules {

    private final FileInfoService fileInfoService;

    //定时清理回收站
    private void configureTasks() {
        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());
    }

}
