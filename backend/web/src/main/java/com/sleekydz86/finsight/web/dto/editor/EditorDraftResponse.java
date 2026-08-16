package com.sleekydz86.finsight.web.dto.editor;

import java.time.Instant;
import java.util.List;

public record EditorDraftResponse(
        String documentId,
        String title,
        String author,
        String markdown,
        List<String> tags,
        String urlSlug,
        String introduction,
        String thumbnailImageUrl,
        String renderedHtml,
        String plainTextPreview,
        String status,
        Instant updatedAt,
        EditorDocumentSummaryResponse previousDocument,
        EditorDocumentSummaryResponse nextDocument) {
}
