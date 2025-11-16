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

@Component
@Slf4j
public class UploadWebSocketHandler extends TextWebSocketHandler {

    // 存储 userId -> WebSocketSession 映射
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储 taskId -> userId 映射
    private final ConcurrentHashMap<String, String> taskUserMap = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        if (userId != null) {
            // 移除旧连接（如果存在）
            WebSocketSession oldSession = sessions.put(userId, session);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close();
                } catch (Exception e) {
                    log.warn("关闭旧 WebSocket 连接失败: userId={}", userId, e);
                }
            }

            log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());

            // 发送连接成功消息
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
            // 只移除当前 session（防止误删新连接）
            sessions.remove(userId, session);

            // 清理该用户的所有任务订阅
            taskUserMap.entrySet().removeIf(entry -> entry.getValue().equals(userId));

            log.info("WebSocket 连接关闭: userId={}, status={}", userId, status);
        }
    }

    /**
     * 传输错误处理
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String userId = getUserId(session);
        log.error("WebSocket 传输错误: userId={}, sessionId={}", userId, session.getId(), exception);

        // 清理会话
        if (userId != null) {
            sessions.remove(userId, session);
        }

        // 尝试关闭连接
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.warn("关闭异常 WebSocket 连接失败", e);
        }
    }

    // ========== 私有方法 ==========

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
     * 安全发送消息（捕获所有异常）
     */
    private boolean sendMessageSafely(WebSocketSession session, UploadMessage message) {
        if (session == null || !session.isOpen()) {
            log.debug("会话未打开，跳过发送: sessionId={}", session != null ? session.getId() : "null");
            return false;
        }

        try {
            synchronized (session) {
                if (!session.isOpen()) {
                    log.debug("会话已关闭，跳过发送: sessionId={}", session.getId());
                    return false;
                }

                String json = JsonUtils.toJsonString(message);
                session.sendMessage(new TextMessage(Objects.requireNonNull(json)));
                return true;
            }
        } catch (ClosedChannelException e) {
            log.debug("WebSocket 通道已关闭: sessionId={}", session.getId());
            return false;
        } catch (IOException e) {
            log.warn("发送 WebSocket 消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("发送 WebSocket 消息异常: sessionId={}", session.getId(), e);
            return false;
        }
    }

    /**
     * 通用推送方法
     */
    private void pushMessage(String taskId, UploadMessage message) {
        String userId = taskUserMap.get(taskId);
        if (userId == null) {
            log.debug("任务未订阅，跳过推送: taskId={}, type={}", taskId, message.getType());
            return;
        }

        WebSocketSession session = sessions.get(userId);
        if (session == null) {
            log.debug("用户会话不存在，跳过推送: userId={}, taskId={}", userId, taskId);
            taskUserMap.remove(taskId, userId);
            return;
        }

        boolean success = sendMessageSafely(session, message);
        if (!success) {
            log.warn("推送消息失败: userId={}, taskId={}, type={}", userId, taskId, message.getType());
            sessions.remove(userId, session);
            taskUserMap.remove(taskId, userId);
        }
    }

    // ========== 公共推送方法 ==========

    /**
     * 推送初始化成功消息
     */
    public void pushInitialized(String taskId) {
        pushMessage(taskId, UploadMessage.initialized(taskId));
        log.debug("推送初始化消息: taskId={}", taskId);
    }

    /**
     * 推送检查中消息
     */
    public void pushChecking(String taskId) {
        pushMessage(taskId, UploadMessage.checking(taskId));
        log.debug("推送检查中消息: taskId={}", taskId);
    }

    /**
     * 推送秒传成功消息
     */
    public void pushQuickUpload(String taskId, String fileId) {
        pushMessage(taskId, UploadMessage.quickUpload(taskId, fileId));
        // 秒传成功后清理订阅
        taskUserMap.remove(taskId);
        log.info("推送秒传成功消息: taskId={}, fileId={}", taskId, fileId);
    }

    /**
     * 推送准备上传消息
     */
    public void pushReadyToUpload(String taskId, String uploadId) {
        pushMessage(taskId, UploadMessage.readyToUpload(taskId, uploadId));
        log.debug("推送准备上传消息: taskId={}, uploadId={}", taskId, uploadId);
    }

    /**
     * 推送上传进度
     */
    public void pushProgress(String taskId, UploadProgressDTO progress) {
        pushMessage(taskId, UploadMessage.progress(taskId, progress));
    }

    /**
     * 推送暂停消息
     */
    public void pushPaused(String taskId) {
        pushMessage(taskId, UploadMessage.paused(taskId));
        log.info("推送暂停消息: taskId={}", taskId);
    }

    /**
     * 推送继续消息
     */
    public void pushResumed(String taskId, Set<Integer> uploadedChunks) {
        pushMessage(taskId, UploadMessage.resumed(taskId, uploadedChunks));
        log.info("推送继续消息: taskId={}, uploadedChunks={}", taskId, uploadedChunks.size());
    }

    /**
     * 推送合并中消息
     */
    public void pushMerging(String taskId) {
        pushMessage(taskId, UploadMessage.merging(taskId));
        log.info("推送合并中消息: taskId={}", taskId);
    }

    /**
     * 推送完成消息
     */
    public void pushComplete(String taskId, String fileId) {
        pushMessage(taskId, UploadMessage.complete(taskId, fileId));
        // 任务完成后清理订阅
        taskUserMap.remove(taskId);
        log.info("推送完成消息: taskId={}, fileId={}", taskId, fileId);
    }

    /**
     * 推送正在取消消息
     */
    public void pushCancelling(String taskId) {
        pushMessage(taskId, UploadMessage.cancelling(taskId));
        // 任务取消后清理订阅
        taskUserMap.remove(taskId);
        log.info("推送正在取消消息: taskId={}", taskId);
    }

    /**
     * 推送取消消息
     */
    public void pushCancelled(String taskId) {
        pushMessage(taskId, UploadMessage.cancelled(taskId));
        // 任务取消后清理订阅
        taskUserMap.remove(taskId);
        log.info("推送取消消息: taskId={}", taskId);
    }

    /**
     * 推送错误消息
     */
    public void pushError(String taskId, String error) {
        pushMessage(taskId, UploadMessage.error(taskId, error));
        // 错误发生后清理订阅
        taskUserMap.remove(taskId);
        log.error("推送错误消息: taskId={}, error={}", taskId, error);
    }
}
