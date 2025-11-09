package com.xddcodec.fs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 上传任务 mapper 接口
 *
 * @Author: xddcode
 * @Date: 2025/11/06 15:22
 */
public interface FileUploadTaskMapper extends BaseMapper<FileUploadTask> {

    /**
     * 原子递增已上传分片数
     * 避免并发更新时的计数丢失问题
     *
     * @param taskId 任务ID
     * @return 受影响的行数
     */
    @Update("UPDATE file_upload_task SET uploaded_chunks = uploaded_chunks + 1, updated_at = NOW() WHERE task_id = #{taskId}")
    int incrementUploadedChunks(@Param("taskId") String taskId);

    /**
     * 原子更新状态（仅当当前状态为指定状态时才更新）
     * 用于防止重复合并
     */
    @Update("UPDATE file_upload_task " +
            "SET status = #{newStatus}, updated_at = NOW() " +
            "WHERE task_id = #{taskId} AND status = #{currentStatus}")
    int updateStatusByTaskIdAndStatus(
            @Param("taskId") String taskId,
            @Param("newStatus") UploadTaskStatus newStatus,
            @Param("currentStatus") UploadTaskStatus currentStatus
    );
}
