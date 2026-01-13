package com.xddcodec.fs.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xddcodec.fs.file.cache.TransferTaskCacheManager;
import com.xddcodec.fs.file.domain.FileInfo;
import com.xddcodec.fs.file.domain.FileTransferTask;
import com.xddcodec.fs.file.domain.dto.CheckUploadCmd;
import com.xddcodec.fs.file.domain.dto.InitUploadCmd;
import com.xddcodec.fs.file.domain.dto.UploadChunkCmd;
import com.xddcodec.fs.file.domain.qry.TransferFilesQry;
import com.xddcodec.fs.file.domain.vo.CheckUploadResultVO;
import com.xddcodec.fs.file.domain.vo.FileDownloadVO;
import com.xddcodec.fs.file.domain.vo.FileTransferTaskVO;
import com.xddcodec.fs.file.enums.TransferTaskType;
import com.xddcodec.fs.file.handler.UploadTaskExceptionHandler;
import com.xddcodec.fs.file.mapper.FileTransferTaskMapper;
import com.xddcodec.fs.file.service.FileInfoService;
import com.xddcodec.fs.file.service.FileTransferTaskService;
import com.xddcodec.fs.file.enums.TransferTaskStatus;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.framework.common.utils.FileUtils;
import com.xddcodec.fs.framework.common.utils.StringUtils;
import com.xddcodec.fs.file.service.TransferSseService;
import com.xddcodec.fs.storage.facade.StorageServiceFacade;
import com.xddcodec.fs.storage.plugin.core.IStorageOperationService;
import com.xddcodec.fs.storage.plugin.core.context.StoragePlatformContextHolder;
import com.xddcodec.fs.system.service.SysUserTransferSettingService;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.xddcodec.fs.file.domain.table.FileInfoTableDef.FILE_INFO;
import static com.xddcodec.fs.file.domain.table.FileTransferTaskTableDef.FILE_TRANSFER_TASK;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferTaskServiceImpl extends ServiceImpl<FileTransferTaskMapper, FileTransferTask> implements FileTransferTaskService {

    private final Converter converter;
    private final FileInfoService fileInfoService;
    private final TransferSseService transferSseService;
    private final TransferTaskCacheManager cacheManager;
    private final UploadTaskExceptionHandler exceptionHandler;
    @Qualifier("chunkUploadExecutor")
    private final ThreadPoolTaskExecutor chunkUploadExecutor;
    @Qualifier("fileMergeExecutor")
    private final ThreadPoolTaskExecutor fileMergeExecutor;
    private final StorageServiceFacade storageServiceFacade;
    private final SysUserTransferSettingService userTransferSettingService;
    @Value("${spring.application.name:free-fs}")
    private String applicationName;

    @Override
    public List<FileTransferTaskVO> getTransferFiles(TransferFilesQry qry) {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.where(FILE_TRANSFER_TASK.USER_ID.eq(userId)
                .and(FILE_TRANSFER_TASK.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId)));
//        if (qry.getStatusType() != null) {
//
//        }
        queryWrapper.orderBy(FILE_TRANSFER_TASK.CREATED_AT.asc());
        List<FileTransferTask> tasks = this.list(queryWrapper);
        List<FileTransferTaskVO> voList = converter.convert(tasks, FileTransferTaskVO.class);
        
        // 计算并填充进度相关字段
        for (FileTransferTaskVO vo : voList) {
            calculateProgressFields(vo);
        }
        
        return voList;
    }
    
    /**
     * 计算并填充进度相关字段
     */
    private void calculateProgressFields(FileTransferTaskVO vo) {
        String taskId = vo.getTaskId();
        
        // 获取已上传字节数
        long uploadedBytes = cacheManager.getTransferredBytes(taskId);
        vo.setUploadedSize(uploadedBytes);
        
        // 计算进度百分比（整数，0-100）
        if (vo.getFileSize() != null && vo.getFileSize() > 0) {
            double progressPercent = (uploadedBytes * 100.0) / vo.getFileSize();
            // 四舍五入取整
            int progressInt = (int) Math.round(Math.min(progressPercent, 100.0));
            vo.setProgress(progressInt);
        } else {
            vo.setProgress(0);
        }
        
        // 计算速度和剩余时间（仅对进行中的任务）
        if (vo.getStatus() != null && 
            (vo.getStatus().name().equals("uploading") || vo.getStatus().name().equals("downloading"))) {
            
            Long startTime = cacheManager.getStartTime(taskId);
            if (startTime != null && uploadedBytes > 0) {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                
                if (elapsedSeconds > 0) {
                    // 计算平均速度 (bytes/s)
                    long speed = uploadedBytes / elapsedSeconds;
                    vo.setSpeed(speed);
                    
                    // 计算剩余时间（秒）
                    if (speed > 0 && vo.getFileSize() != null) {
                        long remainingBytes = vo.getFileSize() - uploadedBytes;
                        int remainTime = (int) (remainingBytes / speed);
                        vo.setRemainTime(remainTime);
                    } else {
                        vo.setRemainTime(null);
                    }
                } else {
                    vo.setSpeed(0L);
                    vo.setRemainTime(null);
                }
            } else {
                vo.setSpeed(0L);
                vo.setRemainTime(null);
            }
        } else {
            // 非进行中的任务不显示速度和剩余时间
            vo.setSpeed(null);
            vo.setRemainTime(null);
        }
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
            //如果有同名文件则需要重新生成
            String displayName = fileInfoService.generateUniqueName(
                    userId,
                    cmd.getParentId(),
                    cmd.getFileName(),
                    false,
                    null,
                    storagePlatformSettingId
            );
            // 创建上传任务
            FileTransferTask task = new FileTransferTask();
            task.setTaskId(taskId);
            task.setUserId(userId);
            task.setParentId(cmd.getParentId());
            task.setFileName(displayName);
            task.setFileSize(cmd.getFileSize());
            task.setSuffix(FileUtils.getSuffix(cmd.getFileName()));
            task.setMimeType(cmd.getMimeType());
            task.setTotalChunks(cmd.getTotalChunks());
            task.setUploadedChunks(0);
            task.setTaskType(TransferTaskType.upload);
            task.setChunkSize(cmd.getChunkSize());
            task.setObjectKey(objectKey);
            task.setStoragePlatformSettingId(storagePlatformSettingId);
            task.setStatus(TransferTaskStatus.initialized); // 初始化状态
            task.setStartTime(LocalDateTime.now());
            this.save(task);
            cacheManager.cacheTask(task);
            cacheManager.recordStartTime(task.getTaskId());

            // 推送初始化成功状态事件
            transferSseService.sendStatusEvent(userId, taskId, 
                TransferTaskStatus.initialized.name(), "任务初始化成功");

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
        FileTransferTask task = null;
        try {
            task = getTaskFromCacheOrDB(taskId);
            if (!TransferTaskStatus.initialized.equals(task.getStatus())) {
                throw new BusinessException("任务状态不正确，当前状态: " + task.getStatus());
            }
            updateTaskStatus(task, TransferTaskStatus.checking);

            transferSseService.sendStatusEvent(userId, taskId, 
                TransferTaskStatus.checking.name(), "正在校验文件");

            // 检查同存储平台是否存在相同MD5的文件（秒传）
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.where(FILE_INFO.CONTENT_MD5.eq(cmd.getFileMd5())
                    .and(FILE_INFO.USER_ID.eq(userId))
                    .and(FILE_INFO.IS_DELETED.eq(false)));
            if (StringUtils.isEmpty(storagePlatformSettingId)) {
                queryWrapper.and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.isNull());
            } else {
                queryWrapper.and(FILE_INFO.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId));
            }
            FileInfo existFile = fileInfoService.getOne(queryWrapper);
            if (existFile != null) {
                // 验证存储插件中文件是否真实存在
                IStorageOperationService storageService =
                        storageServiceFacade.getStorageService(storagePlatformSettingId);
                if (storageService.isFileExist(existFile.getObjectKey())) {
                    // 执行秒传：直接创建文件记录
                    return handleQuickUpload(task, existFile, cmd.getFileMd5(), storagePlatformSettingId);
                } else {
                    // 清理无效的数据库记录
                    fileInfoService.removeById(existFile.getId());
                }
            }
            // 不是秒传，需要正常上传
            // 调用存储插件初始化分片上传
            IStorageOperationService storageService = storageServiceFacade.getStorageService(storagePlatformSettingId);
            String uploadId = storageService.initiateMultipartUpload(task.getObjectKey(), task.getMimeType());
            // 更新任务信息
            task.setFileMd5(cmd.getFileMd5());
            task.setUploadId(uploadId);

            updateTaskStatus(task, TransferTaskStatus.uploading);

            // 推送可以开始上传状态事件
            transferSseService.sendStatusEvent(userId, taskId, 
                TransferTaskStatus.uploading.name(), "校验完成，可以开始上传");
            
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
     * 处理秒传
     */
    protected CheckUploadResultVO handleQuickUpload(FileTransferTask task,
                                                    FileInfo existFile,
                                                    String fileMd5, String storagePlatformSettingId) {
        String taskId = task.getTaskId();

        try {
            // 创建新的文件记录（引用相同的 objectKey）
            String fileId = IdUtil.fastSimpleUUID();
            LocalDateTime now = LocalDateTime.now();
            String displayName = fileInfoService.generateUniqueName(
                    task.getUserId(),
                    task.getParentId(),
                    task.getFileName(),
                    false,
                    null,
                    storagePlatformSettingId
            );
            FileInfo newFileInfo = new FileInfo();
            newFileInfo.setId(fileId);
            // 复用已存在文件的 objectKey
            newFileInfo.setObjectKey(existFile.getObjectKey());
            newFileInfo.setOriginalName(task.getFileName());
            newFileInfo.setDisplayName(displayName);
            newFileInfo.setSuffix(task.getSuffix());
            newFileInfo.setSize(task.getFileSize());
            newFileInfo.setMimeType(task.getMimeType());
            newFileInfo.setIsDir(false);
            newFileInfo.setParentId(task.getParentId());
            newFileInfo.setUserId(task.getUserId());
            newFileInfo.setContentMd5(fileMd5);
            newFileInfo.setStoragePlatformSettingId(task.getStoragePlatformSettingId());
            newFileInfo.setUploadTime(now);
            newFileInfo.setUpdateTime(now);
            newFileInfo.setIsDeleted(false);

            fileInfoService.save(newFileInfo);

            // 更新任务状态为已完成
            task.setFileMd5(fileMd5);
            task.setUploadedChunks(task.getTotalChunks()); // 标记为全部完成
            task.setStatus(TransferTaskStatus.completed);
            task.setCompleteTime(now);
            this.updateById(task);

            // 清理缓存
            cacheManager.cleanTask(taskId);

            // 推送完成事件
            transferSseService.sendCompleteEvent(task.getUserId(), taskId, fileId, 
                displayName, task.getFileSize());

            log.info("秒传成功: taskId={}, newFileId={}, refObjectKey={}",
                    taskId, fileId, existFile.getObjectKey());

            return CheckUploadResultVO.builder()
                    .isQuickUpload(true)
                    .taskId(taskId)
                    .fileId(fileId)
                    .message("秒传成功")
                    .build();
        } catch (Exception e) {
            log.error("秒传处理失败: taskId={}", taskId, e);
            throw new StorageOperationException("秒传处理失败: " + e.getMessage(), e);
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
        FileTransferTask task = getTaskFromCacheOrDB(taskId);
        if (task.getStatus() == TransferTaskStatus.canceled) {
            log.info("任务已取消，停止上传: taskId={}, chunkIndex={}", taskId, chunkIndex);
            return;
        }
        if (task.getStatus() == TransferTaskStatus.paused) {
            log.info("任务已暂停，停止上传: taskId={}, chunkIndex={}", taskId, chunkIndex);
            return;
        }
        if (!TransferTaskStatus.uploading.equals(task.getStatus())) {
            throw new BusinessException("任务状态不正确: " + task.getStatus());
        }
        // 检查分片是否已存在（避免重复上传）
        if (cacheManager.isChunkTransferred(taskId, chunkIndex)) {
            log.info("分片已存在，跳过上传: taskId={}, chunkIndex={}", taskId, chunkIndex);
            return;
        }

        IStorageOperationService storageService =
                storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());

        String eTag;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
            eTag = storageService.uploadPart(
                    task.getObjectKey(),
                    task.getUploadId(),
                    chunkIndex,
                    fileBytes.length,
                    bis);
        }
        cacheManager.addTransferredChunk(taskId, chunkIndex, eTag);
        cacheManager.recordTransferredBytes(taskId, fileBytes.length);

        // 推送进度事件
        Integer uploadedChunks = cacheManager.getTransferredChunks(taskId);
        long uploadedBytes = cacheManager.getTransferredBytes(taskId);
        transferSseService.sendProgressEvent(task.getUserId(), taskId, 
            uploadedBytes, task.getFileSize(), uploadedChunks, task.getTotalChunks());

        log.info("分片上传成功: taskId={}, chunkIndex={}, progress={}/{}",
                taskId, chunkIndex, uploadedChunks, task.getTotalChunks());
    }

    @Override
    public void pauseTransfer(String taskId) {
        FileTransferTask task = null;
        try {
            task = getTaskFromCacheOrDB(taskId);
            TransferTaskStatus currentStatus = task.getStatus();
            if (!TransferTaskStatus.uploading.equals(currentStatus)
                    && !TransferTaskStatus.downloading.equals(currentStatus)) {
                throw new BusinessException("当前任务状态不支持暂停操作: " + currentStatus);
            }
            // 更新数据库状态
            updateTaskStatus(task, TransferTaskStatus.paused);
            
            // 推送暂停状态事件
            transferSseService.sendStatusEvent(task.getUserId(), taskId, 
                TransferTaskStatus.paused.name(), "任务已暂停");
            
            log.info("暂停任务: taskId={}", taskId);
        } catch (Exception e) {
            log.error("暂停失败: taskId={}", taskId, e);
            if (task != null) {
                transferSseService.sendErrorEvent(task.getUserId(), taskId, 
                    "PAUSE_FAILED", "暂停失败: " + e.getMessage());
            }
            throw new StorageOperationException("暂停失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void resumeTransfer(String taskId) {
        FileTransferTask task = null;
        try {
            task = getTaskFromCacheOrDB(taskId);
            if (!TransferTaskStatus.paused.equals(task.getStatus())) {
                throw new StorageOperationException("当前任务状态不支持继续操作");
            }
            TransferTaskStatus newStatus = task.getTaskType() == TransferTaskType.upload
                    ? TransferTaskStatus.uploading
                    : TransferTaskStatus.downloading;

            updateTaskStatus(task, newStatus);

            Map<Integer, String> transferredChunks = cacheManager.getTransferredChunkList(taskId);
            
            // 推送恢复状态事件
            transferSseService.sendStatusEvent(task.getUserId(), taskId, 
                newStatus.name(), "任务已恢复");
            
            log.info("继续任务成功: taskId={}, transferredChunks={}/{}", taskId, transferredChunks.size(), task.getTotalChunks());
        } catch (Exception e) {
            log.error("继续任务失败: taskId={}", taskId, e);
            if (task != null) {
                transferSseService.sendErrorEvent(task.getUserId(), taskId, 
                    "RESUME_FAILED", "继续任务失败: " + e.getMessage());
            }
            throw new StorageOperationException("继续任务失败: " + e.getMessage(), e);
        }

    }

    @Override
    public Set<Integer> getUploadedChunks(String taskId) {
        Map<Integer, String> chunks = cacheManager.getTransferredChunkList(taskId);
        return chunks.keySet();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTransfer(String taskId) {
        FileTransferTask task = null;
        try {
            task = getTaskFromCacheOrDB(taskId);
            TransferTaskStatus currentStatus = task.getStatus();

            // 检查任务状态是否可以取消
            if (TransferTaskStatus.completed.equals(currentStatus)) {
                throw new StorageOperationException("任务已完成，无法取消");
            }
            
            // 验证状态转换合法性
            validateStateTransition(currentStatus, TransferTaskStatus.canceled);
            
            // 推送取消中状态事件
            transferSseService.sendStatusEvent(task.getUserId(), taskId, 
                "cancelling", "正在取消任务");
            
            //修改状态为已取消
            cacheManager.updateTaskStatus(taskId, TransferTaskStatus.canceled);
            // 短暂延迟，确保前端收到消息并停止上传
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 如果是上传任务且已经初始化了分片上传，需要中止分片上传
            if (TransferTaskType.upload.equals(task.getTaskType())
                    && task.getUploadId() != null
                    && !task.getUploadId().isEmpty()) {
                try {
                    IStorageOperationService storageService =
                            storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());

                    // 中止分片上传，清理存储端的临时数据
                    storageService.abortMultipartUpload(task.getObjectKey(), task.getUploadId());
                    log.info("已中止分片上传: taskId={}, uploadId={}", taskId, task.getUploadId());
                } catch (Exception e) {
                    log.error("中止分片上传失败: taskId={}, uploadId={}", taskId, task.getUploadId(), e);
                }
            }

            this.removeById(task.getId());
            cacheManager.cleanTask(taskId);
            
            // 推送已取消状态事件
            transferSseService.sendStatusEvent(task.getUserId(), taskId, 
                TransferTaskStatus.canceled.name(), "任务已取消");
            
            log.info("取消传输任务成功: taskId={}", taskId);
        } catch (Exception e) {
            log.error("取消传输任务异常: taskId={}", taskId, e);
            if (task != null) {
                transferSseService.sendErrorEvent(task.getUserId(), taskId, 
                    "CANCEL_FAILED", "取消失败: " + e.getMessage());
            }
            throw new StorageOperationException("取消传输任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public FileInfo mergeChunks(String taskId) {
        return doMergeChunks(taskId);
    }

    public FileInfo doMergeChunks(String taskId) {
        FileTransferTask task = null;
        try {
            log.info("开始合并文件: taskId={}", taskId);
            task = getByTaskId(taskId);
            if (task == null) {
                throw new StorageOperationException("上传任务不存在: " + taskId);
            }
            
            // 验证当前状态是否允许合并
            if (!TransferTaskStatus.uploading.equals(task.getStatus())) {
                throw new BusinessException("任务状态不正确，当前状态: " + task.getStatus());
            }
            
            Integer uploadedCount = cacheManager.getTransferredChunks(taskId);
            if (!uploadedCount.equals(task.getTotalChunks())) {
                log.error("分片未全部上传，拒绝合并: taskId={}, uploaded={}, total={}",
                        taskId, uploadedCount, task.getTotalChunks());
                throw new StorageOperationException(
                        String.format("分片不完整：已上传 %d/%d", uploadedCount, task.getTotalChunks())
                );
            }
            
            // 更新状态为 merging
            updateTaskStatus(task, TransferTaskStatus.merging);
            
            // 推送 merging 状态事件
            transferSseService.sendStatusEvent(task.getUserId(), taskId, 
                TransferTaskStatus.merging.name(), "正在合并分片");
            
            log.info("状态已更新为 merging: taskId={}", taskId);
            IStorageOperationService storageService = storageServiceFacade.getStorageService(task.getStoragePlatformSettingId());
            Map<Integer, String> chunkETags = cacheManager.getTransferredChunkList(taskId);
            // 获取存储服务并完成分片合并
            List<Map<String, Object>> partETags = new ArrayList<>();
            for (int i = 0; i < task.getTotalChunks(); i++) {
                String etag = chunkETags.get(i);
                if (etag == null || etag.isEmpty()) {
                    log.error("分片ETag丢失: taskId={}, chunkIndex={}", taskId, i);
                    throw new StorageOperationException(
                            String.format("分片 %d 的 ETag 丢失", i)
                    );
                }
                Map<String, Object> partInfo = new HashMap<>();
                partInfo.put("partNumber", i);
                partInfo.put("eTag", etag);
                partETags.add(partInfo);
            }
            storageService.completeMultipartUpload(
                    task.getObjectKey(),
                    task.getUploadId(),
                    partETags
            );

            String fileId = IdUtil.fastSimpleUUID();

            LocalDateTime completeTime = LocalDateTime.now();

            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(fileId);
            fileInfo.setObjectKey(task.getObjectKey());
            fileInfo.setOriginalName(task.getFileName());
            fileInfo.setDisplayName(task.getFileName());
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

            fileInfoService.save(fileInfo);

            // 更新任务状态为已完成
            task.setStatus(TransferTaskStatus.completed);
            // 最终同步一次分片数量
            task.setUploadedChunks(uploadedCount);
            task.setCompleteTime(completeTime);
            this.updateById(task);

            cacheManager.cleanTask(taskId);

            // 推送完成事件
            transferSseService.sendCompleteEvent(task.getUserId(), taskId, 
                fileInfo.getId(), fileInfo.getOriginalName(), fileInfo.getSize());

            log.info("分片合并成功: taskId={}, fileId={}, fileName={}", taskId, fileInfo.getId(), fileInfo.getOriginalName());

            return fileInfo;

        } catch (Exception e) {
            log.error("分片合并失败: taskId={}", taskId, e);
            
            // 推送错误事件
            if (task != null) {
                transferSseService.sendErrorEvent(task.getUserId(), taskId, 
                    "MERGE_FAILED", "文件合并失败: " + e.getMessage());
            }
            
            throw new StorageOperationException("分片合并失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据任务ID获取任务信息
     *
     * @param taskId
     * @return
     */
    private FileTransferTask getByTaskId(String taskId) {
        return this.getOne(
                new QueryWrapper().where(FILE_TRANSFER_TASK.TASK_ID.eq(taskId)
                )
        );
    }

    private FileTransferTask getTaskFromCacheOrDB(String taskId) {
        FileTransferTask task = cacheManager.getTaskFromCache(taskId);
        if (task == null) {
            task = this.getOne(
                    QueryWrapper.create().where(FileTransferTask::getTaskId).eq(taskId)
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
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param newStatus 目标状态
     * @throws BusinessException 如果状态转换不合法
     */
    private void validateStateTransition(TransferTaskStatus currentStatus, TransferTaskStatus newStatus) {
        // 如果状态相同，允许（幂等操作）
        if (currentStatus == newStatus) {
            return;
        }
        
        // 根据状态机规则验证转换合法性
        boolean isValid = false;
        
        switch (currentStatus) {
            case initialized:
                // initialized 可以转换到: checking, failed, canceled
                isValid = newStatus == TransferTaskStatus.checking 
                       || newStatus == TransferTaskStatus.failed 
                       || newStatus == TransferTaskStatus.canceled;
                break;
                
            case checking:
                // checking 可以转换到: uploading, completed, failed, canceled
                isValid = newStatus == TransferTaskStatus.uploading 
                       || newStatus == TransferTaskStatus.completed 
                       || newStatus == TransferTaskStatus.failed 
                       || newStatus == TransferTaskStatus.canceled;
                break;
                
            case uploading:
                // uploading 可以转换到: paused, merging, failed, canceled
                isValid = newStatus == TransferTaskStatus.paused 
                       || newStatus == TransferTaskStatus.merging 
                       || newStatus == TransferTaskStatus.failed 
                       || newStatus == TransferTaskStatus.canceled;
                break;
                
            case paused:
                // paused 可以转换到: uploading, downloading, canceled
                isValid = newStatus == TransferTaskStatus.uploading 
                       || newStatus == TransferTaskStatus.downloading 
                       || newStatus == TransferTaskStatus.canceled;
                break;
                
            case merging:
                // merging 可以转换到: completed, failed
                isValid = newStatus == TransferTaskStatus.completed 
                       || newStatus == TransferTaskStatus.failed;
                break;
                
            case failed:
                // failed 可以转换到: initialized (重试)
                isValid = newStatus == TransferTaskStatus.initialized;
                break;
                
            case downloading:
                // downloading 可以转换到: paused, completed, failed, canceled
                isValid = newStatus == TransferTaskStatus.paused 
                       || newStatus == TransferTaskStatus.completed 
                       || newStatus == TransferTaskStatus.failed 
                       || newStatus == TransferTaskStatus.canceled;
                break;
                
            case completed:
            case canceled:
                // completed 和 canceled 是终态，不允许转换
                isValid = false;
                break;
                
            default:
                isValid = false;
                break;
        }
        
        if (!isValid) {
            throw new BusinessException(
                String.format("非法的状态转换: %s -> %s", currentStatus, newStatus)
            );
        }
    }

    /**
     * 更新任务状态（数据库 + 缓存）
     */
    private void updateTaskStatus(FileTransferTask task, TransferTaskStatus newStatus) {
        // 验证状态转换合法性
        validateStateTransition(task.getStatus(), newStatus);
        
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now());
        this.updateById(task);
        cacheManager.cacheTask(task);
        cacheManager.updateTaskStatus(task.getTaskId(), newStatus);
    }

    /**
     * 计算分片总数
     *
     * @param fileSize  文件大小（字节）
     * @param chunkSize 分片大小（字节）
     * @return 分片总数
     */
    private int calculateTotalChunks(Long fileSize, Long chunkSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException("文件大小无效");
        }

        if (chunkSize == null || chunkSize <= 0) {
            throw new BusinessException("分片大小无效");
        }
        // 向上取整
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        log.debug("计算分片数: fileSize={}, chunkSize={}, totalChunks={}",
                fileSize, chunkSize, totalChunks);

        return totalChunks;
    }

    @Override
    public void clearTransfers() {
        String userId = StpUtil.getLoginIdAsString();
        String storagePlatformSettingId = StoragePlatformContextHolder.getConfigId();

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.where(FILE_TRANSFER_TASK.STATUS.eq(TransferTaskStatus.completed))
                .and(FILE_TRANSFER_TASK.USER_ID.eq(userId))
                .and(FILE_TRANSFER_TASK.STORAGE_PLATFORM_SETTING_ID.eq(storagePlatformSettingId));
        List<FileTransferTask> tasks = this.list(queryWrapper);
        this.removeByIds(tasks);

        List<String> taskIds = tasks.stream()
                .map(FileTransferTask::getTaskId)
                .collect(Collectors.toList());

        //清除缓存
        cacheManager.cleanTasks(taskIds);
    }

    @Override
    public FileDownloadVO downloadFile(String fileId) {
        String userId = StpUtil.getLoginIdAsString();
        FileInfo fileInfo = fileInfoService.getById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("下载失败，该文件不存在");
        }
        if (!fileInfo.getUserId().equals(userId)) {
            throw new BusinessException("无权限下载");
        }
        IStorageOperationService storageService = storageServiceFacade.getStorageService(fileInfo.getStoragePlatformSettingId());
        if (!storageService.isFileExist(fileInfo.getObjectKey())) {
            throw new BusinessException("下载失败，该文件不存在");
        }
        InputStream inputStream = storageService.downloadFile(fileInfo.getObjectKey());
        InputStreamResource resource = new InputStreamResource(inputStream);
        FileDownloadVO downloadVO = new FileDownloadVO();
        downloadVO.setFileName(fileInfo.getDisplayName());
        downloadVO.setFileSize(fileInfo.getSize());
        downloadVO.setResource(resource);
        return downloadVO;
    }

}
