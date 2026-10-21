package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PortfolioLlamaNarratorTest {

    @Test
    @DisplayName("올람라가 꺼져 있으면 규칙 문구를 유지한다")
    void keepsRuleTextWhenOllamaIsOff() {
        // given
        OllamaProperties properties = new OllamaProperties();

        // when
        properties.setEnabled(false);

        // given
        PortfolioLlamaNarrator narrator = new PortfolioLlamaNarrator(mock(WebClient.class), properties, new ObjectMapper());
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(
                List.of(new PortfolioHolding(
                        1L, "예금", PortfolioAssetKind.ASSET, PortfolioAssetCategory.DEPOSIT, 10_000_000L, null, null)),
                List.of(),
                YearMonth.of(2026, 10),
                false);

        // when
        PortfolioDiagnosis diagnosis = narrator.narrate(summary);

        // then
        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.RULE);
        assertThat(diagnosis.advice()).isEqualTo(summary.advice());
        assertThat(diagnosis.insights()).isEqualTo(summary.insights());
    }

    @Test
    @DisplayName("자산이 없으면 라마를 호출하지 않는다")
    void doesNotCallLlamaWithoutAssets() {
        // given
        OllamaProperties properties = new OllamaProperties();

        // when
        properties.setEnabled(true);
        properties.setPortfolioModel("llama2");

        // given
        PortfolioLlamaNarrator narrator = new PortfolioLlamaNarrator(mock(WebClient.class), properties, new ObjectMapper());
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(), List.of(), YearMonth.of(2026, 10), false);

        // when
        PortfolioDiagnosis diagnosis = narrator.narrate(summary);

        // then
        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.RULE);
        assertThat(diagnosis.advice()).isEqualTo("자산을 등록하면 순자산과 구성이 계산됩니다.");
    }
}
