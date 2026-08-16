package com.sleekydz86.finsight.web.dto.editor;

import java.time.Instant;
import java.util.List;

public record EditorDocumentSummaryResponse(
        String documentId,
        String title,
        String author,
        String introduction,
        String thumbnailImageUrl,
        List<String> tags,
        String status,
        Instant updatedAt) {
}
