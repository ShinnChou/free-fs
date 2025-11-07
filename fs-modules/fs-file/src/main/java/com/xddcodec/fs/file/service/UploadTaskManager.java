package com.xddcodec.fs.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 上传任务管理器
 * 用于控制批量上传的并发数（最多5个文件同时上传）
 *
 * @Author: xddcode
 * @Date: 2025/11/07
 */
@Slf4j
@Service
public class UploadTaskManager {

    /**
     * 信号量：限制最多5个并发上传任务
     */
    private final Semaphore semaphore = new Semaphore(5);

    /**
     * 当前正在上传的任务（userId -> taskIds）
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> userTasks = new ConcurrentHashMap<>();

    /**
     * 尝试获取上传许可
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 是否获取成功
     */
    public boolean tryAcquire(String userId, String taskId) {
        if (semaphore.tryAcquire()) {
            userTasks.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .put(taskId, System.currentTimeMillis());
            log.debug("用户 {} 的任务 {} 获取上传许可，当前可用许可: {}", userId, taskId, semaphore.availablePermits());
            return true;
        }
        log.warn("用户 {} 的任务 {} 获取上传许可失败，当前已达最大并发数", userId, taskId);
        return false;
    }

    /**
     * 阻塞式获取上传许可
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @throws InterruptedException 中断异常
     */
    public void acquire(String userId, String taskId) throws InterruptedException {
        semaphore.acquire();
        userTasks.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(taskId, System.currentTimeMillis());
        log.debug("用户 {} 的任务 {} 获取上传许可，当前可用许可: {}", userId, taskId, semaphore.availablePermits());
    }

    /**
     * 释放上传许可
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    public void release(String userId, String taskId) {
        ConcurrentHashMap<String, Long> tasks = userTasks.get(userId);
        if (tasks != null) {
            tasks.remove(taskId);
            if (tasks.isEmpty()) {
                userTasks.remove(userId);
            }
        }
        semaphore.release();
        log.debug("用户 {} 的任务 {} 释放上传许可，当前可用许可: {}", userId, taskId, semaphore.availablePermits());
    }

    /**
     * 获取当前可用的上传许可数
     *
     * @return 可用许可数
     */
    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * 获取用户当前正在上传的任务数
     *
     * @param userId 用户ID
     * @return 正在上传的任务数
     */
    public int getUserActiveTaskCount(String userId) {
        ConcurrentHashMap<String, Long> tasks = userTasks.get(userId);
        return tasks == null ? 0 : tasks.size();
    }

    /**
     * 检查任务是否正在上传
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 是否正在上传
     */
    public boolean isTaskActive(String userId, String taskId) {
        ConcurrentHashMap<String, Long> tasks = userTasks.get(userId);
        return tasks != null && tasks.containsKey(taskId);
    }
}



