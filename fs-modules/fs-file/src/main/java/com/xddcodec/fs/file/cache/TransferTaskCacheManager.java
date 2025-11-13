package com.xddcodec.fs.file.cache;

import com.xddcodec.fs.file.domain.FileTransferTask;
import com.xddcodec.fs.file.enums.TransferTaskStatus;
import com.xddcodec.fs.file.enums.TransferTaskType;
import com.xddcodec.fs.framework.redis.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上传任务 Redis 缓存管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferTaskCacheManager {

    private final RedisRepository redisRepository;
    private final RedissonClient redissonClient;
    private static final String TASK_PREFIX = "transfer:task:";
    private static final String CHUNKS_PREFIX = "transfer:chunks:";
    private static final String BYTES_PREFIX = "transfer:bytes:";
    private static final String START_TIME_PREFIX = "transfer:startTime:";
    private static final String MERGE_LOCK_PREFIX = "transfer:lock:merge:";
    private static final long TASK_EXPIRE_DAYS = 7 * 24 * 60 * 60;

    /**
     * 缓存任务 - 直接存储对象
     */
    public void cacheTask(FileTransferTask task) {
        if (task == null || task.getTaskId() == null) {
            log.warn("缓存任务参数无效");
            return;
        }
        // 从 Redis Set 获取真实分片数
        Integer realCount = getTransferredChunks(task.getTaskId());
        task.setUploadedChunks(realCount);  // 复用字段
        task.setUpdatedAt(LocalDateTime.now());
        String key = TASK_PREFIX + task.getTaskId();
        redisRepository.setExpire(key, task, TASK_EXPIRE_DAYS);
        log.debug("缓存任务: taskId={}, type={}, transferredChunks={}",
                task.getTaskId(), task.getTaskType(), realCount);
    }

    /**
     * 从缓存获取任务 - 直接获取对象
     */
    public FileTransferTask getTaskFromCache(String taskId) {
        String key = TASK_PREFIX + taskId;
        Object obj = redisRepository.get(key);
        if (obj == null) {
            log.debug("📭 缓存中不存在任务: taskId={}", taskId);
            return null;
        }
        if (obj instanceof FileTransferTask) {
            log.debug("从缓存获取任务: taskId={}", taskId);
            return (FileTransferTask) obj;
        }
        log.warn("缓存数据类型错误: taskId={}, type={}", taskId, obj.getClass().getName());
        return null;
    }

    /**
     * 获取已传输分片数（上传下载通用）
     */
    public Integer getTransferredChunks(String taskId) {
        String key = CHUNKS_PREFIX + taskId;
        Long size = redisRepository.hSize(key);
        return size != null ? size.intValue() : 0;
    }

    /**
     * 记录已传输的分片（上传下载通用）
     */
    public void addTransferredChunk(String taskId, Integer chunkIndex, String etag) {
        String key = CHUNKS_PREFIX + taskId;
        redisRepository.hset(key, String.valueOf(chunkIndex), etag);
        redisRepository.expire(key, TASK_EXPIRE_DAYS);
        log.debug("保存分片ETag: taskId={}, chunkIndex={}, etag={}", taskId, chunkIndex, etag);
    }

    /**
     * 获取所有分片的 ETag（按分片号排序）
     */
    public Map<Integer, String> getTransferredChunkList(String taskId) {
        String key = CHUNKS_PREFIX + taskId;
        Map<Object, Object> entries = redisRepository.hmget(key);

        if (entries == null || entries.isEmpty()) {
            log.warn("未找到任务的ETag记录: taskId={}", taskId);
            return Collections.emptyMap();
        }

        Map<Integer, String> result = new TreeMap<>(); // 使用 TreeMap 自动排序
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                Integer chunkIndex = Integer.parseInt(entry.getKey().toString());
                String etag = entry.getValue().toString();
                result.put(chunkIndex, etag);
            } catch (NumberFormatException e) {
                log.error("解析分片索引失败: key={}", entry.getKey(), e);
            }
        }

        log.debug("获取任务所有ETag: taskId={}, count={}", taskId, result.size());
        return result;
    }

    /**
     * 检查分片是否已传输（上传下载通用）
     */
    public boolean isChunkTransferred(String taskId, Integer chunkIndex) {
        String key = CHUNKS_PREFIX + taskId;
        return redisRepository.hHasKey(key, String.valueOf(chunkIndex));
    }

    /**
     * 检查是否所有分片都已传输（上传下载通用）
     */
    public boolean isAllChunksTransferred(String taskId, Integer totalChunks) {
        Integer transferredCount = getTransferredChunks(taskId);
        boolean allTransferred = transferredCount.equals(totalChunks);
        log.debug("检查分片是否全部传输: taskId={}, transferred={}, total={}, result={}",
                taskId, transferredCount, totalChunks, allTransferred);
        return allTransferred;
    }

    /**
     * 记录传输字节数（上传下载通用）
     */
    public void recordTransferredBytes(String taskId, long bytes) {
        String key = BYTES_PREFIX + taskId;
        redisRepository.incr(key, bytes);
        redisRepository.expire(key, TASK_EXPIRE_DAYS);
    }

    /**
     * 获取已传输字节数（上传下载通用）
     */
    public long getTransferredBytes(String taskId) {
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
    public void updateTaskStatus(String taskId, TransferTaskStatus status) {
        FileTransferTask task = getTaskFromCache(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            // 同步真实分片数
            Integer realCount = getTransferredChunks(taskId);
            task.setUploadedChunks(realCount);
            cacheTask(task);

            log.debug("更新任务状态: taskId={}, type={}, status={}, transferredChunks={}", taskId, task.getTaskType(), status, realCount);
        }
    }

    /**
     * 更新任务完成时间
     */
    public void updateTaskCompleteTime(String taskId, LocalDateTime completeTime) {
        FileTransferTask task = getTaskFromCache(taskId);
        if (task != null) {
            task.setCompleteTime(completeTime);
            task.setStatus(TransferTaskStatus.completed);
            task.setUpdatedAt(LocalDateTime.now());
            // 最终同步
            Integer realCount = getTransferredChunks(taskId);
            task.setUploadedChunks(realCount);
            cacheTask(task);

            String typeEmoji = task.getTaskType() == TransferTaskType.upload ? "📤" : "📥";
            log.info("{} 任务完成: taskId={}, type={}, transferredChunks={}",
                    typeEmoji, taskId, task.getTaskType(), realCount);
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
     * 批量清理任务缓存
     *
     * @param taskIds
     */
    public void cleanTasks(List<String> taskIds) {
        taskIds.forEach(this::cleanTask);
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
