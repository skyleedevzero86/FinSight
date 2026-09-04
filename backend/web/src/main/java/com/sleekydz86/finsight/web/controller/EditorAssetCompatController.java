package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService;
import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService.LoadedImage;
import com.sleekydz86.finsight.core.editor.service.EditorAssetStorageService.StoredMetadata;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.web.dto.editor.EditorImageUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/editor/images")
public class EditorAssetCompatController {

    private static final Logger log = LoggerFactory.getLogger(EditorAssetCompatController.class);

    private final EditorAssetStorageService editorAssetStorageService;

    public EditorAssetCompatController(EditorAssetStorageService editorAssetStorageService) {
        this.editorAssetStorageService = editorAssetStorageService;
    }

    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean allowFile) {
        try {
            StoredMetadata meta = editorAssetStorageService.upload(file, allowFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new EditorImageUploadResponse(
                            meta.imageUrl(),
                            meta.originalFileName(),
                            meta.storedFileName(),
                            meta.size()));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (ResponseStatusException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_REQUEST;
            }
            String message = e.getReason() != null ? e.getReason() : "요청을 처리할 수 없습니다.";
            return ResponseEntity.status(status).body(errorBody(message));
        } catch (IllegalStateException e) {
            log.warn("에디터 업로드 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody(e.getMessage() != null ? e.getMessage() : "파일 업로드에 실패했습니다."));
        } catch (Exception e) {
            log.error("에디터 업로드 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("파일 업로드 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<?> loadImage(@PathVariable UUID assetId) {
        try {
            LoadedImage image = editorAssetStorageService.load(assetId);
            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(image.originalFileName(), StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(MediaType.parseMediaType(image.contentType()))
                    .contentLength(image.size())
                    .body(image.asResource());
        } catch (ResponseStatusException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.NOT_FOUND;
            }
            String message = e.getReason() != null ? e.getReason() : "파일을 찾을 수 없습니다.";
            return ResponseEntity.status(status).body(errorBody(message));
        } catch (Exception e) {
            log.error("에디터 파일 조회 중 오류: assetId={}", assetId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("파일을 불러오는 중 오류가 발생했습니다."));
        }
    }

    private static Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        return body;
    }
}
