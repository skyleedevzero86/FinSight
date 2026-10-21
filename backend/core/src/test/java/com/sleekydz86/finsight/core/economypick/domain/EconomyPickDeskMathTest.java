package com.sleekydz86.finsight.core.economypick.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickDeskMathTest {

    @Test
    @DisplayName("기간은 야후 간격으로 바뀐다")
    void periodMapsToYahooInterval() {
        // given

        // when

        // then
        assertThat(ChartWindow.of("15분").interval()).isEqualTo("15m");
        assertThat(ChartWindow.of("3분").label()).isEqualTo("2분");
        assertThat(ChartWindow.of("일").range()).isEqualTo("3mo");
        assertThat(ChartWindow.yahooSymbol("005930", "코스피")).isEqualTo("005930.KS");
        assertThat(ChartWindow.yahooSymbol("196170", "코스닥")).isEqualTo("196170.KQ");
    }

    @Test
    @DisplayName("규칙 추천은 매출 구성을 문장으로 만든다")
    void ruleRecommendationBuildsRevenueSentence() {
        // given
        EconomyPickBrief brief = BriefMath.rules("버진 갤럭틱", "SPCE");

        // when

        // then
        assertThat(brief.source()).isEqualTo("RULE");
        assertThat(brief.headline()).contains("버진 갤럭틱");
        assertThat(brief.points()).isNotEmpty();
    }

    @Test
    @DisplayName("라마 응답에서 추천 문장을 읽는다")
    void readsRecommendationSentenceFromLlamaResponse() {
        // given
        String raw = "설명 {\"headline\":\"우주 비중\",\"body\":\"발사 서비스가 중심입니다.\",\"points\":[\"우주관광\",\"발사\"]} 끝";
        EconomyPickBrief brief = BriefMath.ollama(raw, new ObjectMapper());

        // when

        // then
        assertThat(brief.source()).isEqualTo("OLLAMA");
        assertThat(brief.points()).containsExactly("우주관광", "발사");
    }
}
