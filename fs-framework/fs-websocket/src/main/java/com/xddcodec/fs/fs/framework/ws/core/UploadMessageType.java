package com.xddcodec.fs.fs.framework.ws.core;

public enum UploadMessageType {
    // 系统消息
    success,      // 成功消息（如订阅成功）
    error,        // 错误消息
    pong,         // 心跳响应

    // 任务状态消息
    initialized,  // 任务初始化成功
    checking,     // 文件校验中
    quick_upload, // 秒传成功
    ready_to_upload, // 准备上传（校验完成，非秒传）

    // 上传过程消息
    progress,     // 上传进度
    paused,       // 已暂停
    resumed,      // 已继续
    merging,      // 合并中
    complete,     // 上传完成
    cancelled     // 已取消
}
