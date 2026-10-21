package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentStatus;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class NewsSentimentRecorderTest {

    @Test
    @DisplayName("DJL 결과는 요약을 바꾸지 않는다")
    void djlResultDoesNotReplaceSummary() {
        // given
        NewsJpaEntity news = news();

        // when
        news.setOverview("기존 요약");
        NewsSentimentRecorder.applyDjl(news, DjlSentimentResult.builder()
                .label("POSITIVE")
                .score(0.6)
                .confidence(0.8)
                .positiveProbability(0.7)
                .neutralProbability(0.2)
                .negativeProbability(0.1)
                .success(true)
                .modelName("cardiffnlp/twitter-roberta-base-sentiment-latest")
                .build());

        // then
        assertThat(news.getOverview()).isEqualTo("기존 요약");
        assertThat(news.getSentimentStatus()).isEqualTo(SentimentStatus.DONE);
        assertThat(news.getSentimentType()).isEqualTo(SentimentType.POSITIVE);
        assertThat(news.getSentimentScore()).isEqualTo(0.6);
        assertThat(news.getSentimentConfidence()).isEqualTo(0.8);
        assertThat(news.getSentimentPositive()).isEqualTo(0.7);
        assertThat(news.getSentimentModel()).isEqualTo("cardiffnlp/twitter-roberta-base-sentiment-latest");
        assertThat(news.getSentimentAttempts()).isZero();
    }

    @Test
    @DisplayName("키워드 폴백은 요약을 유지하고 시도 횟수를 센다")
    void keywordFallbackKeepsSummaryAndCountsAttempt() {
        // given
        NewsJpaEntity news = news();

        // when
        news.setOverview("기존 요약");
        NewsSentimentRecorder.applyKeyword(news, SentimentAnalysisResult.success(
                SentimentType.NEGATIVE, 0.2, 3L, "키워드 감성"));

        // then
        assertThat(news.getOverview()).isEqualTo("기존 요약");
        assertThat(news.getSentimentStatus()).isEqualTo(SentimentStatus.KEYWORD);
        assertThat(news.getSentimentType()).isEqualTo(SentimentType.NEGATIVE);
        assertThat(news.getSentimentAttempts()).isEqualTo(1);
        assertThat(news.getSentimentPositive()).isNull();
    }

    @Test
    @DisplayName("실패하면 요약을 지우지 않고 시도 횟수만 올린다")
    void failureIncrementsAttemptWithoutClearingSummary() {
        // given
        NewsJpaEntity news = news();

        // when
        news.setOverview("기존 요약");
        NewsSentimentRecorder.markFailed(news);

        // then
        assertThat(news.getOverview()).isEqualTo("기존 요약");
        assertThat(news.getSentimentStatus()).isEqualTo(SentimentStatus.FAILED);
        assertThat(news.getSentimentAttempts()).isEqualTo(1);
    }

    private NewsJpaEntity news() {
        NewsJpaEntity news = new NewsJpaEntity();
        news.setId(1L);
        news.setOriginalTitle("제목");
        news.setOriginalContent("본문");
        return news;
    }
}
