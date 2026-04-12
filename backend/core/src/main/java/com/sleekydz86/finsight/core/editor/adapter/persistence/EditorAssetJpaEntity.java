package com.sleekydz86.finsight.core.editor.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "editor_assets",
        indexes = { @Index(name = "idx_editor_assets_uploaded_at", columnList = "uploaded_at") })
public class EditorAssetJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "bucket_name", nullable = false, length = 80)
    private String bucketName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 160)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public EditorAssetJpaEntity() {
    }

    public EditorAssetJpaEntity(
            String id,
            String objectKey,
            String bucketName,
            String originalFileName,
            String contentType,
            long fileSize,
            Instant uploadedAt) {
        this.id = id;
        this.objectKey = objectKey;
        this.bucketName = bucketName;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
