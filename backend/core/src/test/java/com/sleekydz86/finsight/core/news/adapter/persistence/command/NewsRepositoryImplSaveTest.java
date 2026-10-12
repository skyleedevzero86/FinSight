package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsRepositoryImplSaveTest {

    @Mock
    private NewsJpaRepository newsJpaRepository;

    @Test
    void 유니크_위반_한_건은_건너뛰고_나머지는_저장한다() {
        when(newsJpaRepository.findSourceUrlBySourceUrlIn(any())).thenReturn(List.of());
        when(newsJpaRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            NewsJpaEntity entity = invocation.getArgument(0);
            if (entity.getSourceUrl().contains("dup")) {
                throw new DataIntegrityViolationException("uk_news_source_url");
            }
            entity.setId(11L);
            return entity;
        });
        NewsRepositoryImpl repository = new NewsRepositoryImpl(
                new NewsJpaMapper(),
                newsJpaRepository,
                null,
                new ImmediateTransactionManager());

        var saved = repository.saveAllNews(List.of(
                news("https://news.example/ok"),
                news("https://news.example/dup")));

        assertThat(saved.getNewses()).extracting(item -> item.getNewsMeta().getSourceUrl())
                .containsExactly("https://news.example/ok");
    }

    @Test
    void 키워드가_다르면_조회_조건이_달라진다() {
        QNewsJpaEntity news = QNewsJpaEntity.newsJpaEntity;
        String kospi = NewsListQuery.filters(news, query("코스피")).toString();
        String nasdaq = NewsListQuery.filters(news, query("나스닥")).toString();

        assertThat(kospi).contains("코스피");
        assertThat(nasdaq).contains("나스닥");
        assertThat(kospi).isNotEqualTo(nasdaq);
        NewsQueryRequest blank = new NewsQueryRequest(null, null, null, "  ", null, null);
        assertThat(NewsListQuery.filters(news, blank).hasValue()).isFalse();
    }

    private static NewsQueryRequest query(String keyword) {
        return new NewsQueryRequest(null, null, null, keyword, List.of(TargetCategory.BTC), null);
    }

    private static News news(String url) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, url),
                new Content("제목", "본문"));
    }

    private static final class ImmediateTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
