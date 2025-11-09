package com.xddcodec.fs.file.cache;

import com.xddcodec.fs.file.domain.FileUploadTask;
import com.xddcodec.fs.framework.common.enums.UploadTaskStatus;
import com.xddcodec.fs.framework.redis.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 上传任务 Redis 缓存管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTaskCacheManager {

    private final RedisRepository redisRepository;
    private final RedissonClient redissonClient;
    // Redis Key 前缀
    private static final String TASK_PREFIX = "upload:task:";
    private static final String CHUNKS_PREFIX = "upload:chunks:";
    private static final String BYTES_PREFIX = "upload:bytes:";
    private static final String START_TIME_PREFIX = "upload:startTime:";
    private static final String USER_COUNT_PREFIX = "upload:userCount:";
    private static final String MERGE_LOCK_PREFIX = "upload:lock:merge:";
    // 过期时间
    private static final long TASK_EXPIRE_DAYS = 7 * 24 * 60 * 60; // 7天（秒）
    private static final long USER_COUNT_EXPIRE_HOURS = 60 * 60; // 1小时（秒）

    /**
     * 缓存任务
     */
    public void cacheTask(FileUploadTask task) {
        String key = TASK_PREFIX + task.getTaskId();

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("taskId", task.getTaskId());
        taskMap.put("userId", task.getUserId());
        taskMap.put("parentId", task.getParentId());
        taskMap.put("objectKey", task.getObjectKey());
        taskMap.put("fileName", task.getFileName());
        taskMap.put("fileSize", task.getFileSize());
        taskMap.put("fileMd5", task.getFileMd5());
        taskMap.put("suffix", task.getSuffix());
        taskMap.put("mimeType", task.getMimeType());
        taskMap.put("totalChunks", task.getTotalChunks());
        taskMap.put("uploadedChunks", task.getUploadedChunks());
        taskMap.put("chunkSize", task.getChunkSize());
        taskMap.put("storagePlatformSettingId", task.getStoragePlatformSettingId());
        taskMap.put("status", task.getStatus().name());
        taskMap.put("createdAt", task.getCreatedAt().toString());
        taskMap.put("updatedAt", task.getUpdatedAt().toString());

        if (task.getStartTime() != null) {
            taskMap.put("startTime", task.getStartTime().toString());
        }
        if (task.getCompleteTime() != null) {
            taskMap.put("completeTime", task.getCompleteTime().toString());
        }
        redisRepository.hmset(key, taskMap, TASK_EXPIRE_DAYS);

        log.debug("缓存任务: taskId={}", task.getTaskId());
    }

    /**
     * 从缓存获取任务
     */
    public FileUploadTask getTaskFromCache(String taskId) {
        String key = TASK_PREFIX + taskId;

        Map<Object, Object> taskMap = redisRepository.hmget(key);
        if (taskMap.isEmpty()) {
            return null;
        }
        FileUploadTask task = new FileUploadTask();
        task.setTaskId((String) taskMap.get("taskId"));
        task.setUserId((String) taskMap.get("userId"));
        task.setParentId((String) taskMap.get("parentId"));
        task.setObjectKey((String) taskMap.get("objectKey"));
        task.setFileName((String) taskMap.get("fileName"));
        task.setFileSize(Long.parseLong(taskMap.get("fileSize").toString()));
        task.setFileMd5((String) taskMap.get("fileMd5"));
        task.setSuffix((String) taskMap.get("suffix"));
        task.setMimeType((String) taskMap.get("mimeType"));
        task.setTotalChunks(Integer.parseInt(taskMap.get("totalChunks").toString()));
        task.setUploadedChunks(Integer.parseInt(taskMap.get("uploadedChunks").toString()));
        task.setChunkSize(Long.parseLong(taskMap.get("chunkSize").toString()));
        task.setStoragePlatformSettingId((String) taskMap.get("storagePlatformSettingId"));
        task.setStatus(UploadTaskStatus.valueOf((String) taskMap.get("status")));
        task.setCreatedAt(LocalDateTime.parse((String) taskMap.get("createdAt")));
        task.setUpdatedAt(LocalDateTime.parse((String) taskMap.get("updatedAt")));

        if (taskMap.containsKey("startTime")) {
            task.setStartTime(LocalDateTime.parse((String) taskMap.get("startTime")));
        }
        if (taskMap.containsKey("completeTime")) {
            task.setCompleteTime(LocalDateTime.parse((String) taskMap.get("completeTime")));
        }
        log.debug("从缓存获取任务: taskId={}", taskId);
        return task;
    }

    /**
     * 原子递增已上传分片数
     */
    public void incrementUploadedChunks(String taskId) {
        String key = TASK_PREFIX + taskId;
        redisRepository.hincr(key, "uploadedChunks", 1);
        redisRepository.hset(key, "updatedAt", LocalDateTime.now().toString());

        log.debug("递增已上传分片数: taskId={}", taskId);
    }

    /**
     * 获取已上传分片数
     */
    public Integer getUploadedChunks(String taskId) {
        String key = TASK_PREFIX + taskId;
        Object value = redisRepository.hget(key, "uploadedChunks");
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    /**
     * 记录已上传的分片
     */
    public void addUploadedChunk(String taskId, Integer chunkIndex) {
        String key = CHUNKS_PREFIX + taskId;
        redisRepository.sSetAndTime(key, TASK_EXPIRE_DAYS, chunkIndex);

        log.debug("记录已上传分片: taskId={}, chunkIndex={}", taskId, chunkIndex);
    }

    /**
     * 获取已上传的分片列表
     */
    public Set<Integer> getUploadedChunkList(String taskId) {
        String key = CHUNKS_PREFIX + taskId;
        Set<Object> chunks = redisRepository.sGet(key);

        if (chunks == null || chunks.isEmpty()) {
            return Set.of();
        }

        return chunks.stream()
                .map(obj -> Integer.parseInt(obj.toString()))
                .collect(Collectors.toSet());
    }

    /**
     * 检查分片是否已上传
     */
    public boolean isChunkUploaded(String taskId, Integer chunkIndex) {
        String key = CHUNKS_PREFIX + taskId;
        return redisRepository.sHasKey(key, chunkIndex);
    }

    /**
     * 记录上传字节数
     */
    public void recordUploadedBytes(String taskId, long bytes) {
        String key = BYTES_PREFIX + taskId;
        redisRepository.incr(key, bytes);
        redisRepository.expire(key, TASK_EXPIRE_DAYS);
    }

    /**
     * 获取已上传字节数
     */
    public long getUploadedBytes(String taskId) {
        String key = BYTES_PREFIX + taskId;
        Object value = redisRepository.get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    /**
     * 记录任务开始时间
     */
    public void recordStartTime(String taskId) {
        String key = START_TIME_PREFIX + taskId;
        redisRepository.setExpire(key, System.currentTimeMillis(), TASK_EXPIRE_DAYS);
    }

    /**
     * 获取任务开始时间
     */
    public Long getStartTime(String taskId) {
        String key = START_TIME_PREFIX + taskId;
        Object value = redisRepository.get(key);
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    /**
     * 递增用户上传文件数
     */
    public void incrementUserUploadCount(String userId) {
        String key = USER_COUNT_PREFIX + userId;
        redisRepository.incr(key, 1);
        redisRepository.expire(key, USER_COUNT_EXPIRE_HOURS);
    }

    /**
     * 递减用户上传文件数
     */
    public void decrementUserUploadCount(String userId) {
        String key = USER_COUNT_PREFIX + userId;
        Long count = redisRepository.decr(key, 1);

        // 如果计数降到 0 或以下，删除 key
        if (count != null && count <= 0) {
            redisRepository.del(key);
        }
    }

    /**
     * 获取用户上传文件数
     */
    public int getUserUploadCount(String userId) {
        String key = USER_COUNT_PREFIX + userId;
        Object value = redisRepository.get(key);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    /**
     * 检查用户是否可以开始上传（限制最多5个并发上传）
     */
    public boolean canUserStartUpload(String userId, int maxConcurrent) {
        int currentCount = getUserUploadCount(userId);
        return currentCount < maxConcurrent;
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, UploadTaskStatus status) {
        String key = TASK_PREFIX + taskId;
        redisRepository.hset(key, "status", status.name());
        redisRepository.hset(key, "updatedAt", LocalDateTime.now().toString());
    }

    /**
     * 更新任务完成时间
     */
    public void updateTaskCompleteTime(String taskId, LocalDateTime completeTime) {
        String key = TASK_PREFIX + taskId;
        redisRepository.hset(key, "completeTime", completeTime.toString());
        redisRepository.hset(key, "status", UploadTaskStatus.completed.name());
        redisRepository.hset(key, "updatedAt", LocalDateTime.now().toString());
    }

    /**
     * 获取分布式锁（Redisson）
     */
    public RLock getMergeLock(String taskId) {
        String lockKey = MERGE_LOCK_PREFIX + taskId;
        return redissonClient.getLock(lockKey);
    }

    /**
     * 清理任务缓存
     */
    public void cleanTask(String taskId) {
        redisRepository.del(
                TASK_PREFIX + taskId,
                CHUNKS_PREFIX + taskId,
                BYTES_PREFIX + taskId,
                START_TIME_PREFIX + taskId
        );

        log.info("清理任务缓存: taskId={}", taskId);
    }

    /**
     * 延长任务缓存过期时间（任务完成后调用）
     */
    public void extendTaskExpire(String taskId, long days) {
        long seconds = days * 24 * 60 * 60;
        redisRepository.expire(TASK_PREFIX + taskId, seconds);
        redisRepository.expire(CHUNKS_PREFIX + taskId, seconds);
        redisRepository.expire(BYTES_PREFIX + taskId, seconds);
        redisRepository.expire(START_TIME_PREFIX + taskId, seconds);
    }
}
