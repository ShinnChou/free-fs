package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileUploadChunk;
import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.FileUploadTaskVO;
import com.xddcodec.fs.file.mapper.FileInfoMapper;
import com.xddcodec.fs.file.mapper.FileUploadChunkMapper;
import com.xddcodec.fs.file.mapper.FileUploadTaskMapper;
import com.xddcodec.fs.file.service.FileTransferService;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.fs.framework.ws.handler.UploadWebSocketHandler;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileUploadChunkTableDef.FILE_UPLOAD_CHUNK;
import static com.xddcodec.fs.file.domain.table.FileUploadTaskTableDef.FILE_UPLOAD_TASK;

@Slf4j
@Service
public class FileTransferServiceImpl implements FileTransferService {
    @Autowired
    private Converter converter;
    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private FileUploadTaskMapper fileUploadTaskMapper;
    @Autowired
    private FileUploadChunkMapper fileUploadChunkMapper;
    @Autowired
    private StorageServiceFacade storageServiceFacade;
    @Autowired
    private UploadWebSocketHandler uploadWebSocketHandler;
    @Autowired
    @Qualifier("uploadTaskExecutor")
    private ThreadPoolTaskExecutor uploadTaskExecutor;
    @Value("${spring.application.name:free-fs}")
    private String applicationName;

