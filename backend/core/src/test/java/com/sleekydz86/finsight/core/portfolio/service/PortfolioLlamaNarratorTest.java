package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioHolding;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PortfolioLlamaNarratorTest {

    @Test
    void 올람라가_꺼져_있으면_규칙_문구를_유지한다() {
        OllamaProperties properties = new OllamaProperties();
        properties.setEnabled(false);
        PortfolioLlamaNarrator narrator = new PortfolioLlamaNarrator(mock(WebClient.class), properties, new ObjectMapper());
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(
                List.of(new PortfolioHolding(
                        1L, "예금", PortfolioAssetKind.ASSET, PortfolioAssetCategory.DEPOSIT, 10_000_000L, null, null)),
                List.of(),
                YearMonth.of(2026, 10),
                false);

        PortfolioDiagnosis diagnosis = narrator.narrate(summary);

        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.RULE);
        assertThat(diagnosis.advice()).isEqualTo(summary.advice());
        assertThat(diagnosis.insights()).isEqualTo(summary.insights());
    }

    @Test
    void 자산이_없으면_라마를_호출하지_않는다() {
        OllamaProperties properties = new OllamaProperties();
        properties.setEnabled(true);
        properties.setPortfolioModel("llama2");
        PortfolioLlamaNarrator narrator = new PortfolioLlamaNarrator(mock(WebClient.class), properties, new ObjectMapper());
        PortfolioSummary summary = PortfolioSummaryCalculator.summarize(List.of(), List.of(), YearMonth.of(2026, 10), false);

        PortfolioDiagnosis diagnosis = narrator.narrate(summary);

        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.RULE);
        assertThat(diagnosis.advice()).isEqualTo("자산을 등록하면 순자산과 구성이 계산됩니다.");
    }
}
