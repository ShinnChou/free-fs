package com.xddcodec.fs.file.domain.vo;

import com.xddcodec.fs.file.domain.FileTransferTask;
import com.xddcodec.fs.file.enums.TransferTaskStatus;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AutoMapper(target = FileTransferTask.class)
public class FileTransferTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 父目录ID
     */
    private String parentId;
    /**
     * 对象key
     */
    private String objectKey;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型(扩展名)
     */
    private String suffix;

    /**
     * 总分片数
     */
    private Integer totalChunks;
    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;
    /**
     * 分片大小(默认5MB)
     */
    private Long chunkSize;
    /**
     * 存储平台配置ID
     */
    private String storagePlatformSettingId;
    /**
     * 状态: uploading-上传中, paused-已暂停, completed-已完成, failed-失败, canceled-已取消
     */
    private TransferTaskStatus status;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
