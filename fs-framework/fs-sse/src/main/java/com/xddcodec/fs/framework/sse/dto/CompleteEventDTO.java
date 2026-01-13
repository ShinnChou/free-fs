package com.xddcodec.fs.framework.sse.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 完成事件数据传输对象
 * 
 * @author xddcodec
 */
@Data
@Builder
public class CompleteEventDTO {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 文件ID
     */
    private String fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
}
