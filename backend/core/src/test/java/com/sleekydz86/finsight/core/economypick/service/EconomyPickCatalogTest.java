package com.sleekydz86.finsight.core.economypick.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.adapter.CassandraSnapshotCatalog;
import com.sleekydz86.finsight.core.economypick.adapter.YahooChartClient;
import com.sleekydz86.finsight.core.economypick.domain.KosdaqDashboard;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickCatalogTest {

    @Test
    @DisplayName("실공시 파일에서 소송 시그널을 만든다")
    void buildsLawsuitSignalFromFilingFile() {
        // given
        KosdaqSignalService service = new KosdaqSignalService(new CassandraSnapshotCatalog(new ObjectMapper()));

        // when
        KosdaqDashboard dashboard = service.dashboard();

        // then
        assertThat(dashboard.companyCount()).isGreaterThan(0);
        assertThat(dashboard.lawsuits()).isGreaterThan(0);
        assertThat(dashboard.signals()).isNotEmpty();
        assertThat(service.rows()).allMatch(row -> "DASHBOARD".equals(row.lane()));
        assertThat(service.rows()).allMatch(row -> row.metrics().size() == 8);
    }

    @Test
    @DisplayName("종목정보 칸에는 시가총액과 순위만 넣고 위험 점수는 넣지 않는다")
    void keepsCompanySlotsFreeOfRiskScore() {
        // given
        KosdaqSignalService service = new KosdaqSignalService(new CassandraSnapshotCatalog(new ObjectMapper()));

        // when
        var rows = service.rows();

        // then
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.metrics()).hasSize(8);
            assertThat(row.metrics()).noneMatch(value -> value.startsWith("위험"));
            assertThat(row.metrics().get(2)).isEmpty();
            assertThat(row.metrics().get(6)).isEmpty();
            assertThat(row.metrics().get(7)).isEmpty();
        });
        assertThat(rows).anyMatch(row -> row.detail().contains("위험"));
        assertThat(rows).anyMatch(row -> !row.metrics().get(0).isBlank());
        assertThat(rows).anyMatch(row -> row.metrics().get(1).endsWith("위"));
    }

    @Test
    @DisplayName("야후 차트 응답에서 봉을 읽는다")
    void readsBarsFromYahooChartResponse() {
        // given
        String body = """
                {"chart":{"result":[{"timestamp":[1710000000,1710086400],"indicators":{"quote":[{"open":[1,2],"high":[2,3],"low":[0.5,1],"close":[1.5,null],"volume":[10,20]}]}}]}}
                """;
        List<PriceBar> bars = new YahooChartClient(null, new ObjectMapper()).parse(body);

        // when

        // then
        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).close()).isEqualTo(1.5);
        assertThat(bars.get(0).volume()).isEqualTo(10);
    }

    @Test
    @DisplayName("야후 심볼은 한 번만 인코딩한다")
    void encodesYahooSymbolOnce() {
        // given
        URI uri = new YahooChartClient(null, new ObjectMapper()).chartUri("^VIX");

        // when

        // then
        assertThat(uri.toASCIIString()).contains("/chart/%5EVIX?");
        assertThat(uri.toASCIIString()).doesNotContain("%255E");
    }
}
