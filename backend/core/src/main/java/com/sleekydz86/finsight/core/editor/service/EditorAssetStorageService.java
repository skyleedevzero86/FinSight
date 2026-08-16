package com.sleekydz86.finsight.core.editor.service;

import com.sleekydz86.finsight.core.editor.adapter.persistence.EditorAssetJpaEntity;
import com.sleekydz86.finsight.core.editor.adapter.persistence.EditorAssetJpaRepository;
import com.sleekydz86.finsight.core.editor.config.EditorProperties;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Service
public class EditorAssetStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp",
            "image/svg+xml");

    private final ObjectProvider<MinioClient> minioClientProvider;
    private final EditorProperties editorProperties;
    private final EditorAssetJpaRepository editorAssetJpaRepository;
    private final Clock clock = Clock.systemUTC();
    private Path filesystemRoot;

    public EditorAssetStorageService(
            ObjectProvider<MinioClient> minioClientProvider,
            EditorProperties editorProperties,
            EditorAssetJpaRepository editorAssetJpaRepository) {
        this.minioClientProvider = minioClientProvider;
        this.editorProperties = editorProperties;
        this.editorAssetJpaRepository = editorAssetJpaRepository;
    }

    @PostConstruct
    void initFilesystem() throws IOException {
        String configured = editorProperties.getImageStorageDir();
        if (configured == null || configured.isBlank()) {
            filesystemRoot = Path.of(System.getProperty("java.io.tmpdir"), "finsight-editor-images");
        } else {
            filesystemRoot = Path.of(configured);
        }
        Files.createDirectories(filesystemRoot);
    }

    @Transactional
    public StoredMetadata upload(MultipartFile file) throws IOException {
        if (useMinio()) {
            return uploadToMinio(file);
        }
        return uploadToFilesystem(file);
    }

    public LoadedImage load(UUID assetId) throws IOException {
        if (useMinio()) {
            return loadFromMinio(assetId);
        }
        return loadFromFilesystem(assetId);
    }

    private boolean useMinio() {
        return editorProperties.getMinio().isEnabled() && minioClientProvider.getIfAvailable() != null;
    }

    private StoredMetadata uploadToMinio(MultipartFile image) throws IOException {
        validateImage(image);
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        String bucket = editorProperties.getMinio().getBucket();
        ensureBucketExists(minioClient, bucket);

        UUID assetId = UUID.randomUUID();
        String objectKey = buildObjectKey(image.getOriginalFilename());
        String contentType = Objects.requireNonNullElse(image.getContentType(), "application/octet-stream");
        String originalName = Objects.requireNonNullElse(image.getOriginalFilename(), objectKey);

        try (InputStream inputStream = image.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, image.getSize(), -1)
                    .contentType(contentType)
                    .build());

            Instant uploadedAt = clock.instant();
            try {
                editorAssetJpaRepository.save(new EditorAssetJpaEntity(
                        assetId.toString(),
                        objectKey,
                        bucket,
                        originalName,
                        contentType,
                        image.getSize(),
                        uploadedAt));
            } catch (RuntimeException e) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
                } catch (Exception ignored) {
                }
                throw e;
            }

            return new StoredMetadata(
                    "/api/editor/images/" + assetId,
                    originalName,
                    objectKey,
                    image.getSize());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 이미지 업로드를 처리하지 못했습니다.", e);
        }
    }

    private LoadedImage loadFromMinio(UUID assetId) {
        EditorAssetJpaEntity asset = editorAssetJpaRepository
                .findById(assetId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "image not found"));
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        if (minioClient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image not found");
        }
        try {
            GetObjectResponse stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(asset.getBucketName())
                    .object(asset.getObjectKey())
                    .build());
            return new LoadedImage(asset.getOriginalFileName(), asset.getContentType(), asset.getFileSize(), stream);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 이미지 조회를 처리하지 못했습니다.", e);
        }
    }

    private void ensureBucketExists(MinioClient minioClient, String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 버킷 준비에 실패했습니다.", e);
        }
    }

    private String buildObjectKey(String originalFileName) {
        LocalDate currentDate = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return String.format(
                Locale.ROOT,
                "editor-images/%d/%02d/%02d/%s%s",
                currentDate.getYear(),
                currentDate.getMonthValue(),
                currentDate.getDayOfMonth(),
                UUID.randomUUID(),
                extractExtension(originalFileName));
    }

    private StoredMetadata uploadToFilesystem(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("이미지 파일이 비어 있습니다", List.of("file required"));
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ValidationException("이미지 파일만 업로드할 수 있습니다", List.of("contentType must be image/*"));
        }
        long max = editorProperties.getImageMaxBytes();
        if (file.getSize() > max) {
            throw new ValidationException("이미지 크기가 허용 한도를 초과했습니다", List.of("max bytes: " + max));
        }
        UUID id = UUID.randomUUID();
        Path dataPath = filesystemRoot.resolve(id + ".bin");
        Path metaPath = filesystemRoot.resolve(id + ".properties");
        Files.write(dataPath, file.getBytes());
        Properties meta = new Properties();
        meta.setProperty("originalFileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        meta.setProperty("contentType", contentType);
        meta.setProperty("size", String.valueOf(file.getSize()));
        try (var out = Files.newOutputStream(metaPath)) {
            meta.store(out, "editor-image");
        }
        String stored = id + ".bin";
        String imageUrl = "/api/editor/images/" + id;
        return new StoredMetadata(
                imageUrl,
                meta.getProperty("originalFileName"),
                stored,
                file.getSize());
    }

    private LoadedImage loadFromFilesystem(UUID id) throws IOException {
        Path dataPath = filesystemRoot.resolve(id + ".bin");
        Path metaPath = filesystemRoot.resolve(id + ".properties");
        if (!Files.isRegularFile(dataPath) || !Files.isRegularFile(metaPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image not found");
        }
        Properties meta = new Properties();
        try (var in = Files.newInputStream(metaPath)) {
            meta.load(in);
        }
        String original = meta.getProperty("originalFileName", "image");
        String contentType = meta.getProperty("contentType", "application/octet-stream");
        long size = Long.parseLong(meta.getProperty("size", "0"));
        InputStream stream = Files.newInputStream(dataPath);
        return new LoadedImage(original, contentType, size, stream);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ValidationException("업로드할 이미지 파일을 선택해주세요.", List.of("file required"));
        }
        String contentType = Objects.requireNonNullElse(image.getContentType(), "").toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ValidationException("PNG, JPG, GIF, WEBP, SVG 이미지만 업로드할 수 있습니다.", List.of("invalid content type"));
        }
    }

    private static String extractExtension(String fileName) {
        String safeName = Objects.requireNonNullElse(fileName, "");
        int extensionIndex = safeName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "";
        }
        return safeName.substring(extensionIndex);
    }

    public record StoredMetadata(String imageUrl, String originalFileName, String storedFileName, long size) {
    }

    public record LoadedImage(String originalFileName, String contentType, long size, InputStream stream) {

        public InputStreamResource asResource() {
            return new InputStreamResource(stream);
        }
    }
}
