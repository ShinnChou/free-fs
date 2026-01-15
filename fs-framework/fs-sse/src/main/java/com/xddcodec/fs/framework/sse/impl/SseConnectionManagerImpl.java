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
 * 
 * @author xddcodec
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseConnectionManagerImpl implements SseConnectionManager {
    
    /**
     * SSE连接超时时间：30分钟（毫秒）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    /**
     * 用户连接池，key为用户ID，value为SseEmitter
     */
    private final ConcurrentHashMap<String, SseEmitter> connections = new ConcurrentHashMap<>();
    
    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper;
    
    @Override
    public SseEmitter createConnection(String userId) {
        log.info("Creating SSE connection for user: {}", userId);
        
        // 如果用户已有连接，先关闭旧连接
        if (connections.containsKey(userId)) {
            log.info("User {} already has a connection, closing old connection", userId);
            removeConnection(userId);
        }
        
        // 创建新的SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 设置完成回调
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            connections.remove(userId);
        });
        
        // 设置超时回调
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            connections.remove(userId);
        });
        
        // 设置错误回调
        emitter.onError(throwable -> {
            log.info("SSE connection closed with error for user: {}, reason: {}", 
                    userId, throwable.getMessage());
            connections.remove(userId);
        });
        
        // 保存连接
        connections.put(userId, emitter);
        log.info("SSE connection created successfully for user: {}", userId);
        
        return emitter;
    }
    
    @Override
    public void removeConnection(String userId) {
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
            log.warn("No active SSE connection found for user: {}", userId);
            return;
        }
        
        try {
            // 将数据序列化为JSON字符串
            String jsonData = objectMapper.writeValueAsString(data);
            
            // 发送SSE事件
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(jsonData));
            
            log.info("SSE event sent to user {}: type={}, data={}", userId, eventType, jsonData);
        } catch (IOException e) {
            // SSE 连接断开是正常现象（客户端关闭、网络问题等），使用 WARN 级别
            log.warn("SSE connection closed, failed to send event to user {}: type={}, reason: {}", 
                    userId, eventType, e.getMessage());
            // 发送失败，移除失效连接
            removeConnection(userId);
        } catch (Exception e) {
            log.error("Unexpected error sending SSE event to user {}: type={}", userId, eventType, e);
        }
    }
    
    @Override
    public boolean hasConnection(String userId) {
        return connections.containsKey(userId);
    }
}
