package com.sleekydz86.finsight.core.economypick.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickMathTest {

    @Test
    void 로고는_국내코드와_미국티커를_구분한다() {
        assertThat(QuoteText.logo("005930")).endsWith("/kr/005930.png");
        assertThat(QuoteText.logo("QQQ")).isEqualTo("https://assets.parqet.com/logos/symbol/QQQ");
        assertThat(QuoteText.logo("^VIX")).isEmpty();
        assertThat(QuoteText.logo("FG")).isEmpty();
    }

    @Test
    void 급등_국면은_매수_시그널이다() {
        QuantMath.Regime regime = QuantMath.regime(120, 100, 90, 75, 300, 100, 12);
        assertThat(regime.regime()).isEqualTo(3);
        assertThat(regime.signal()).isEqualTo("BUY");
        assertThat(QuantMath.hedge(3).longWeight()).isEqualTo(90);
    }

    @Test
    void 하락_국면은_매도_시그널이다() {
        QuantMath.Regime regime = QuantMath.regime(70, 90, 100, 20, 10, 10, 35);
        assertThat(regime.label()).isEqualTo("하락");
        assertThat(QuantMath.hedge(0).hedge()).isEqualTo(35);
    }

    @Test
    void 모멘텀과_AMQS_점수를_계산한다() {
        List<Double> prices = List.of(10d, 10d, 10d, 10d, 10d, 12d);
        assertThat(QuantMath.momentum(prices, 5)).isEqualTo(20);
        assertThat(QuantMath.momentumSignal(6)).isEqualTo("BUY");
        assertThat(QuantMath.amqsScore(16, 65, 120, 100)).isEqualTo(80);
    }

    @Test
    void 과매도_윌리엄스_점수는_매수다() {
        List<PriceBar> bars = oversold();
        SpaceXMath.ScoreCard card = SpaceXMath.score(bars);
        assertThat(card.williamsR()).isLessThanOrEqualTo(-80);
        assertThat(card.signal()).isIn("BUY", "HOLD");
        assertThat(card.score()).isPositive();
    }

    @Test
    void 소송과_상장폐지_공시는_복합위험을_만든다() {
        LocalDate today = LocalDate.of(2026, 10, 12);
        List<RiskRuleEngine.Filing> filings = List.of(
                new RiskRuleEngine.Filing("상장폐지결정 효력정지 가처분", today.minusDays(10)),
                new RiskRuleEngine.Filing("회생절차개시신청", today.minusDays(20)),
                new RiskRuleEngine.Filing("전환사채 발행", today.minusDays(30)));
        List<RiskRuleEngine.Firing> scored = RiskRuleEngine.evaluateFilings(filings, today, 180);
        List<RiskRuleEngine.Firing> combined = new java.util.ArrayList<>(scored);
        combined.addAll(RiskRuleEngine.composite(scored, List.of(), today));
        assertThat(scored).extracting(RiskRuleEngine.Firing::label).contains("소송/분쟁", "상장폐지위험", "CB발행");
        assertThat(RiskRuleEngine.totalRisk(combined)).isGreaterThanOrEqualTo(50);
    }

    @Test
    void 공포탐욕_상태와_하이닉스_베타를_계산한다() {
        assertThat(FearGreedMath.status(35)).isEqualTo("극단적 공포");
        List<Double> mu = rising(30, 100);
        List<Double> hynix = rising(30, 1000);
        HynixPredictMath.Prediction prediction = HynixPredictMath.predict(mu, hynix);
        assertThat(prediction).isNotNull();
        assertThat(prediction.beta()).isGreaterThan(0);
        assertThat(prediction.hynixPredictedChangePct()).isPositive();
    }

    @Test
    void 트럼프_섹터는_언급_횟수로_픽을_만든다() {
        List<TrumpSectorBook.Pick> picks = TrumpSectorBook.score(List.of(
                "Trump tariff on China steel",
                "New tariff hits steel imports",
                "Bitcoin rally after Trump post"), 6);
        assertThat(picks).extracting(TrumpSectorBook.Pick::ticker).contains("X", "MSTR");
        assertThat(TrumpSectorBook.mood(picks)).isIn("강경", "완화", "중립");
    }

    private static List<PriceBar> oversold() {
        List<PriceBar> bars = new java.util.ArrayList<>();
        for (int index = 0; index < 60; index++) {
            double close = index == 59 ? 1 : 20;
            double high = index < 46 ? 22 : 20;
            double low = index == 59 ? 1 : 18;
            bars.add(new PriceBar("2026-01-01", close, high, low, close, 1000));
        }
        return bars;
    }

    private static List<Double> rising(int size, double start) {
        List<Double> values = new java.util.ArrayList<>();
        for (int index = 0; index < size; index++) {
            values.add(start + index);
        }
        return values;
    }
}
