package com.sleekydz86.finsight.web.config;

import com.sleekydz86.finsight.core.editor.config.EditorProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class EditorWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final EditorProperties editorProperties;

    public EditorWebSocketConfig(EditorProperties editorProperties) {
        this.editorProperties = editorProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String raw = editorProperties.getWebsocket().getAllowedOriginPatterns();
        String[] patterns = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (patterns.length == 0) {
            patterns = new String[] { "http://localhost:*", "http://127.0.0.1:*" };
        }
        registry.addEndpoint("/ws-editor").setAllowedOriginPatterns(patterns).withSockJS();
    }
}
