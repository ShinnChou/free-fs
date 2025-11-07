package com.xddcodec.fs.fs.framework.ws.core;

import lombok.Data;

@Data
public class UploadProgressDTO {
    private Integer uploadedChunks;
    private Integer totalChunks;
    private Long uploadedSize;
    private Long totalSize;
    private Double progress;
    private Long speed;       // 字节/秒
    private Long remainTime;  // 剩余时间（秒）
}
