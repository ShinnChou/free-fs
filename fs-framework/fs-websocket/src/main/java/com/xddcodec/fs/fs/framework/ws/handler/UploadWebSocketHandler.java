package com.xddcodec.fs.fs.framework.ws.handler;

import com.xddcodec.fs.framework.common.utils.JsonUtils;
import com.xddcodec.fs.fs.framework.ws.core.UploadCommand;
import com.xddcodec.fs.fs.framework.ws.core.UploadMessage;
import com.xddcodec.fs.fs.framework.ws.core.UploadProgressDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class UploadWebSocketHandler extends TextWebSocketHandler {

    /**
     * 存储 userId -> Session集合 的映射
     * 使用 CopyOnWriteArraySet 保证并发读写安全，适合读多写少（推送多，连接少）的场景
     */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    // 存储 taskId -> userId 映射
    private final ConcurrentHashMap<String, String> taskUserMap = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        if (userId != null) {
            // 不再剔除旧连接，而是将新连接加入集合
            // computeIfAbsent：如果key不存在则创建新集合，存在则返回现有集合
            sessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);

            log.info("WebSocket 连接建立: userId={}, sessionId={}, 当前该用户在线窗口数={}", userId, session.getId(), sessions.get(userId).size());

            // 发送连接成功消息（只发给当前建立连接的这个窗口）
            sendMessageSafely(session, UploadMessage.success("连接成功"));
        } else {
            log.warn("WebSocket 连接缺少 userId 参数，关闭连接");
            session.close();
        }
    }

    /**
     * 接收消息
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String userId = getUserId(session);
        String payload = message.getPayload();
        log.debug("收到消息: userId={}, message={}", userId, payload);

        UploadCommand command = JsonUtils.parseObject(payload, UploadCommand.class);
        if (command != null && command.getAction() != null) {
            switch (command.getAction()) {
                case subscribe:
                    subscribeTask(userId, command.getTaskId());
                    // 响应只发给当前操作的窗口
                    sendMessageSafely(session, UploadMessage.success("订阅成功: " + command.getTaskId()));
                    break;
                case unsubscribe:
                    unsubscribeTask(command.getTaskId());
                    sendMessageSafely(session, UploadMessage.success("取消订阅: " + command.getTaskId()));
                    break;
                case ping:
                    sendMessageSafely(session, UploadMessage.pong());
                    break;
                default:
                    sendMessageSafely(session, UploadMessage.error("未知命令"));
            }
        } else {
            sendMessageSafely(session, UploadMessage.error("命令格式错误"));
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = getUserId(session);
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions != null) {
                // 只移除当前断开的 session
                userSessions.remove(session);
                log.info("WebSocket 连接关闭: userId={}, sessionId={}, 剩余窗口数={}",
                        userId, session.getId(), userSessions.size());

                // 只有当该用户的所有连接都断开时，才清除用户信息和任务订阅
                if (userSessions.isEmpty()) {
                    sessions.remove(userId);
                    // 清理该用户的所有任务订阅
                    taskUserMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));
                    log.info("用户 [{}] 所有连接已断开，清理资源", userId);
                }
            }
        }
    }

    /**
     * 传输错误处理
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String userId = getUserId(session);
        log.error("WebSocket 传输错误: userId={}, sessionId={}", userId, session.getId(), exception);

        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessions.remove(userId);
                }
            }
        }

        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.warn("关闭异常 WebSocket 连接失败", e);
        }
    }

    /**
     * 订阅任务
     */
    private void subscribeTask(String userId, String taskId) {
        taskUserMap.put(taskId, userId);
        log.info("订阅任务: userId={}, taskId={}", userId, taskId);
    }

    /**
     * 取消订阅
     */
    private void unsubscribeTask(String taskId) {
        taskUserMap.remove(taskId);
        log.info("取消订阅: taskId={}", taskId);
    }

    /**
     * 获取用户ID
     */
    private String getUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length >= 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    /**
     * 安全发送消息（底层方法，针对单个Session）
     */
    private void sendMessageSafely(WebSocketSession session, UploadMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            // 加锁防止多线程并发写入同一个Session导致报错
            synchronized (session) {
                if (!session.isOpen()) return;
                String json = JsonUtils.toJsonString(message);
                session.sendMessage(new TextMessage(Objects.requireNonNull(json)));
            }
        } catch (ClosedChannelException ignored) {
        } catch (IOException e) {
            log.warn("发送 WebSocket 消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("发送 WebSocket 消息异常: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 通用推送方法（核心修改：广播给用户的所有Session）
     */
    private void pushMessage(String taskId, UploadMessage message) {
        String userId = taskUserMap.get(taskId);
        if (userId == null) {
            // 任务未被订阅，忽略
            return;
        }

        CopyOnWriteArraySet<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            log.debug("用户会话不存在，清理订阅: userId={}, taskId={}", userId, taskId);
            taskUserMap.remove(taskId, userId);
            return;
        }

        // 核心修改：遍历该用户所有的 Session 进行广播
        for (WebSocketSession session : userSessions) {
            sendMessageSafely(session, message);
        }
    }

    public void pushInitialized(String taskId) {
        pushMessage(taskId, UploadMessage.initialized(taskId));
    }

    public void pushChecking(String taskId) {
        pushMessage(taskId, UploadMessage.checking(taskId));
    }

    public void pushQuickUpload(String taskId, String fileId) {
        pushMessage(taskId, UploadMessage.quickUpload(taskId, fileId));
        taskUserMap.remove(taskId);
    }

    public void pushReadyToUpload(String taskId, String uploadId) {
        pushMessage(taskId, UploadMessage.readyToUpload(taskId, uploadId));
    }

    public void pushProgress(String taskId, UploadProgressDTO progress) {
        pushMessage(taskId, UploadMessage.progress(taskId, progress));
    }

    public void pushPaused(String taskId) {
        pushMessage(taskId, UploadMessage.paused(taskId));
    }

    public void pushResumed(String taskId, Set<Integer> uploadedChunks) {
        pushMessage(taskId, UploadMessage.resumed(taskId, uploadedChunks));
    }

    public void pushMerging(String taskId) {
        pushMessage(taskId, UploadMessage.merging(taskId));
    }

    public void pushComplete(String taskId, String fileId) {
        pushMessage(taskId, UploadMessage.complete(taskId, fileId));
        taskUserMap.remove(taskId);
    }

    public void pushCancelling(String taskId) {
        pushMessage(taskId, UploadMessage.cancelling(taskId));
        taskUserMap.remove(taskId);
    }

    public void pushCancelled(String taskId) {
        pushMessage(taskId, UploadMessage.cancelled(taskId));
        taskUserMap.remove(taskId);
    }

    public void pushError(String taskId, String error) {
        pushMessage(taskId, UploadMessage.error(taskId, error));
        taskUserMap.remove(taskId);
    }
}