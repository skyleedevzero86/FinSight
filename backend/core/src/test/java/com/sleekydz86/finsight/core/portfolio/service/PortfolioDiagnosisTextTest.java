package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioDiagnosis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioDiagnosisTextTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 설명_문장_안의_진단_json을_읽는다() {
        String content = """
                진단 결과입니다.
                {"insights":[{"tone":"WARN","text":"현금 비중이 낮습니다."},{"tone":"GOOD","text":"부채가 없습니다."}],"advice":"현금을 먼저 확보하세요."}
                """;

        PortfolioDiagnosis diagnosis = PortfolioDiagnosisText.parse(content, mapper);

        assertThat(diagnosis).isNotNull();
        assertThat(diagnosis.source()).isEqualTo(PortfolioDiagnosis.LLAMA2);
        assertThat(diagnosis.advice()).isEqualTo("현금을 먼저 확보하세요.");
        assertThat(diagnosis.insights()).hasSize(2);
        assertThat(diagnosis.insights().get(0).tone()).isEqualTo("WARN");
    }

    @Test
    void 알_수_없는_톤과_빈_문장은_버린다() {
        String content = """
                {"insights":[{"tone":"BAD","text":"무시"},{"tone":"GOOD","text":"  "},{"tone":"GOOD","text":"예금 비중이 높습니다."}],"advice":"구성을 유지하세요."}
                """;

        PortfolioDiagnosis diagnosis = PortfolioDiagnosisText.parse(content, mapper);

        assertThat(diagnosis.insights()).hasSize(1);
        assertThat(diagnosis.insights().get(0).text()).isEqualTo("예금 비중이 높습니다.");
    }

    @Test
    void 조언이_없으면_null이다() {
        assertThat(PortfolioDiagnosisText.parse("{\"insights\":[{\"tone\":\"GOOD\",\"text\":\"양호\"}]}", mapper)).isNull();
        assertThat(PortfolioDiagnosisText.parse("문장만 있습니다.", mapper)).isNull();
    }
}
