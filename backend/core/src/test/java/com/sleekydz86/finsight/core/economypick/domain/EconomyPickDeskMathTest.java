package com.sleekydz86.finsight.core.economypick.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickDeskMathTest {

    @Test
    void 기간은_야후_간격으로_바뀐다() {
        assertThat(ChartWindow.of("15분").interval()).isEqualTo("15m");
        assertThat(ChartWindow.of("3분").label()).isEqualTo("2분");
        assertThat(ChartWindow.of("일").range()).isEqualTo("3mo");
        assertThat(ChartWindow.yahooSymbol("005930", "코스피")).isEqualTo("005930.KS");
        assertThat(ChartWindow.yahooSymbol("196170", "코스닥")).isEqualTo("196170.KQ");
    }

    @Test
    void 규칙_추천은_매출_구성을_문장으로_만든다() {
        EconomyPickBrief brief = BriefMath.rules("버진 갤럭틱", "SPCE");
        assertThat(brief.source()).isEqualTo("RULE");
        assertThat(brief.headline()).contains("버진 갤럭틱");
        assertThat(brief.points()).isNotEmpty();
    }

    @Test
    void 라마_응답에서_추천_문장을_읽는다() {
        String raw = "설명 {\"headline\":\"우주 비중\",\"body\":\"발사 서비스가 중심입니다.\",\"points\":[\"우주관광\",\"발사\"]} 끝";
        EconomyPickBrief brief = BriefMath.ollama(raw, new ObjectMapper());
        assertThat(brief.source()).isEqualTo("OLLAMA");
        assertThat(brief.points()).containsExactly("우주관광", "발사");
    }
}
