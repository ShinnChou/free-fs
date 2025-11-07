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
 * 分片记录表实体类
 *
 * @Author: xddcode
 * @Date: 2025/11/06 15:22
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table("file_upload_chunk")
public class FileUploadChunk extends BaseEntity {

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
     * 分片索引
     */
    private Integer chunkIndex;

    /**
     * 分片MD5
     */
    private String chunkMd5;

    /**
     * 分片大小
     */
    private Long chunkSize;

    /**
     * 分片存储路径
     */
    private String chunkPath;

    /**
     * 分片ETag（云存储返回的标识）
     */
    private String etag;

    /**
     * 状态
     */
    private UploadTaskStatus status;

    /**
     * 重试次数
     */
    private String retryCount;

    /**
     * 上传完成时间
     */
    private LocalDateTime uploadTime;
}
