package com.xddcodec.fs.fs.framework.ws;

import com.xddcodec.fs.fs.framework.ws.handler.UploadWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * websocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketAutoConfigure implements WebSocketConfigurer {

    @Autowired
    private UploadWebSocketHandler uploadWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(uploadWebSocketHandler, "/ws/upload")
                .setAllowedOrigins("*"); // 允许跨域访问
    }
}
