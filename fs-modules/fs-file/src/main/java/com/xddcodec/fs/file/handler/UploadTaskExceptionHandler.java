package com.xddcodec.fs.file.handler;

import com.xddcodec.fs.file.cache.UploadTaskCacheManager;
import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.file.mapper.FileUploadTaskMapper;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import com.xddcodec.fs.fs.framework.ws.handler.UploadWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 上传任务异常处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTaskExceptionHandler {

    private final FileUploadTaskMapper fileUploadTaskMapper;
    private final UploadTaskCacheManager cacheManager;
    private final UploadWebSocketHandler wsHandler;

    /**
     * 处理任务失败
     *
     * @param taskId   任务ID
     * @param errorMsg 错误信息
     * @param e        异常对象
     */
    public void handleTaskFailed(String taskId, String errorMsg, Exception e) {
        try {
            log.error("任务失败: taskId={}, error={}", taskId, errorMsg, e);

            // 更新数据库状态
            FileUploadTask task = fileUploadTaskMapper.selectOneByQuery(
                    com.mybatisflex.core.query.QueryWrapper.create()
                            .where(FileUploadTask::getTaskId).eq(taskId)
            );

            if (task != null) {
                task.setStatus(UploadTaskStatus.failed);
                task.setErrorMsg(truncateErrorMsg(errorMsg));
                task.setUpdatedAt(LocalDateTime.now());
                fileUploadTaskMapper.update(task);
            }

            // 更新缓存状态
            cacheManager.updateTaskStatus(taskId, UploadTaskStatus.failed);

            // 延长缓存过期时间（保留1小时供查询）
            cacheManager.extendTaskExpire(taskId, 1);

            // 推送失败消息
            wsHandler.pushError(taskId, errorMsg);

        } catch (Exception ex) {
            log.error("处理任务失败时发生异常: taskId={}", taskId, ex);
        }
    }

    /**
     * 处理分片上传失败（不改变任务状态，只记录错误）
     *
     * @param taskId     任务ID
     * @param chunkIndex 分片索引
     * @param errorMsg   错误信息
     * @param e          异常对象
     */
    public void handleChunkUploadFailed(String taskId, Integer chunkIndex, String errorMsg, Exception e) {
        log.error("分片上传失败: taskId={}, chunkIndex={}, error={}", taskId, chunkIndex, errorMsg, e);

        // 推送错误消息（不改变任务状态，允许重试）
        wsHandler.pushError(taskId, String.format("分片 %d 上传失败: %s", chunkIndex, errorMsg));
    }

    /**
     * 截断错误信息（防止过长）
     */
    private String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) {
            return "未知错误";
        }
        return errorMsg.length() > 500 ? errorMsg.substring(0, 500) + "..." : errorMsg;
    }
}
