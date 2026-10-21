package com.sleekydz86.finsight.core.news.adapter.crawl;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlAttemptStatus;
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
@Table(name = "news_crawl_attempt")
public class NewsCrawlAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NewsCrawlAttemptStatus status;

    @Column(nullable = false, length = 255)
    private String executor;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "result_message", nullable = false, length = 500)
    private String resultMessage;
}
