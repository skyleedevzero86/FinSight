package com.sleekydz86.finsight.core.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareDetectionTextTest {

    @Test
    void 허용된_글과_라벨만_반영한다() {
        Map<String, Set<Long>> hits = new LinkedHashMap<>();
        hits.put("과장 수익문구", new LinkedHashSet<>());
        hits.put("광고", new LinkedHashSet<>());
        String raw = "설명 {\"hits\":[{\"id\":4,\"labels\":[\"과장 수익문구\",\"없는라벨\"]},{\"id\":99,\"labels\":[\"광고\"]}]}";

        boolean accepted = PortfolioShareDetectionText.merge(raw, Set.of(4L), hits, new ObjectMapper());

        assertThat(accepted).isTrue();
        assertThat(hits.get("과장 수익문구")).containsExactly(4L);
        assertThat(hits.get("광고")).isEmpty();
    }
}
