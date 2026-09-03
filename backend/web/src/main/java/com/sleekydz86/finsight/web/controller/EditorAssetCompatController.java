package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService;
import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService.LoadedImage;
import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService.StoredMetadata;
import com.sleekydz86.finsight.web.dto.editor.EditorImageUploadResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/editor/images")
public class EditorAssetCompatController {

    private final EditorAssetStorageService editorAssetStorageService;

    public EditorAssetCompatController(EditorAssetStorageService editorAssetStorageService) {
        this.editorAssetStorageService = editorAssetStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EditorImageUploadResponse uploadImage(
            @RequestPart("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean allowFile)
            throws IOException {
        StoredMetadata meta = editorAssetStorageService.upload(file, allowFile);
        return new EditorImageUploadResponse(
                meta.imageUrl(), meta.originalFileName(), meta.storedFileName(), meta.size());
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<InputStreamResource> loadImage(@PathVariable UUID assetId) throws IOException {
        LoadedImage image = editorAssetStorageService.load(assetId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(image.originalFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.size())
                .body(image.asResource());
    }
}
