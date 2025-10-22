package com.xddcodec.fs.file.domain;

import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏文件实体类
 *
 * @Author: xddcode
 * @Date: 2025/5/12 13:47
 */
@Data
@Table("file_user_favorites")
public class FileUserFavorites implements Serializable {

    @Serial
    private static final long serialVersionUID = 4725335083497380636L;

    private String userId;

    private String fileId;

    private LocalDateTime favoriteTime;
}
