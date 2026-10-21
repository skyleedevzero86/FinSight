package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioDiagnosisTextTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("설명 문장 안의 진단 json을 읽는다")
    void readsDiagnosisJsonInsideTheExplanation() {
        // given
        String content = """
                진단 결과입니다.
                {"insights":[{"tone":"WARN","text":"현금 비중이 낮습니다."},{"tone":"GOOD","text":"부채가 없습니다."}],"advice":"현금을 먼저 확보하세요."}
                """;
        PortfolioDiagnosis diagnosis = PortfolioDiagnosisText.parse(content, mapper);

        // when

        // then
        assertThat(diagnosis).isNotNull();
        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.LLAMA2);
        assertThat(diagnosis.advice()).isEqualTo("현금을 먼저 확보하세요.");
        assertThat(diagnosis.insights()).hasSize(2);
        assertThat(diagnosis.insights().get(0).tone()).isEqualTo("WARN");
    }

    @Test
    @DisplayName("알 수 없는 톤과 빈 문장은 버린다")
    void dropsUnknownToneAndBlankSentence() {
        // given
        String content = """
                {"insights":[{"tone":"BAD","text":"무시"},{"tone":"GOOD","text":"  "},{"tone":"GOOD","text":"예금 비중이 높습니다."}],"advice":"구성을 유지하세요."}
                """;
        PortfolioDiagnosis diagnosis = PortfolioDiagnosisText.parse(content, mapper);

        // when

        // then
        assertThat(diagnosis.insights()).hasSize(1);
        assertThat(diagnosis.insights().get(0).text()).isEqualTo("예금 비중이 높습니다.");
    }

    @Test
    @DisplayName("조언이 없으면 null이다")
    void isNullWhenAdviceIsMissing() {
        // given

        // when

        // then
        assertThat(PortfolioDiagnosisText.parse("{\"insights\":[{\"tone\":\"GOOD\",\"text\":\"양호\"}]}", mapper)).isNull();
        assertThat(PortfolioDiagnosisText.parse("문장만 있습니다.", mapper)).isNull();
    }
}
