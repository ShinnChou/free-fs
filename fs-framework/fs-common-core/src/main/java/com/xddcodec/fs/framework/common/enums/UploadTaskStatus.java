package com.xddcodec.fs.framework.common.enums;

/**
 * 上传任务状态枚举
 */
public enum UploadTaskStatus {
    /**
     * 等待上传
     */
    waiting,
    /**
     * 上传中
     */
    uploading,
    /**
     * 合并中
     */
    merging,
    /**
     * 上传成功
     */
    success,
    /**
     * 失败
     */
    failed,
    /**
     * 已暂停
     */
    paused,
    /**
     * 已完成
     */
    completed,
    /**
     * 已取消
     */
    canceled
}
