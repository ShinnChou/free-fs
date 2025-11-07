package com.xddcodec.fs.framework.common.enums;

/**
 * 上传任务状态枚举
 */
public enum UploadTaskStatus {
    /**
     * 上传中
     */
    uploading,
    /**
     * 已暂停
     */
    paused,
    /**
     * 已完成
     */
    completed,
    /**
     * 失败
     */
    failed,
    /**
     * 已取消
     */
    canceled
}
