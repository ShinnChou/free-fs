package com.xddcodec.fs.file.domain.vo;

import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileUploadTask;
import lombok.Data;

@Data
public class UploadInitVO {
    /**
     * 是否秒传
     */
    private Boolean instant;

    /**
     * 秒传时返回文件信息
     */
    private FileInfo fileInfo;

    /**
     * 需要上传时返回任务信息
     */
    private FileUploadTask task;

    /**
     * 提示信息
     */
    private String message;
}
