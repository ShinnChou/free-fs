package com.xddcodec.fs.file.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import com.xddcodec.fs.framework.orm.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 上传任务表实体类
 *
 * @Author: xddcode
 * @Date: 2025/11/06 15:22
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table("file_upload_task")
public class FileUploadTask extends BaseEntity {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;
    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 唯一上传ID
     */
    private String uploadId;
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
     * 文件MD5值
     */
    private String fileMd5;
    /**
     * 文件类型(扩展名)
     */
    private String suffix;
    /**
     * 存储标准MIME类型
     */
    private String mimeType;
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
    private UploadTaskStatus status;
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