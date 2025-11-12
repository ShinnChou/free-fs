package com.xddcodec.fs.file.service;

import com.mybatisflex.core.service.IService;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileTransferTask;
import com.xddcodec.fs.file.domain.dto.CheckUploadCmd;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.CheckUploadResultVO;
import com.xddcodec.fs.file.domain.vo.FileTransferTaskVO;

import java.util.List;
import java.util.Set;

public interface FileTransferTaskService extends IService<FileTransferTask> {

    /**
     * 获取用户传输列表
     *
     * @return
     */
    List<FileTransferTaskVO> getTransferFiles(TransferFilesQry qry);

    /**
     * 初始化上传
     *
     * @param cmd 初始化上传命令
     * @return 任务ID
     */
    String initUpload(InitUploadCmd cmd);

    /**
     * 资源校验
     *
     * @param cmd
     */
    CheckUploadResultVO checkUpload(CheckUploadCmd cmd);

    /**
     * 上传分片
     *
     * @param fileBytes 分片文件字节数组
     * @param cmd       上传分片命令
     */
    void uploadChunk(byte[] fileBytes, UploadChunkCmd cmd);

    /**
     * 暂停传输
     *
     * @param taskId 任务ID
     */
    void pauseTransfer(String taskId);

    /**
     * 继续传输
     *
     * @param taskId 任务ID
     */
    void resumeTransfer(String taskId);

    /**
     * 合并分片
     *
     * @param taskId 合并分片任务ID
     * @return 文件信息
     */
    FileInfo mergeChunks(String taskId);

    /**
     * 获取该任务下已上传的分片
     *
     * @param taskId 上传任务ID
     * @return 已上传的分片索引集合
     */
    Set<Integer> getUploadedChunks(String taskId);

    /**
     * 取消传输
     *
     * @param taskId 任务ID
     */
    void cancelTransfer(String taskId);

    /**
     * 清空已完成传输列表
     */
    void clearTransfers();
}
