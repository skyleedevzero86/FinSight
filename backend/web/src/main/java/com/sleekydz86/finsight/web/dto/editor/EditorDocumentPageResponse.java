package com.sleekydz86.finsight.web.dto.editor;

import java.util.List;

public record EditorDocumentPageResponse(
        List<EditorDocumentSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String query,
        String statusFilter) {
}
