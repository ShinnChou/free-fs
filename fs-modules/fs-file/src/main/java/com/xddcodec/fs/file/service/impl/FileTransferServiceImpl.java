package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xddcodec.fs.file.cache.UploadTaskCacheManager;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.file.domain.dto.CheckUploadCmd;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.CheckUploadResultVO;
import com.xddcodec.fs.file.domain.vo.FileUploadTaskVO;
import com.xddcodec.fs.file.handler.UploadTaskExceptionHandler;
import com.xddcodec.fs.file.mapper.FileInfoMapper;
import com.xddcodec.fs.file.mapper.FileUploadTaskMapper;
import com.xddcodec.fs.file.service.FileTransferService;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.framework.common.utils.FileUtils;
import com.xddcodec.fs.fs.framework.ws.core.UploadProgressDTO;
import com.xddcodec.fs.fs.framework.ws.handler.UploadWebSocketHandler;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileUploadTaskTableDef.FILE_UPLOAD_TASK;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferServiceImpl implements FileTransferService {

    private final Converter converter;
    private final FileUploadTaskMapper fileUploadTaskMapper;
    private final FileInfoMapper fileInfoMapper;
    private final UploadWebSocketHandler wsHandler;
    private final UploadTaskCacheManager cacheManager;
    private final UploadTaskExceptionHandler exceptionHandler;
    @Qualifier("chunkUploadExecutor")
    private final ThreadPoolTaskExecutor chunkUploadExecutor;
    @Qualifier("fileMergeExecutor")
    private final ThreadPoolTaskExecutor fileMergeExecutor;
    private final StorageServiceFacade storageServiceFacade;
    @Value("${spring.application.name:free-fs}")
    private String applicationName;

    @Override
    public List<FileUploadTaskVO> getTransferFiles(TransferFilesQry qry) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.where(FILE_UPLOAD_TASK.USER_ID.eq(userId)
                .and(FILE_UPLOAD_TASK.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId)));
