package com.xddcodec.fs.framework.common.enums;

/**
 * 上传任务状态枚举
 */
public enum UploadTaskStatus {
    /**
     * 初始化
     */
    initialized,
    /**
     * 校验中 - 正在计算MD5
     */
    checking,
    /**
     * 上传中
     */
    uploading,
    /**
     * 合并中
     */
    merging,
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
