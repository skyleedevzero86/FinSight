package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_moderation_run")
@EntityListeners(AuditingEntityListener.class)
public class BoardModerationRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "triggered_by", nullable = false, length = 32)
    private String triggeredBy;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "report_threshold", nullable = false)
    private int reportThreshold;

    @Column(name = "hidden_count", nullable = false)
    private int hiddenCount;

    @Lob
    @Column(name = "details_json", columnDefinition = "MEDIUMTEXT")
    private String detailsJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public int getReportThreshold() {
        return reportThreshold;
    }

    public void setReportThreshold(int reportThreshold) {
        this.reportThreshold = reportThreshold;
    }

    public int getHiddenCount() {
        return hiddenCount;
    }

    public void setHiddenCount(int hiddenCount) {
        this.hiddenCount = hiddenCount;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