//        if (qry.getStatusType() != null) {
//
//        }
        queryWrapper.orderBy(FILE_UPLOAD_TASK.CREATED_AT.asc());
        List<FileUploadTask> tasks = fileUploadTaskMapper.selectListByQuery(queryWrapper);
        return converter.convert(tasks, FileUploadTaskVO.class);
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
            String taskId = IdUtil.fastSimpleUUID();
            String suffix = FileUtils.extName(cmd.getFileName());
            String tempFileName = IdUtil.fastSimpleUUID() + "." + suffix;
            String objectKey = FileUtils.generateObjectKey(applicationName, userId, tempFileName);

            // 创建上传任务
            FileUploadTask task = new FileUploadTask();
            task.setTaskId(taskId);
            task.setUserId(userId);
            task.setParentId(cmd.getParentId());
            task.setFileName(cmd.getFileName());
            task.setFileSize(cmd.getFileSize());
            task.setSuffix(FileUtils.getSuffix(cmd.getFileName()));
            task.setMimeType(cmd.getMimeType());
            task.setTotalChunks(cmd.getTotalChunks());
            task.setUploadedChunks(0);
            task.setChunkSize(cmd.getChunkSize());
            task.setObjectKey(objectKey);
            task.setStoragePlatformSettingId(storagePlatformSettingId);
            task.setStatus(UploadTaskStatus.initialized); // 初始化状态
            task.setStartTime(LocalDateTime.now());
            fileUploadTaskMapper.insert(task);
            cacheManager.cacheTask(task);
            cacheManager.recordStartTime(task.getTaskId());

            // 推送初始化成功消息
            wsHandler.pushInitialized(taskId);

            log.info("初始化上传成功: fileName={}", cmd.getFileName());
            return task.getTaskId();
        } catch (Exception e) {
            log.error("初始化上传失败: fileName={}", cmd.getFileName(), e);
            throw new StorageOperationException("初始化上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CheckUploadResultVO checkUpload(CheckUploadCmd cmd) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        String taskId = cmd.getTaskId();
        // 获取任务
        FileUploadTask task = null;
        try {
            task = getTaskFromCacheOrDB(taskId);
            if (!UploadTaskStatus.initialized.equals(task.getStatus())) {
                throw new BusinessException("任务状态不正确，当前状态: " + task.getStatus());
            }
            updateTaskStatus(task, UploadTaskStatus.checking);

            wsHandler.pushChecking(taskId);

            // 检查是否存在相同MD5的文件（秒传）
            FileInfo existFile = fileInfoMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .where(FILE_INFO.CONTENT_MD5.eq(cmd.getFileMd5()))
                            .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                            .and(FILE_INFO.USER_ID.eq(userId))
                            .and(FILE_INFO.IS_DELETED.eq(false))
            );
            if (existFile != null) {

                //TODO 应该复制文件记录
            }
            // 不是秒传，需要正常上传
            // 调用存储插件初始化分片上传
            IStorageOperationService storageService = storageServiceFacade.getStorageService(storagePlatformSettingId);
            String uploadId = storageService.initiateMultipartUpload(task.getObjectKey(), task.getMimeType());
            // 更新任务信息
            task.setFileMd5(cmd.getFileMd5());
            task.setUploadId(uploadId);

            updateTaskStatus(task, UploadTaskStatus.uploading);

            // 推送可以开始上传消息
            wsHandler.pushReadyToUpload(taskId, uploadId);
            return CheckUploadResultVO.builder()
                    .isQuickUpload(false)
                    .taskId(taskId)
                    .uploadId(uploadId)
                    .message("校验完成，可以开始上传")
                    .build();
        } catch (Exception e) {
            log.error("文件校验失败: taskId={}", taskId, e);
            exceptionHandler.handleTaskFailed(taskId, "文件校验失败: " + e.getMessage(), e);
            throw new StorageOperationException("文件校验失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传分片
     *
     * @param fileBytes 分片文件字节数组
     * @param cmd       上传分片命令
     */
    @Override
    public void uploadChunk(byte[] fileBytes, UploadChunkCmd cmd) {
        String taskId = cmd.getTaskId();
        Integer chunkIndex = cmd.getChunkIndex();
        // 异步上传分片
        CompletableFuture.runAsync(() -> {
            try {
                doUploadChunk(fileBytes, cmd);
            } catch (Exception e) {
                log.error("分片上传失败: taskId={}, chunkIndex={}", taskId, cmd.getChunkIndex(), e);
                exceptionHandler.handleChunkUploadFailed(taskId, chunkIndex, e.getMessage(), e);
            }
        }, chunkUploadExecutor);
    }

    /**
     * 上传分片
     */
    private void doUploadChunk(byte[] fileBytes, UploadChunkCmd cmd) throws IOException {
        String taskId = cmd.getTaskId();
        Integer chunkIndex = cmd.getChunkIndex();
        FileUploadTask task = getTaskFromCacheOrDB(taskId);
        if (task.getStatus() == UploadTaskStatus.paused) {
            log.info("任务已暂停，停止上传: taskId={}, chunkIndex={}", taskId, chunkIndex);
            return;
        }
        if (!UploadTaskStatus.uploading.equals(task.getStatus())) {
            throw new BusinessException("任务状态不正确: " + task.getStatus());
        }
        // 检查分片是否已存在（避免重复上传）
        if (cacheManager.isChunkUploaded(taskId, chunkIndex)) {
            log.info("分片已存在，跳过上传: taskId={}, chunkIndex={}", taskId, chunkIndex);
            return;
        }

        IStorageOperationService storageService =
                storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());

        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
            storageService.uploadPart(
                    task.getObjectKey(),
                    task.getUploadId(),
                    chunkIndex,
                    fileBytes.length,
                    bis);
        }
        cacheManager.addUploadedChunk(taskId, chunkIndex);
        cacheManager.recordUploadedBytes(taskId, fileBytes.length);

//        cacheManager.incrementUploadedChunks(taskId);           // 递增已上传分片数
//        cacheManager.addUploadedChunk(taskId, chunkIndex);       // 记录已上传分片
//        cacheManager.recordUploadedBytes(taskId, fileBytes.length); // 记录上传字节数
//        task = cacheManager.getTaskFromCache(taskId);

        // 推送进度
        UploadProgressDTO progressDTO = buildProgressDTO(task);
        wsHandler.pushProgress(taskId, progressDTO);

        log.info("分片上传成功: taskId={}, chunkIndex={}, progress={}/{}",
                taskId, chunkIndex, task.getUploadedChunks(), task.getTotalChunks());
        // 检查是否需要触发合并
        checkAndTriggerMerge(task);
    }

    /**
     * 检查并触发合并
     */
    private void checkAndTriggerMerge(FileUploadTask task) {
        String taskId = task.getTaskId();
        Integer totalChunks = task.getTotalChunks();

        if (!cacheManager.isAllChunksUploaded(taskId, totalChunks)) {
            Integer uploadedCount = cacheManager.getUploadedChunks(taskId);
            log.debug("📊 分片未全部上传: taskId={}, progress={}/{}",
                    taskId, uploadedCount, totalChunks);
            return;
        }
        RLock lock = cacheManager.getMergeLock(taskId);
        try {
            // 尝试获取锁（等待最多10秒，锁自动释放时间30秒）
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // 双重检查状态（从 Redis）
                    FileUploadTask latestTask = cacheManager.getTaskFromCache(taskId);

                    if (!UploadTaskStatus.uploading.equals(latestTask.getStatus())) {
                        log.info("任务已在合并或已完成，跳过: taskId={}, status={}",
                                taskId, latestTask.getStatus());
                        return;
                    }
                    // 新状态为 merging（Redis + 数据库）
                    cacheManager.updateTaskStatus(taskId, UploadTaskStatus.merging);

                    int updatedRows = fileUploadTaskMapper.updateStatusByTaskIdAndStatus(
                            taskId,
                            UploadTaskStatus.merging,
                            UploadTaskStatus.uploading
                    );
                    if (updatedRows == 0) {
                        log.warn("数据库状态更新失败，可能已被其他实例更新: taskId={}", taskId);
                        return;
                    }
                    log.info("开始合并文件: taskId={}", taskId);
                    // 异步合并文件
                    CompletableFuture.runAsync(() -> {
                        try {
                            doMergeChunks(latestTask.getTaskId());
                        } catch (Exception e) {
                            log.error("文件合并失败: taskId={}", taskId, e);
                            exceptionHandler.handleTaskFailed(taskId, "文件合并失败: " + e.getMessage(), e);
                        }
                    }, fileMergeExecutor);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取合并锁超时: taskId={}", taskId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取合并锁被中断: taskId={}", taskId, e);
        }
    }

    @Override
    public void pauseUpload(String taskId) {
        String userId = StpUtil.getLoginIdAsString();
        FileUploadTask task = getTaskFromCacheOrDB(taskId);
        if (!UploadTaskStatus.uploading.equals(task.getStatus())) {
            throw new StorageOperationException("当前任务状态不支持暂停操作");
        }
        // 更新数据库状态
        task.setStatus(UploadTaskStatus.paused);
        task.setUpdatedAt(LocalDateTime.now());
        fileUploadTaskMapper.update(task);
        // 更新缓存状态
        cacheManager.updateTaskStatus(taskId, UploadTaskStatus.paused);
        // 推送暂停消息
        wsHandler.pushPaused(taskId);
        log.info("暂停上传任务: taskId={}, userId={}", taskId, userId);
    }

    @Override
    public void resumeUpload(String taskId) {
        String userId = StpUtil.getLoginIdAsString();
        FileUploadTask task = getTaskFromCacheOrDB(taskId);
        if (!UploadTaskStatus.paused.equals(task.getStatus())) {
            throw new StorageOperationException("当前任务状态不支持继续操作");
        }
        task.setStatus(UploadTaskStatus.uploading);
        task.setUpdatedAt(LocalDateTime.now());
        fileUploadTaskMapper.update(task);
        cacheManager.updateTaskStatus(taskId, UploadTaskStatus.uploading);
        Set<Integer> uploadedChunks = cacheManager.getUploadedChunkList(taskId);
        wsHandler.pushResumed(taskId, uploadedChunks);
        log.info("继续上传任务: taskId={}, userId={}, uploadedChunks={}/{}",
                taskId, userId, task.getUploadedChunks(), task.getTotalChunks());
    }


    @Override
    public Set<Integer> getUploadedChunks(String taskId) {
        return cacheManager.getUploadedChunkList(taskId);
    }

    @Override
    public void cancelUpload(String taskId) {

    }

    @Override
    @Transactional
    public FileInfo mergeChunks(String taskId) {
        return doMergeChunks(taskId);
    }

    public FileInfo doMergeChunks(String taskId) {
        try {
            log.info("开始合并文件: taskId={}", taskId);
            FileUploadTask task = getByTaskId(taskId);
            if (task == null) {
                throw new StorageOperationException("上传任务不存在: " + taskId);
            }
            Integer uploadedCount = cacheManager.getUploadedChunks(taskId);
            if (!uploadedCount.equals(task.getTotalChunks())) {
                log.error("分片未全部上传，拒绝合并: taskId={}, uploaded={}, total={}",
                        taskId, uploadedCount, task.getTotalChunks());
                throw new StorageOperationException(
                        String.format("分片不完整：已上传 %d/%d", uploadedCount, task.getTotalChunks())
                );
            }
            IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());

            // 获取存储服务并完成分片合并
            List<Map<String, Object>> partETags = new ArrayList<>();
            for (int i = 0; i < task.getTotalChunks(); i++) {
                Map<String, Object> partInfo = new HashMap<>();
                partInfo.put("partNumber", i);
                partInfo.put("eTag", "");
                partETags.add(partInfo);
            }
            storageService.completeMultipartUpload(
                    task.getObjectKey(),
                    task.getUploadId(),
                    partETags
            );

            String fileId = IdUtil.fastSimpleUUID();
            String displayName = task.getObjectKey().substring(task.getObjectKey().lastIndexOf("/") + 1);

            LocalDateTime completeTime = LocalDateTime.now();
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
            fileInfo.setUploadTime(completeTime);
            fileInfo.setUpdateTime(completeTime);
            fileInfo.setIsDeleted(false);

            fileInfoMapper.insert(fileInfo);

            // 更新任务状态为已完成
            task.setStatus(UploadTaskStatus.completed);
            // 最终同步一次分片数量
            task.setUploadedChunks(uploadedCount);
            task.setCompleteTime(completeTime);
            fileUploadTaskMapper.update(task);

            cacheManager.cleanTask(taskId);

            // 推送完成消息
            wsHandler.pushComplete(taskId, fileInfo.getId());

            log.info("分片合并成功: taskId={}, fileId={}, fileName={}", taskId, fileInfo.getId(), fileInfo.getOriginalName());

            return fileInfo;

        } catch (Exception e) {
            log.error("分片合并失败: taskId={}", taskId, e);
            // 推送错误消息
            throw new StorageOperationException("分片合并失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建进度DTO
     */
    private UploadProgressDTO buildProgressDTO(FileUploadTask task) {
        String taskId = task.getTaskId();

        // 从 Redis Set 获取真实已上传数量
        Integer uploadedCount = cacheManager.getUploadedChunks(taskId);
        long uploadedBytes = cacheManager.getUploadedBytes(taskId);

        // 计算进度百分比
        double progress = task.getTotalChunks() > 0
                ? (uploadedCount * 100.0 / task.getTotalChunks())
                : 0;

        // 计算上传速度
        Long startTime = cacheManager.getStartTime(taskId);
        long speed = 0;
        int remainTime = 0;

        if (startTime != null) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds > 0) {
                speed = uploadedBytes / elapsedSeconds;

                // 计算剩余时间
                long remainingBytes = task.getFileSize() - uploadedBytes;
                if (speed > 0) {
                    remainTime = (int) (remainingBytes / speed);
                }
            }
        }

        return UploadProgressDTO.builder()
                .taskId(taskId)
                .uploadedChunks(uploadedCount)  // ⭐ 真实数量
                .totalChunks(task.getTotalChunks())
                .uploadedSize(uploadedBytes)
                .totalSize(task.getFileSize())
                .progress(Math.min(progress, 100.0))
                .speed(speed)
                .remainTime(remainTime)
                .build();
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

    private FileUploadTask getTaskFromCacheOrDB(String taskId) {
        FileUploadTask task = cacheManager.getTaskFromCache(taskId);
        if (task == null) {
            task = fileUploadTaskMapper.selectOneByQuery(
                    QueryWrapper.create().where(FileUploadTask::getTaskId).eq(taskId)
            );
            if (task == null) {
                throw new BusinessException("任务不存在: " + taskId);
            }
            // 缓存到 Redis
            cacheManager.cacheTask(task);
        }
        return task;
    }

    /**
     * 更新任务状态（数据库 + 缓存）
     */
    private void updateTaskStatus(FileUploadTask task, UploadTaskStatus newStatus) {
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        fileUploadTaskMapper.update(task);
        cacheManager.cacheTask(task);
        cacheManager.updateTaskStatus(task.getTaskId(), newStatus);
    }

}
