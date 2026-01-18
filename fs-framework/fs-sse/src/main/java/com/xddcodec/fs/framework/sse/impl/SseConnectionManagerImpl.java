package com.xddcodec.fs.framework.sse.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xddcodec.fs.framework.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE连接管理器实现
 * 使用ConcurrentHashMap管理用户连接，确保线程安全
 * 支持心跳保活机制，防止连接超时
 * 
 * @author xddcodec
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionManagerImpl implements SseConnectionManager {
    
    /**
     * SSE连接超时时间：60分钟（毫秒）
     */
    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    
    /**
     * 心跳间隔：25秒（避免 Nginx 默认 30 秒超时）
     */
    private static final long HEARTBEAT_INTERVAL = 25 * 1000L;
    
    /**
     * 用户连接池，key为用户ID，value为SseEmitter
     */
    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();
    
    /**
     * 心跳任务调度器
     */
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 心跳任务执行器
     */
    private final java.util.concurrent.ScheduledExecutorService heartbeatExecutor = 
            java.util.concurrent.Executors.newScheduledThreadPool(2);
    
    @Override
    public SseEmitter createConnection(String userId) {
        log.info("Creating SSE connection for user: {}", userId);
        
        // 如果用户已有连接，先清理旧连接（不调用 complete，避免异常）
        SseEmitter oldEmitter = connections.remove(userId);
        if (oldEmitter != null) {
            log.info("User {} already has a connection, replacing with new connection", userId);
            stopHeartbeat(userId);
            // 不调用 complete()，让旧连接自然断开
        }
        
        // 创建新的SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            stopHeartbeat(userId);
            connections.remove(userId);
        });
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for user: {}", userId);
            stopHeartbeat(userId);
            connections.remove(userId);
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            log.warn("SSE connection closed with error for user: {}, reason: {}", 
                    userId, throwable.getMessage());
            stopHeartbeat(userId);
            connections.remove(userId);
        });
        
        // 保存连接
        connections.put(userId, emitter);
        
        // 启动心跳
        startHeartbeat(userId, emitter);
        
        // 发送连接成功事件（可选，帮助前端确认连接建立）
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"message\":\"SSE connection established\"}"));
            log.debug("Sent connection confirmation to user: {}", userId);
        } catch (IOException e) {
            log.warn("Failed to send connection confirmation to user: {}", userId, e);
        }
        
        log.info("SSE connection created successfully for user: {}", userId);
        
        return emitter;
    }
    
    /**
     * 启动心跳任务
     */
    private void startHeartbeat(String userId, SseEmitter emitter) {
        // 停止旧的心跳任务（如果存在）
        stopHeartbeat(userId);
        
        // 创建新的心跳任务
        java.util.concurrent.ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                // 发送心跳注释（SSE 注释不会触发客户端事件）
                emitter.send(SseEmitter.event().comment("heartbeat"));
                log.debug("Heartbeat sent to user: {}", userId);
            } catch (IOException e) {
                log.warn("Heartbeat failed for user: {}, connection may be closed", userId);
                stopHeartbeat(userId);
                removeConnection(userId);
            } catch (Exception e) {
                log.error("Unexpected error in heartbeat for user: {}", userId, e);
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        heartbeatTasks.put(userId, task);
        log.debug("Heartbeat started for user: {}, interval: {}ms", userId, HEARTBEAT_INTERVAL);
    }
    
    /**
     * 停止心跳任务
     */
    private void stopHeartbeat(String userId) {
        java.util.concurrent.ScheduledFuture<?> task = heartbeatTasks.remove(userId);
        if (task != null) {
            task.cancel(false);
            log.debug("Heartbeat stopped for user: {}", userId);
        }
    }
    
    @Override
    public void removeConnection(String userId) {
        stopHeartbeat(userId);
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE connection removed for user: {}", userId);
            } catch (Exception e) {
                log.error("Error completing SSE connection for user: {}", userId, e);
            }
        }
    }
    
    @Override
    public void sendEvent(String userId, String eventType, Object data) {
        SseEmitter emitter = connections.get(userId);
        
        if (emitter == null) {
            log.warn("No active SSE connection found for user: {}, eventType: {}", userId, eventType);
            return;
        }
        
        try {
            // 将数据序列化为JSON字符串
            String jsonData = objectMapper.writeValueAsString(data);
            
            // 发送SSE事件
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(jsonData));
            
            log.debug("SSE event sent to user {}: type={}", userId, eventType);
        } catch (IOException e) {
            // SSE 连接断开是正常现象（客户端关闭、网络问题等），使用 WARN 级别
            log.warn("SSE connection closed, failed to send event to user {}: type={}, reason: {}", 
                    userId, eventType, e.getMessage());
            // 发送失败，移除失效连接
            removeConnection(userId);
        } catch (Exception e) {
            log.error("Unexpected error sending SSE event to user {}: type={}", userId, eventType, e);
            // 发生异常也移除连接
            removeConnection(userId);
        }
    }
    
    @Override
    public boolean hasConnection(String userId) {
        return connections.containsKey(userId);
    }
}
