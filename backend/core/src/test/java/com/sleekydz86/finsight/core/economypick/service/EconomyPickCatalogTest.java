package com.sleekydz86.finsight.core.economypick.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.adapter.CassandraSnapshotCatalog;
import com.sleekydz86.finsight.core.economypick.adapter.YahooChartClient;
import com.sleekydz86.finsight.core.economypick.domain.KosdaqDashboard;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickCatalogTest {

    @Test
    void 실공시_파일에서_소송_시그널을_만든다() {
        KosdaqSignalService service = new KosdaqSignalService(new CassandraSnapshotCatalog(new ObjectMapper()));
        KosdaqDashboard dashboard = service.dashboard();
        assertThat(dashboard.companyCount()).isGreaterThan(0);
        assertThat(dashboard.lawsuits()).isGreaterThan(0);
        assertThat(dashboard.signals()).isNotEmpty();
        assertThat(service.rows()).allMatch(row -> "DASHBOARD".equals(row.lane()));
        assertThat(service.rows()).allMatch(row -> row.metrics().size() == 8);
    }

    @Test
    void 야후_차트_응답에서_봉을_읽는다() {
        String body = """
                {"chart":{"result":[{"timestamp":[1710000000,1710086400],"indicators":{"quote":[{"open":[1,2],"high":[2,3],"low":[0.5,1],"close":[1.5,null],"volume":[10,20]}]}}]}}
                """;
        List<PriceBar> bars = new YahooChartClient(null, new ObjectMapper()).parse(body);
        assertThat(bars).hasSize(1);
        assertThat(bars.get(0).close()).isEqualTo(1.5);
        assertThat(bars.get(0).volume()).isEqualTo(10);
    }

    @Test
    void 야후_심볼은_한_번만_인코딩한다() {
        URI uri = new YahooChartClient(null, new ObjectMapper()).chartUri("^VIX");
        assertThat(uri.toASCIIString()).contains("/chart/%5EVIX?");
        assertThat(uri.toASCIIString()).doesNotContain("%255E");
    }
}
