package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.xddcodec.fs.file.cache.UploadTaskCacheManager;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.FileUploadTaskVO;
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
        if (qry.getStatusType() != null) {

        }
        queryWrapper.orderBy(FILE_UPLOAD_TASK.UPDATED_AT.desc());
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
            // 检查秒传
            if (cmd.getFileMd5() != null) {
                FileInfo existFile = fileInfoMapper.selectOneByQuery(
                        new QueryWrapper()
                                .where(FILE_INFO.CONTENT_MD5.eq(cmd.getFileMd5())
                                        .and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId))
                                        .and(FILE_INFO.USER_ID.eq(userId))
                                        .and(FILE_INFO.IS_DELETED.eq(false))
                                )
                );
                if (existFile != null) {
                    log.info("秒传成功: userId={}, fileName={}", userId, cmd.getFileName());
                    // 秒传：直接复制一条记录
                    // TODO: 复制文件记录逻辑
                    return "QUICK_UPLOAD_" + UUID.randomUUID();
                }
            }

            // 生成objectKey
            String suffix = FileUtils.extName(cmd.getFileName());
            String tempFileName = IdUtil.fastSimpleUUID() + "." + suffix;
            String objectKey = FileUtils.generateObjectKey(applicationName, userId, tempFileName);

            // 调用存储插件初始化（同步执行，确保环境准备好）
            IStorageOperationService storageService = storageServiceFacade.getStorageService(storagePlatformSettingId);
            String uploadId = storageService.initiateMultipartUpload(objectKey, cmd.getMimeType());

            // 创建上传任务（保存到数据库）
            FileUploadTask task = createUploadTask(uploadId, userId, objectKey, cmd, storagePlatformSettingId);

            cacheManager.cacheTask(task);
            cacheManager.recordStartTime(task.getTaskId());
            cacheManager.incrementUserUploadCount(userId);
            log.info("初始化上传成功: taskId={}, fileName={}", uploadId, cmd.getFileName());
            // 返回taskId供前端使用
            return task.getTaskId();

        } catch (Exception e) {
            log.error("初始化上传失败: fileName={}", cmd.getFileName(), e);
            throw new StorageOperationException("初始化上传失败: " + e.getMessage(), e);
        }
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
        task.setSuffix(FileUtils.getSuffix(cmd.getFileName()));
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
     * 上传分片
     *
     * @param fileBytes 分片文件字节数组
     * @param cmd       上传分片命令
     */
    @Override
    public void uploadChunk(byte[] fileBytes, UploadChunkCmd cmd) {
        String taskId = cmd.getTaskId();
        // 异步上传分片
        CompletableFuture.runAsync(() -> {
            try {
                doUploadChunk(fileBytes, cmd);
            } catch (Exception e) {
                log.error("分片上传失败: taskId={}, chunkIndex={}", taskId, cmd.getChunkIndex(), e);
                wsHandler.pushError(taskId, "分片上传失败: " + e.getMessage());
            }
        }, chunkUploadExecutor);
    }

    /**
     * 上传分片
     */
    private void doUploadChunk(byte[] fileBytes, UploadChunkCmd cmd) throws IOException {
        String taskId = cmd.getTaskId();
        Integer chunkIndex = cmd.getChunkIndex();
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
                    task.getTaskId(),
                    chunkIndex,
                    fileBytes.length,
                    bis
            );
        }
        fileUploadTaskMapper.incrementUploadedChunks(taskId);

        cacheManager.incrementUploadedChunks(taskId);           // 递增已上传分片数
        cacheManager.addUploadedChunk(taskId, chunkIndex);       // 记录已上传分片
        cacheManager.recordUploadedBytes(taskId, fileBytes.length); // 记录上传字节数
        task = cacheManager.getTaskFromCache(taskId);

        // 推送进度（WebSocket）
        UploadProgressDTO progress = buildProgressDTO(task);
        wsHandler.pushProgress(taskId, progress);

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
        // 检查是否所有分片都已上传完成
        if (!task.getUploadedChunks().equals(task.getTotalChunks())) {
            log.debug("分片未全部上传: taskId={}, progress={}/{}",
                    taskId, task.getUploadedChunks(), task.getTotalChunks());
            return;
        }
        RLock lock = cacheManager.getMergeLock(taskId);
        try {
            // 尝试获取锁（等待最多10秒，锁自动释放时间30秒）
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // 双重检查状态（从 Redis）
                    FileUploadTask latestTask = cacheManager.getTaskFromCache(taskId);

                    if (latestTask.getStatus() != UploadTaskStatus.uploading) {
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
                            wsHandler.pushError(taskId, "文件合并失败: " + e.getMessage());
                            handleMergeFailed(taskId, e.getMessage());
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
    public Set<Integer> getUploadedChunks(String taskId) {
        return cacheManager.getUploadedChunkList(taskId);
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
                    task.getTaskId(),
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
            task.setCompleteTime(completeTime);
            fileUploadTaskMapper.update(task);


            cacheManager.updateTaskCompleteTime(taskId, completeTime);

            // 推送完成消息
            wsHandler.pushComplete(taskId, fileInfo.getId());

            // 递减用户上传文件数
            cacheManager.decrementUserUploadCount(task.getUserId());
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
        // 计算进度百分比
        double progress = task.getTotalChunks() > 0
                ? (task.getUploadedChunks() * 100.0 / task.getTotalChunks())
                : 0;
        // 从 Redis 获取开始时间和已上传字节数
        Long startTime = cacheManager.getStartTime(taskId);
        long uploadedBytes = cacheManager.getUploadedBytes(taskId);
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
                .uploadedChunks(task.getUploadedChunks())
                .totalChunks(task.getTotalChunks())
                .uploadedSize(uploadedBytes)
                .totalSize(task.getFileSize())
                .progress(Math.min(progress, 100.0))
                .speed(speed)
                .remainTime(remainTime)
                .build();
    }

    /**
     * 处理合并失败
     */
    private void handleMergeFailed(String taskId, String errorMsg) {
        FileUploadTask task = fileUploadTaskMapper.selectOneByQuery(
                QueryWrapper.create().where(FileUploadTask::getTaskId).eq(taskId)
        );
        if (task != null) {
            task.setStatus(UploadTaskStatus.failed);
            task.setErrorMsg(errorMsg);
            fileUploadTaskMapper.update(task);
            cacheManager.updateTaskStatus(taskId, UploadTaskStatus.failed);
            cacheManager.decrementUserUploadCount(task.getUserId());
            cacheManager.extendTaskExpire(taskId, 1);
        }
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
}
