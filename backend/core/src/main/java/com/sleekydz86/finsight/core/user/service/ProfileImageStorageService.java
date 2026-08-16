package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProfileImageStorageService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif");
    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp",
            "image/gif", "gif");
    private static final Map<String, String> TYPE_BY_EXT = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "gif", "image/gif");

    @Value("${finsight.profile-image.dir:}")
    private String configuredDir;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        if (configuredDir == null || configuredDir.isBlank()) {
            root = Path.of(System.getProperty("java.io.tmpdir"), "finsight-profile-images");
        } else {
            root = Path.of(configuredDir);
        }
        Files.createDirectories(root);
    }

    public String store(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("프로필 사진을 선택해 주세요", List.of("IMAGE_REQUIRED"));
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ValidationException("프로필 사진은 2MB 이하여야 합니다", List.of("IMAGE_TOO_LARGE"));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException("PNG, JPG, WEBP, GIF 이미지만 업로드할 수 있습니다", List.of("IMAGE_TYPE_INVALID"));
        }
        String ext = EXT_BY_TYPE.getOrDefault(contentType, "jpg");
        deleteExisting(userId);
        Path target = root.resolve(userId + "." + ext);
        try (java.io.InputStream in = file.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return publicUrl(userId);
    }

    public Optional<StoredProfileImage> load(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, userId + ".*")) {
            for (Path path : stream) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = path.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "jpg";
                String type = TYPE_BY_EXT.getOrDefault(ext, "application/octet-stream");
                return Optional.of(new StoredProfileImage(path, type));
            }
        } catch (IOException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public String publicUrl(Long userId) {
        return "/api/v1/users/avatars/" + userId;
    }

    private void deleteExisting(Long userId) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, userId + ".*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }
}
