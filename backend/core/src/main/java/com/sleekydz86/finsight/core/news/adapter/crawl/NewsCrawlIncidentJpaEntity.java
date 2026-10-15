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

    private Integer httpStatus;

    private Integer retryAfterSeconds;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean retryable;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private LocalDateTime analyzedAt;

    @Column(length = 1000)
    private String modelNote;

    @Column(length = 255)
    private String approvedBy;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private long fencingToken;
}