    @Override
    public List<FileUploadTaskVO> getTransferFiles(TransferFilesQry qry) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.where(FILE_UPLOAD_TASK.USER_ID.eq(userId)
                .and(FILE_UPLOAD_TASK.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId)));
        if (qry.getStatusType() != null) {

        }
        queryWrapper.orderBy(FILE_UPLOAD_TASK.UPDATED_AT.desc());
        List<FileUploadTask> tasks = fileUploadTaskMapper.selectListByQuery(queryWrapper);
        return converter.convert(tasks, FileUploadTaskVO.class);
    }

    /**
     * 检查秒传
     *
     * @param md5
     * @param storagePlatformSettingId
     * @param userId
     * @param originalName
     * @return
     */
    private FileInfo checkSecondUpload(String md5, String storagePlatformSettingId, String userId, String originalName) {
        if (StrUtil.isBlank(md5) || StrUtil.isBlank(storagePlatformSettingId)) {
            return null;
        }
        return fileInfoMapper.selectOneByQuery(
                new QueryWrapper()
                        .where(FILE_INFO.CONTENT_MD5.eq(md5)
                                .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                                .and(FILE_INFO.USER_ID.eq(userId))
                                .and(FILE_INFO.ORIGINAL_NAME.eq(originalName))
                                .and(FILE_INFO.IS_DELETED.eq(false))
                        )
        );
    }

    /**
     * 创建上传任务
     *
     * @param taskId                   任务ID（等于uploadId）
     * @param userId                   用户ID
     * @param objectKey                对象key
     * @param cmd                      上传初始化信息
     * @param storagePlatformSettingId 存储平台配置ID
     */
    private FileUploadTask createUploadTask(String taskId, String userId, String objectKey, InitUploadCmd cmd, String storagePlatformSettingId) {
        FileUploadTask task = new FileUploadTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setParentId(cmd.getParentId());
        task.setFileName(cmd.getFileName());
        task.setFileSize(cmd.getFileSize());
        task.setFileMd5(cmd.getFileMd5());
        task.setSuffix(FileUtil.getSuffix(cmd.getFileName()));
        task.setMimeType(cmd.getMimeType());
        task.setTotalChunks(cmd.getTotalChunks());
        task.setUploadedChunks(0);
        task.setChunkSize(cmd.getChunkSize());
        task.setObjectKey(objectKey);
        task.setStoragePlatformSettingId(storagePlatformSettingId);
        task.setParentId(cmd.getParentId());
        task.setStatus(UploadTaskStatus.uploading);
        task.setStartTime(LocalDateTime.now());
        fileUploadTaskMapper.insert(task);
        return task;
    }

    /**
     * 生成对象键
     * <p>
     * 格式: {projectName}/{userId}/{yyyyMMdd}/{fileId}.{suffix}
     * 示例: free-fs/user001/20241226/abc123.pdf
     *
     * @param userId     用户ID
     * @param objectName 文件名
     * @return 对象键
     */
    private String generateObjectKey(String userId, String objectName) {
        StringBuilder objectKey = new StringBuilder();

        objectKey.append(applicationName).append("/");

        if (StrUtil.isNotBlank(userId)) {
            objectKey.append(userId).append("/");
        } else {
            objectKey.append("anonymous/");  // 匿名用户
        }

        String dateDir = DateUtil.format(new java.util.Date(), "yyyyMMdd");
        objectKey.append(dateDir).append("/");

        objectKey.append(objectName);
        return objectKey.toString();
    }

    /**
     * 推送上传进度（使用精确统计）
     */
    private void pushProgress(FileUploadTask task) {
        try {
            // 从数据库精确统计已上传的分片
            List<FileUploadChunk> completedChunks = fileUploadChunkMapper.selectListByQuery(
                    new QueryWrapper()
                            .where(FILE_UPLOAD_CHUNK.TASK_ID.eq(task.getTaskId())
                                    .and(FILE_UPLOAD_CHUNK.STATUS.eq(UploadTaskStatus.completed)))
            );

            // 精确计算已上传数量和大小
            int actualUploadedChunks = completedChunks.size();
            long uploadedSize = completedChunks.stream()
                    .mapToLong(FileUploadChunk::getChunkSize)
                    .sum();

            com.xddcodec.fs.fs.framework.ws.core.UploadProgressDTO progress =
                    new com.xddcodec.fs.fs.framework.ws.core.UploadProgressDTO();
            progress.setUploadedChunks(actualUploadedChunks);
            progress.setTotalChunks(task.getTotalChunks());
            progress.setUploadedSize(uploadedSize);
            progress.setTotalSize(task.getFileSize());

            // 计算进度百分比（防止超过100%）
            double progressPercentage = task.getTotalChunks() > 0
                    ? (double) actualUploadedChunks / task.getTotalChunks() * 100
                    : 0;
            progress.setProgress(Math.min(progressPercentage, 100.0));

            // 计算上传速度和剩余时间
            if (task.getStartTime() != null) {
                long elapsedSeconds = java.time.Duration.between(
                        task.getStartTime(), LocalDateTime.now()).getSeconds();
                if (elapsedSeconds > 0) {
                    long speed = uploadedSize / elapsedSeconds; // 字节/秒
                    progress.setSpeed(speed);

                    long remainingBytes = task.getFileSize() - uploadedSize;
                    long remainingSeconds = speed > 0 ? remainingBytes / speed : 0;
                    progress.setRemainTime(remainingSeconds);
                }
            }

            // 推送进度
            uploadWebSocketHandler.pushProgress(task.getTaskId(), progress);

        } catch (Exception e) {
            // 进度推送失败不影响主流程
            log.warn("推送上传进度失败: taskId={}", task.getTaskId(), e);
        }
    }

    @Override
    public Set<Integer> getUploadedChunks(String taskId) {
        List<FileUploadChunk> chunks = fileUploadChunkMapper.selectListByQuery(
                new QueryWrapper()
                        .where(FILE_UPLOAD_CHUNK.TASK_ID.eq(taskId)
                                .and(FILE_UPLOAD_CHUNK.STATUS.eq(UploadTaskStatus.completed))
                        )
        );
        return chunks.stream()
                .map(FileUploadChunk::getChunkIndex)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo mergeChunks(String taskId) {
        try {
            // 查询上传任务
            FileUploadTask task = getByTaskId(taskId);
            if (task == null) {
                throw new StorageOperationException("上传任务不存在: " + taskId);
            }

            // 查询所有已上传的分片（按索引排序）
            List<FileUploadChunk> chunks = fileUploadChunkMapper.selectListByQuery(
                    new QueryWrapper()
                            .where(FILE_UPLOAD_CHUNK.TASK_ID.eq(taskId)
                                    .and(FILE_UPLOAD_CHUNK.STATUS.eq(UploadTaskStatus.completed)))
                            .orderBy(FILE_UPLOAD_CHUNK.CHUNK_INDEX.asc())
            );

            // 验证分片完整性
            if (chunks.size() != task.getTotalChunks()) {
                throw new StorageOperationException(
                        String.format("分片不完整：期望%d个，实际%d个", task.getTotalChunks(), chunks.size())
                );
            }

            // 构建partETags列表（用于云存储合并）
            List<Map<String, Object>> partETags = new ArrayList<>();
            for (FileUploadChunk chunk : chunks) {
                Map<String, Object> partInfo = new HashMap<>();
                partInfo.put("partNumber", chunk.getChunkIndex() + 1); // partNumber从1开始
                partInfo.put("eTag", chunk.getEtag());
                partETags.add(partInfo);
            }

            String fileId = IdUtil.fastSimpleUUID();
            String displayName = task.getObjectKey().substring(task.getObjectKey().lastIndexOf("/") + 1);

            // 获取存储服务并完成分片合并
            IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());
            storageService.completeMultipartUpload(
                    task.getObjectKey(),
                    task.getTaskId(),
                    partETags
            );

            // 创建FileInfo记录
            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(fileId);
            fileInfo.setObjectKey(task.getObjectKey());
            fileInfo.setOriginalName(task.getFileName());
            fileInfo.setDisplayName(displayName);
            fileInfo.setSuffix(task.getSuffix());
            fileInfo.setSize(task.getFileSize());
            fileInfo.setMimeType(task.getMimeType());
            fileInfo.setIsDir(false);
            fileInfo.setParentId(task.getParentId());
            fileInfo.setUserId(task.getUserId());
            fileInfo.setContentMd5(task.getFileMd5());
            fileInfo.setStoragePlatformSettingId(task.getStoragePlatformSettingId());
            fileInfo.setUploadTime(LocalDateTime.now());
            fileInfo.setUpdateTime(LocalDateTime.now());
            fileInfo.setIsDeleted(false);

            // 保存文件信息
            fileInfoMapper.insert(fileInfo);

            // 更新任务状态为已完成
            task.setStatus(UploadTaskStatus.completed);
            task.setCompleteTime(LocalDateTime.now());
            fileUploadTaskMapper.update(task);

            // 推送完成消息
            pushComplete(task.getTaskId(), fileInfo);

            log.info("分片合并成功: taskId={}, fileId={}, fileName={}", taskId, fileInfo.getId(), fileInfo.getOriginalName());

            return fileInfo;

        } catch (Exception e) {
            log.error("分片合并失败: taskId={}", taskId, e);
            // 推送错误消息
            pushError(taskId, "分片合并失败: " + e.getMessage());
            throw new StorageOperationException("分片合并失败: " + e.getMessage(), e);
        }
    }

    /**
     * 推送完成消息
     */
    private void pushComplete(String taskId, FileInfo fileInfo) {
        try {
            uploadWebSocketHandler.pushComplete(taskId, fileInfo.getId());
        } catch (Exception e) {
            // 完成消息推送失败不影响主流程
            log.warn("推送完成消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 推送错误消息
     */
    private void pushError(String taskId, String errorMessage) {
        try {
            uploadWebSocketHandler.pushError(taskId, errorMessage);
        } catch (Exception e) {
            // 错误消息推送失败不影响主流程
            log.warn("推送错误消息失败: taskId={}", taskId, e);
        }
    }

    /**
     * 自动合并分片（异步执行）
     */
    private void autoMergeChunks(String taskId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始自动合并分片: taskId={}", taskId);
                FileInfo fileInfo = mergeChunks(taskId);
                log.info("自动合并分片成功: taskId={}, fileId={}", taskId, fileInfo.getId());
                // 推送合并完成消息
                pushComplete(taskId, fileInfo);
            } catch (Exception e) {
                log.error("自动合并分片失败: taskId={}", taskId, e);
                pushError(taskId, "合并分片失败: " + e.getMessage());
            }
        }, uploadTaskExecutor);
    }

    /**
     * 根据任务ID获取任务信息
     *
     * @param taskId
     * @return
     */
    private FileUploadTask getByTaskId(String taskId) {
        return fileUploadTaskMapper.selectOneByQuery(
                new QueryWrapper().where(FILE_UPLOAD_TASK.TASK_ID.eq(taskId)
                )
        );
    }

    /**
     * 初始化上传
     *
     * @param cmd 初始化上传命令
     * @return 任务ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initUpload(InitUploadCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();

        try {
            // 检查秒传
            FileInfo existFile = checkSecondUpload(cmd.getFileMd5(), storagePlatformSettingId, userId, cmd.getFileName());
            if (existFile != null) {
                log.info("秒传成功: userId={}, fileName={}", userId, cmd.getFileName());
                // 秒传返回文件ID（前端可以根据返回值判断是否秒传）
                return "INSTANT:" + existFile.getId();
            }

            // 生成objectKey
            String suffix = FileUtil.extName(cmd.getFileName());
            String tempFileName = IdUtil.fastSimpleUUID() + "." + suffix;
            String objectKey = generateObjectKey(userId, tempFileName);

            // 调用存储插件初始化（同步执行，确保环境准备好）
            IStorageOperationService storageService = storageServiceFacade.getStorageService(storagePlatformSettingId);
            String uploadId = storageService.initiateMultipartUpload(objectKey, cmd.getMimeType());

            // 创建上传任务（保存到数据库）
            createUploadTask(uploadId, userId, objectKey, cmd, storagePlatformSettingId);

            log.info("初始化上传成功: taskId={}, fileName={}", uploadId, cmd.getFileName());

            // 返回taskId供前端使用
            return uploadId;

        } catch (Exception e) {
            log.error("初始化上传失败: fileName={}", cmd.getFileName(), e);
            throw new StorageOperationException("初始化上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步上传分片
     *
     * @param fileBytes 分片文件字节数组
     * @param cmd       上传分片命令
     */
    @Override
    @Async("uploadTaskExecutor")
    public void uploadChunkAsync(byte[] fileBytes, UploadChunkCmd cmd) {
        try {

            // 验证分片MD5
            String actualMd5 = DigestUtils.md5DigestAsHex(fileBytes);
            if (!actualMd5.equals(cmd.getChunkMd5())) {
                uploadWebSocketHandler.pushError(cmd.getTaskId(), "分片MD5校验失败");
                return;
            }

            // 查询上传任务
            FileUploadTask task = getByTaskId(cmd.getTaskId());
            if (task == null) {
                uploadWebSocketHandler.pushError(cmd.getTaskId(), "上传任务不存在");
                return;
            }

            // 检查分片是否已存在
            FileUploadChunk existChunk = fileUploadChunkMapper.selectOneByQuery(
                    new QueryWrapper()
                            .where(FILE_UPLOAD_CHUNK.TASK_ID.eq(cmd.getTaskId())
                                    .and(FILE_UPLOAD_CHUNK.CHUNK_INDEX.eq(cmd.getChunkIndex())))
            );

            if (existChunk != null && UploadTaskStatus.completed.equals(existChunk.getStatus())) {
                log.info("分片已存在，跳过上传，推送当前进度: taskId={}, chunkIndex={}", cmd.getTaskId(), cmd.getChunkIndex());
                // 推送当前进度
                FileUploadTask latestTask = getByTaskId(cmd.getTaskId());
                if (latestTask != null) {
                    pushProgress(latestTask);
                }
                return;
            }

            // 获取存储服务
            IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());

            // 上传分片
            String etag = storageService.uploadPart(
                    task.getObjectKey(),
                    task.getTaskId(),
                    cmd.getChunkIndex() + 1,
                    (long) fileBytes.length,
                    new ByteArrayInputStream(fileBytes)
            );

            // 保存分片记录
            if (existChunk == null) {
                FileUploadChunk chunk = new FileUploadChunk();
                chunk.setTaskId(cmd.getTaskId());
                chunk.setChunkIndex(cmd.getChunkIndex());
                chunk.setChunkMd5(cmd.getChunkMd5());
                chunk.setChunkSize((long) fileBytes.length);
                chunk.setEtag(etag);
                chunk.setStatus(UploadTaskStatus.completed);
                chunk.setUploadTime(LocalDateTime.now());
                fileUploadChunkMapper.insert(chunk);
            } else {
                existChunk.setEtag(etag);
                existChunk.setChunkSize((long) fileBytes.length);
                existChunk.setStatus(UploadTaskStatus.completed);
                existChunk.setUploadTime(LocalDateTime.now());
                fileUploadChunkMapper.update(existChunk);
            }

            // 使用原子递增更新计数
            fileUploadTaskMapper.incrementUploadedChunks(cmd.getTaskId());

            // 推送进度
            FileUploadTask latestTask = getByTaskId(cmd.getTaskId());
            if (latestTask != null) {
                pushProgress(latestTask);

                // 检查是否所有分片都已上传完成
                if (latestTask.getUploadedChunks() >= latestTask.getTotalChunks()) {
                    log.info("所有分片上传完成，服务端自动开始合并: taskId={}, totalChunks={}",
                            cmd.getTaskId(), latestTask.getTotalChunks());
                    // 服务端自动合并分片
                    autoMergeChunks(cmd.getTaskId());
                }
            }

            log.info("分片上传成功: taskId={}, chunkIndex={}", cmd.getTaskId(), cmd.getChunkIndex());

        } catch (Exception e) {
            log.error("分片上传失败: taskId={}, chunkIndex={}", cmd.getTaskId(), cmd.getChunkIndex(), e);
            uploadWebSocketHandler.pushError(cmd.getTaskId(), "分片上传失败: " + e.getMessage());
        }
    }
}
