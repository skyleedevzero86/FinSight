package com.sleekydz86.finsight.core.search.adapter.persistence;

import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaEntity;
import com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command.YoutubeVideoMetaJpaRepository;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {NewsJpaEntity.class, YoutubeVideoMetaJpaEntity.class})
@EnableJpaRepositories(basePackageClasses = {NewsJpaRepository.class, YoutubeVideoMetaJpaRepository.class})
class CatalogSearchMysqlTestConfig {
}
