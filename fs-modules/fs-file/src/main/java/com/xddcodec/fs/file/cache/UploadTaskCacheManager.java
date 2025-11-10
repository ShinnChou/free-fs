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
    private static final String MERGE_LOCK_PREFIX = "upload:lock:merge:";

    // 过期时间
    private static final long TASK_EXPIRE_DAYS = 7 * 24 * 60 * 60; // 7天（秒）

    /**
     * 缓存任务 - 直接存储对象
     */
    public void cacheTask(FileUploadTask task) {
        if (task == null || task.getTaskId() == null) {
            log.warn("缓存任务参数无效");
            return;
        }

        String key = TASK_PREFIX + task.getTaskId();
        redisRepository.setExpire(key, task, TASK_EXPIRE_DAYS);
        log.debug("缓存任务: taskId={}", task.getTaskId());
    }

    /**
     * 从缓存获取任务 - 直接获取对象
     */
    public FileUploadTask getTaskFromCache(String taskId) {
        String key = TASK_PREFIX + taskId;
        Object obj = redisRepository.get(key);

        if (obj == null) {
            log.debug("缓存中不存在任务: taskId={}", taskId);
            return null;
        }

        if (obj instanceof FileUploadTask) {
            log.debug("从缓存获取任务: taskId={}", taskId);
            return (FileUploadTask) obj;
        }

        log.warn("缓存数据类型错误: taskId={}, type={}", taskId, obj.getClass().getName());
        return null;
    }

    /**
     * 原子递增已上传分片数
     */
    public void incrementUploadedChunks(String taskId) {
        FileUploadTask task = getTaskFromCache(taskId);

        if (task != null) {
            task.setUploadedChunks(task.getUploadedChunks() + 1);
            task.setUpdatedAt(LocalDateTime.now());
            cacheTask(task);
            log.debug("递增已上传分片数: taskId={}, uploadedChunks={}",
                    taskId, task.getUploadedChunks());
        } else {
            log.warn("任务不存在，无法递增分片数: taskId={}", taskId);
        }
    }

    /**
     * 获取已上传分片数
     */
    public Integer getUploadedChunks(String taskId) {
        FileUploadTask task = getTaskFromCache(taskId);
        return task != null ? task.getUploadedChunks() : 0;
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
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, UploadTaskStatus status) {
        FileUploadTask task = getTaskFromCache(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            cacheTask(task);
            log.debug("更新任务状态: taskId={}, status={}", taskId, status);
        }
    }

    /**
     * 更新任务完成时间
     */
    public void updateTaskCompleteTime(String taskId, LocalDateTime completeTime) {
        FileUploadTask task = getTaskFromCache(taskId);
        if (task != null) {
            task.setCompleteTime(completeTime);
            task.setStatus(UploadTaskStatus.completed);
            task.setUpdatedAt(LocalDateTime.now());
            cacheTask(task);
            log.debug("更新任务完成时间: taskId={}", taskId);
        }
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
     * 延长任务缓存过期时间
     */
    public void extendTaskExpire(String taskId, long days) {
        long seconds = days * 24 * 60 * 60;
        redisRepository.expire(TASK_PREFIX + taskId, seconds);
        redisRepository.expire(CHUNKS_PREFIX + taskId, seconds);
        redisRepository.expire(BYTES_PREFIX + taskId, seconds);
        redisRepository.expire(START_TIME_PREFIX + taskId, seconds);
    }
}
