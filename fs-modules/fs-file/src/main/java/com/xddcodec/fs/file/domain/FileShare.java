package com.xddcodec.fs.file.domain;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.xddcodec.fs.framework.orm.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件分享实体类
 *
 * @Author: xddcode
 * @Date: 2025/10/29 15:13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Table("file_shares")
public class FileShare extends BaseEntity {

    @Id(keyType = KeyType.None)
    private String id;

    /**
     * 分享人ID
     */
    private String userId;

    /**
     * 分享名称（默认取第一个文件名）
     */
    private String shareName;

    /**
     * 提取码（可选）
     */
    private String shareCode;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 查看次数
     */
    private Integer viewCount;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 最大查看次数（NULL表示无限制）
     */
    private Integer maxViewCount;

    /**
     * 最大下载次数（NULL表示无限制）
     */
    private Integer maxDownloadCount;

    /**
     * 是否已取消
     */
    private Boolean isCanceled;
}
