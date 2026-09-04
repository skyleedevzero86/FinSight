package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.web.dto.editor.EditorLiveSyncMessage;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Hidden
@Controller
public class EditorRealtimeCompatController {

    @MessageMapping("/editor/live")
    @SendTo("/topic/editor.live")
    public EditorLiveSyncMessage broadcast(EditorLiveSyncMessage message) {
        List<String> normalizedTags = Objects.requireNonNullElse(message.tags(), List.<String>of()).stream()
                .map(String::trim)
                .map(tag -> tag.replace("#", "").replace(",", ""))
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
        String sessionId = normalize(message.sessionId(), "anonymous-session");
        String documentId = normalize(message.documentId(), UUID.randomUUID().toString());
        String title = trimToEmpty(message.title());
        String author = trimToEmpty(message.author());
        String markdown = Objects.requireNonNullElse(message.markdown(), "");
        String status = normalizeStatus(message.status());
        return new EditorLiveSyncMessage(
                sessionId, documentId, title, author, markdown, normalizedTags, status, Instant.now().toString());
    }

    private static String normalize(String value, String fallback) {
        String t = trimToEmpty(value);
        return t.isEmpty() ? fallback : t;
    }

    private static String normalizeStatus(String raw) {
        String s = trimToEmpty(raw).toLowerCase();
        if ("published".equals(s) || "draft".equals(s)) {
            return s;
        }
        if (s.isEmpty()) {
            return "draft";
        }
        return s;
    }

    private static String trimToEmpty(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
