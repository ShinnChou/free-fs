package com.xddcodec.fs.file.domain.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.xddcodec.fs.file.domain.FileShare;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author Yann
 * @date 2025/11/19 13:54
 */
@Data
@AutoMapper(target = FileShare.class)
public class FileShareThinVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private String id;
    /**
     * 分享名称
     */
    private String shareName;
    /**
     * 到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    /**
     * 文件数量
     */
    private Long fileCount;
    /**
     * 是否需要验证码
     */
    private Boolean hasCheckCode;
}
