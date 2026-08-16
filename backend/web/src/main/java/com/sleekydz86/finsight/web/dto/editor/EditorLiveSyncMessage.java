package com.sleekydz86.finsight.web.dto.editor;

import java.util.List;

public record EditorLiveSyncMessage(
        String sessionId,
        String documentId,
        String title,
        String author,
        String markdown,
        List<String> tags,
        String status,
        String updatedAt) {
}
