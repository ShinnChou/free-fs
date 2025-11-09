package com.xddcodec.fs.file.domain.dto;

import lombok.Data;

@Data
public class InitUploadCmd {
    private String fileName;
    private Long fileSize;
    private String fileMd5;
    private String parentId;
    private Integer totalChunks;
    private Long chunkSize;
    private String mimeType;
}
