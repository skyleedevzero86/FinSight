package com.sleekydz86.finsight.core.news.adapter.crawl;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlCause;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "news_crawl_incident")
public class NewsCrawlIncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NewsCrawlIncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NewsCrawlCause cause;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "retry_after_seconds")
    private Integer retryAfterSeconds;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean retryable;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "model_note", length = 1000)
    private String modelNote;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;
}
