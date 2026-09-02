package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "live_vod_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email", "video_id"})
)
public class LiveVodReactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 32)
    private String videoId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "reaction_type", nullable = false, length = 16)
    private String reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LiveVodReactionJpaEntity() {
    }

    /**
     * Creates a live VOD reaction entity.
     *
     * @param videoId      the identifier of the video
     * @param userEmail    the email address of the reacting user
     * @param reactionType the type of reaction
     */
    public LiveVodReactionJpaEntity(String videoId, String userEmail, String reactionType) {
        this.videoId = videoId;
        this.userEmail = userEmail;
        this.reactionType = reactionType;
    }

    /**
     * Initializes the creation timestamp when absent and updates the modification timestamp before persistence.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    /**
     * Refreshes the last-update timestamp before the entity is updated.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    /**
     * Retrieves the identifier of the associated video.
     *
     * @return the video identifier
     */
    public String getVideoId() {
        return videoId;
    }

    /**
     * Retrieves the email address associated with the reaction.
     *
     * @return the user's email address
     */
    public String getUserEmail() {
        return userEmail;
    }

    public String getReactionType() {
        return reactionType;
    }

    /**
     * Updates the reaction type.
     *
     * @param reactionType the new reaction type
     */
    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}
