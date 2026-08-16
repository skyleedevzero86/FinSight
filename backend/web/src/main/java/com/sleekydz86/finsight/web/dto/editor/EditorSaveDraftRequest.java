package com.sleekydz86.finsight.web.dto.editor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EditorSaveDraftRequest(
        String documentId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 60) String author,
        @NotBlank String markdown,
        List<@NotBlank @Size(max = 24) String> tags,
        @Size(max = 180) String urlSlug,
        @Size(max = 150) String introduction,
        @Size(max = 500) String thumbnailImageUrl,
        String status) {
}
