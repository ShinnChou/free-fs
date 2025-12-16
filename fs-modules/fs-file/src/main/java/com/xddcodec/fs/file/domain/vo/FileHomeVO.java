package com.xddcodec.fs.file.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class FileHomeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long usedStorage;
    private Long fileCount;
    private Long directoryCount;
    private Long favoriteCount;
    private Long shareCount;
    private List<FileVO> recentFiles;
}
