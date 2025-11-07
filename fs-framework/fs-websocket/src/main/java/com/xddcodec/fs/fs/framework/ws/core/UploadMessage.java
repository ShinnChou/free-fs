package com.xddcodec.fs.fs.framework.ws.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadMessage {
    private UploadMessageType type;
    private String taskId;
    private Object data;
    private String message;
    private Long timestamp;

    public static UploadMessage progress(String taskId, UploadProgressDTO progress) {
        return new UploadMessage(UploadMessageType.progress, taskId, progress, null, System.currentTimeMillis());
    }

    public static UploadMessage complete(String taskId, String fileId) {
        return new UploadMessage(UploadMessageType.complete, taskId, fileId, "上传成功", System.currentTimeMillis());
    }

    public static UploadMessage error(String taskId, String error) {
        return new UploadMessage(UploadMessageType.error, taskId, null, error, System.currentTimeMillis());
    }

    public static UploadMessage success(String message) {
        return new UploadMessage(UploadMessageType.success, null, null, message, System.currentTimeMillis());
    }

    public static UploadMessage pong() {
        return new UploadMessage(UploadMessageType.pong, null, null, null, System.currentTimeMillis());
    }

    public static UploadMessage error(String message) {
        return new UploadMessage(UploadMessageType.error, null, null, message, System.currentTimeMillis());
    }
}

