package com.xddcodec.fs.fs.framework.ws.core;

public enum UploadMessageType {
    progress,     // 进度更新
    complete,     // 上传完成
    error,        // 错误
    success,      // 成功消息
    pong        // 心跳响应
}
