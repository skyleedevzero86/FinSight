package com.sleekydz86.finsight.web.dto.editor;

import java.util.List;

public record EditorBootstrapResponse(
        EditorDraftResponse draft,
        List<String> suggestedTags,
        String referenceImageUrl) {
}
