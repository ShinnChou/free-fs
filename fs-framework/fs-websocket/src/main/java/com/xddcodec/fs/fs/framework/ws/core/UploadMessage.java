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

    public static UploadMessage success(String message) {
        return new UploadMessage(UploadMessageType.success, null, null, message, System.currentTimeMillis());
    }

    public static UploadMessage error(String message) {
        return new UploadMessage(UploadMessageType.error, null, null, message, System.currentTimeMillis());
    }

    public static UploadMessage error(String taskId, String error) {
        return new UploadMessage(UploadMessageType.error, taskId, null, error, System.currentTimeMillis());
    }

    public static UploadMessage pong() {
        return new UploadMessage(UploadMessageType.pong, null, null, null, System.currentTimeMillis());
    }

    /**
     * 任务初始化成功
     */
    public static UploadMessage initialized(String taskId) {
        return new UploadMessage(
                UploadMessageType.initialized,
                taskId,
                null,
                "任务创建成功，请计算文件MD5",
                System.currentTimeMillis()
        );
    }

    /**
     * 文件校验中
     */
    public static UploadMessage checking(String taskId) {
        return new UploadMessage(
                UploadMessageType.checking,
                taskId,
                null,
                "正在校验文件...",
                System.currentTimeMillis()
        );
    }

    /**
     * 秒传成功
     */
    public static UploadMessage quickUpload(String taskId, String fileId) {
        return new UploadMessage(
                UploadMessageType.quick_upload,
                taskId,
                fileId,
                "秒传成功",
                System.currentTimeMillis()
        );
    }

    /**
     * 准备上传（校验完成，非秒传）
     */
    public static UploadMessage readyToUpload(String taskId, String uploadId) {
        return new UploadMessage(
                UploadMessageType.ready_to_upload,
                taskId,
                uploadId,
                "校验完成，可以开始上传",
                System.currentTimeMillis()
        );
    }

    /**
     * 上传进度
     */
    public static UploadMessage progress(String taskId, UploadProgressDTO progress) {
        return new UploadMessage(
                UploadMessageType.progress,
                taskId,
                progress,
                null,
                System.currentTimeMillis()
        );
    }

    /**
     * 已暂停
     */
    public static UploadMessage paused(String taskId) {
        return new UploadMessage(
                UploadMessageType.paused,
                taskId,
                null,
                "上传已暂停",
                System.currentTimeMillis()
        );
    }

    /**
     * 已继续
     */
    public static UploadMessage resumed(String taskId, Object uploadedChunks) {
        return new UploadMessage(
                UploadMessageType.resumed,
                taskId,
                uploadedChunks,
                "上传已继续",
                System.currentTimeMillis()
        );
    }

    /**
     * 合并中
     */
    public static UploadMessage merging(String taskId) {
        return new UploadMessage(
                UploadMessageType.merging,
                taskId,
                null,
                "正在合并文件...",
                System.currentTimeMillis()
        );
    }

    /**
     * 上传完成
     */
    public static UploadMessage complete(String taskId, String fileId) {
        return new UploadMessage(
                UploadMessageType.complete,
                taskId,
                fileId,
                "上传完成",
                System.currentTimeMillis()
        );
    }

    /**
     * 已取消
     */
    public static UploadMessage cancelled(String taskId) {
        return new UploadMessage(
                UploadMessageType.cancelled,
                taskId,
                null,
                "上传已取消",
                System.currentTimeMillis()
        );
    }
}
