package com.sleekydz86.finsight.web.dto.editor;

public record EditorImageUploadResponse(
        String imageUrl,
        String originalFileName,
        String storedFileName,
        long size) {
}
