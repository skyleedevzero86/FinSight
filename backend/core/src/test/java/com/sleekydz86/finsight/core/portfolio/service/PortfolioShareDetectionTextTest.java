package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareDetectionTextTest {

    @Test
    @DisplayName("허용된 글과 라벨만 반영한다")
    void appliesOnlyAllowedTextAndLabels() {
        // given
        Map<String, Set<Long>> hits = new LinkedHashMap<>();

        // when
        hits.put("과장 수익문구", new LinkedHashSet<>());
        hits.put("광고", new LinkedHashSet<>());

        // given
        String raw = "설명 {\"hits\":[{\"id\":4,\"labels\":[\"과장 수익문구\",\"없는라벨\"]},{\"id\":99,\"labels\":[\"광고\"]}]}";
        boolean accepted = PortfolioShareDetectionText.merge(raw, Set.of(4L), hits, new ObjectMapper());

        // then
        assertThat(accepted).isTrue();
        assertThat(hits.get("과장 수익문구")).containsExactly(4L);
        assertThat(hits.get("광고")).isEmpty();
    }
}
