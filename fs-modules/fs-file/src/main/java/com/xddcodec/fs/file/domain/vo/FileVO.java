package com.xddcodec.fs.file.domain.vo;

import com.xddcodec.fs.file.domain.FileInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AutoMapper(target = FileInfo.class)
public class FileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    private String id;

    /**
     * 资源名称
     */
    private String objectKey;

    /**
     * 资源原始名称
     */
    private String originalName;

    /**
     * 资源别名
     */
    private String displayName;

    /**
     * 后缀名
     */
    private String suffix;

    /**
     * 大小
     */
    private Long size;

    /**
     * 存储标准MIME类型
     */
    private String mimeType;

    /**
     * 是否目录
     */
    private Boolean isDir;

    /**
     * 父目录ID
     */
    private String parentId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否收藏
     */
    private Boolean isFavorite;
}
