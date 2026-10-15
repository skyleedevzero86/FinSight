package com.sleekydz86.finsight.core.news.adapter.crawl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "news_crawl_provider_lock")
public class NewsCrawlLockJpaEntity {

    @Id
    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 255)
    private String owner;

    @Column(nullable = false)
    private long fencingToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
