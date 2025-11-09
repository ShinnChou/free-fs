package com.xddcodec.fs.fs.framework.ws.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadProgressDTO {
    private String taskId;
    private Integer uploadedChunks;
    private Integer totalChunks;
    private Long uploadedSize;
    private Long totalSize;
    private Double progress;
    private Long speed;       // 字节/秒
    private Integer remainTime;  // 剩余时间（秒）
}
