package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "youtube_video_meta")
public class YoutubeVideoMetaJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false, unique = true)
    private Long boardId;

    @Column(name = "video_id", nullable = false, unique = true, length = 50)
    private String videoId;

    @Column(name = "channel_id", length = 100)
    private String channelId;

    @Column(name = "channel_title", length = 300)
    private String channelTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private YoutubeImportSourceType sourceType;

    @Column(name = "source_value", length = 500)
    private String sourceValue;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "youtube_title", nullable = false, length = 500)
    private String youtubeTitle;

    @Column(name = "youtube_description", columnDefinition = "TEXT")
    private String youtubeDescription;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "duration", length = 50)
    private String duration;

    @Column(name = "embed_url", length = 1000)
    private String embedUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "editor_comment", columnDefinition = "TEXT")
    private String editorComment;

    @ElementCollection
    @CollectionTable(name = "youtube_video_meta_key_points", joinColumns = @JoinColumn(name = "video_meta_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "key_point", length = 500)
    private List<String> keyPoints = new ArrayList<>();

    @Column(name = "ai_generated_at")
    private LocalDateTime aiGeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 30)
    private YoutubeImportStatus importStatus;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
