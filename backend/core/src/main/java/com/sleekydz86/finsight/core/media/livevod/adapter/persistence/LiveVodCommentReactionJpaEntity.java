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
        name = "live_vod_comment_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email", "comment_id"})
)
public class LiveVodCommentReactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "reaction_type", nullable = false, length = 16)
    private String reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LiveVodCommentReactionJpaEntity() {
    }

    /**
     * Creates a reaction entity for a live VOD comment.
     *
     * @param commentId    the identifier of the commented live VOD
     * @param userEmail    the email address of the reacting user
     * @param reactionType the type of reaction
     */
    public LiveVodCommentReactionJpaEntity(Long commentId, String userEmail, String reactionType) {
        this.commentId = commentId;
        this.userEmail = userEmail;
        this.reactionType = reactionType;
    }

    /**
     * Initializes creation and update timestamps when the entity is persisted.
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
     * Refreshes the update timestamp before the entity is updated.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Retrieves the entity's identifier.
     *
     * @return the entity ID
     */
    public Long getId() {
        return id;
    }

    public Long getCommentId() {
        return commentId;
    }

    /**
     * Gets the email address of the user associated with the reaction.
     *
     * @return the user's email address
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Retrieves the reaction type.
     *
     * @return the reaction type
     */
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
