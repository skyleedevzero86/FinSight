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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EditorAssetStorageService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp",
            "image/svg+xml");

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "application/zip",
            "application/x-zip-compressed",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/csv",
            "application/json");

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
        return upload(file, false);
    }

    @Transactional
    public StoredMetadata upload(MultipartFile file, boolean allowNonImage) throws IOException {
        validateUpload(file, allowNonImage);
        if (useMinio()) {
            try {
                return uploadToMinio(file, allowNonImage);
            } catch (Exception e) {
                log.warn("MinIO 업로드 실패, 파일시스템으로 대체합니다: {}", e.getMessage());
            }
        }
        return uploadToFilesystem(file, allowNonImage);
    }

    public LoadedImage load(UUID assetId) throws IOException {
        if (useMinio()) {
            try {
                return loadFromMinio(assetId);
            } catch (ResponseStatusException e) {
                log.debug("MinIO에서 이미지를 찾지 못해 파일시스템을 조회합니다: {}", assetId);
            } catch (Exception e) {
                log.warn("MinIO 이미지 조회 실패, 파일시스템을 조회합니다: {}", e.getMessage());
            }
        }
        return loadFromFilesystem(assetId);
    }

    private boolean useMinio() {
        return editorProperties.getMinio().isEnabled() && minioClientProvider.getIfAvailable() != null;
    }

    private StoredMetadata uploadToMinio(MultipartFile image, boolean allowNonImage) throws IOException {
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        String bucket = editorProperties.getMinio().getBucket();
        ensureBucketExists(minioClient, bucket);

        UUID assetId = UUID.randomUUID();
        String objectKey = buildObjectKey(image.getOriginalFilename(), allowNonImage);
        String contentType = resolveContentType(image, allowNonImage);
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
                } catch (Exception cleanupError) {
                    log.warn("MinIO 업로드 롤백 중 객체 삭제에 실패했습니다: {}", cleanupError.getMessage());
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
            throw new IllegalStateException("MinIO 업로드를 처리하지 못했습니다.", e);
        }
    }

    private LoadedImage loadFromMinio(UUID assetId) {
        EditorAssetJpaEntity asset = editorAssetJpaRepository
                .findById(assetId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."));
        MinioClient minioClient = minioClientProvider.getIfAvailable();
        if (minioClient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.");
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

    private String buildObjectKey(String originalFileName, boolean allowNonImage) {
        LocalDate currentDate = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        String prefix = allowNonImage ? "editor-files" : "editor-images";
        return String.format(
                Locale.ROOT,
                "%s/%d/%02d/%02d/%s%s",
                prefix,
                currentDate.getYear(),
                currentDate.getMonthValue(),
                currentDate.getDayOfMonth(),
                UUID.randomUUID(),
                extractExtension(originalFileName));
    }

    private StoredMetadata uploadToFilesystem(MultipartFile file, boolean allowNonImage) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("파일이 비어 있습니다", List.of("파일이 필요합니다"));
        }
        String contentType = resolveContentType(file, allowNonImage);
        long max = allowNonImage ? editorProperties.getFileMaxBytes() : editorProperties.getImageMaxBytes();
        if (file.getSize() > max) {
            throw new ValidationException("파일 크기가 허용 한도를 초과했습니다", List.of("최대 용량: " + max + "바이트"));
        }
        UUID id = UUID.randomUUID();
        Path dataPath = filesystemRoot.resolve(id + ".bin");
        Path metaPath = filesystemRoot.resolve(id + ".properties");
        Files.write(dataPath, file.getBytes());
        Properties meta = new Properties();
        meta.setProperty("originalFileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        meta.setProperty("contentType", contentType);
        meta.setProperty("size", String.valueOf(file.getSize()));
        try (var out = Files.newOutputStream(metaPath)) {
            meta.store(out, allowNonImage ? "editor-file" : "editor-image");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        Properties meta = new Properties();
        try (var in = Files.newInputStream(metaPath)) {
            meta.load(in);
        }
        String original = meta.getProperty("originalFileName", "file");
        String contentType = meta.getProperty("contentType", "application/octet-stream");
        long size = Long.parseLong(meta.getProperty("size", "0"));
        InputStream stream = Files.newInputStream(dataPath);
        return new LoadedImage(original, contentType, size, stream);
    }

    private void validateUpload(MultipartFile file, boolean allowNonImage) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("업로드할 파일을 선택해주세요.", List.of("파일이 필요합니다"));
        }
        String contentType = resolveContentType(file, allowNonImage);
        long max = allowNonImage ? editorProperties.getFileMaxBytes() : editorProperties.getImageMaxBytes();
        if (file.getSize() > max) {
            throw new ValidationException("파일 크기가 허용 한도를 초과했습니다", List.of("최대 용량: " + max + "바이트"));
        }
        if (allowNonImage) {
            boolean ok = contentType.startsWith("image/")
                    || ALLOWED_FILE_TYPES.contains(contentType)
                    || contentType.equals("application/octet-stream");
            if (!ok) {
                throw new ValidationException("허용되지 않은 파일 형식입니다.", List.of("지원하지 않는 파일 형식입니다"));
            }
            return;
        }
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ValidationException("PNG, JPG, GIF, WEBP, SVG 이미지만 업로드할 수 있습니다.", List.of("지원하지 않는 이미지 형식입니다"));
        }
    }

    private String resolveContentType(MultipartFile file, boolean allowNonImage) {
        String declared = Objects.requireNonNullElse(file.getContentType(), "").toLowerCase(Locale.ROOT).trim();
        if (!declared.isEmpty()
                && !declared.equals("application/octet-stream")
                && (ALLOWED_IMAGE_TYPES.contains(declared) || allowNonImage)) {
            return declared;
        }
        try {
            byte[] header = file.getBytes();
            String sniffed = sniffImageContentType(header);
            if (sniffed != null) {
                return sniffed;
            }
        } catch (IOException e) {
            log.warn("업로드 파일 형식 확인 중 오류가 발생했습니다: {}", e.getMessage());
        }
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return declared.isEmpty() ? "application/octet-stream" : declared;
    }

    private static String sniffImageContentType(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return null;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
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
