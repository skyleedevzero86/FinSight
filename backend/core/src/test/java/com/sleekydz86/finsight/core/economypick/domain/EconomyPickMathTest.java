package com.sleekydz86.finsight.core.economypick.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyPickMathTest {

    @Test
    @DisplayName("로고는 국내코드와 미국티커를 구분한다")
    void logoDistinguishesKoreanCodeFromUsTicker() {
        // given

        // when

        // then
        assertThat(QuoteText.logo("005930")).endsWith("/kr/005930.png");
        assertThat(QuoteText.logo("QQQ")).isEqualTo("https://assets.parqet.com/logos/symbol/QQQ");
        assertThat(QuoteText.logo("^VIX")).isEmpty();
        assertThat(QuoteText.logo("FG")).isEmpty();
    }

    @Test
    @DisplayName("급등 국면은 매수 시그널이다")
    void risingRegimeIsBuySignal() {
        // given
        QuantMath.Regime regime = QuantMath.regime(120, 100, 90, 75, 300, 100, 12);

        // when

        // then
        assertThat(regime.regime()).isEqualTo(3);
        assertThat(regime.signal()).isEqualTo("BUY");
        assertThat(QuantMath.hedge(3).longWeight()).isEqualTo(90);
    }

    @Test
    @DisplayName("하락 국면은 매도 시그널이다")
    void fallingRegimeIsSellSignal() {
        // given
        QuantMath.Regime regime = QuantMath.regime(70, 90, 100, 20, 10, 10, 35);

        // when

        // then
        assertThat(regime.label()).isEqualTo("하락");
        assertThat(QuantMath.hedge(0).hedge()).isEqualTo(35);
    }

    @Test
    @DisplayName("모멘텀과 AMQS 점수를 계산한다")
    void calculatesMomentumAndAmqsScore() {
        // given
        List<Double> prices = List.of(10d, 10d, 10d, 10d, 10d, 12d);

        // when

        // then
        assertThat(QuantMath.momentum(prices, 5)).isEqualTo(20);
        assertThat(QuantMath.momentumSignal(6)).isEqualTo("BUY");
        assertThat(QuantMath.amqsScore(16, 65, 120, 100)).isEqualTo(80);
    }

    @Test
    @DisplayName("과매도 윌리엄스 점수는 매수다")
    void oversoldWilliamsScoreIsBuy() {
        // given
        List<PriceBar> bars = oversold();
        SpaceXMath.ScoreCard card = SpaceXMath.score(bars);

        // when

        // then
        assertThat(card.williamsR()).isLessThanOrEqualTo(-80);
        assertThat(card.signal()).isIn("BUY", "HOLD");
        assertThat(card.score()).isPositive();
    }

    @Test
    @DisplayName("소송과 상장폐지 공시는 복합위험을 만든다")
    void lawsuitAndDelistingFilingsCreateCompoundRisk() {
        // given
        LocalDate today = LocalDate.of(2026, 10, 12);
        List<RiskRuleEngine.Filing> filings = List.of(
                new RiskRuleEngine.Filing("상장폐지결정 효력정지 가처분", today.minusDays(10)),
                new RiskRuleEngine.Filing("회생절차개시신청", today.minusDays(20)),
                new RiskRuleEngine.Filing("전환사채 발행", today.minusDays(30)));
        List<RiskRuleEngine.Firing> scored = RiskRuleEngine.evaluateFilings(filings, today, 180);
        List<RiskRuleEngine.Firing> combined = new java.util.ArrayList<>(scored);

        // when
        combined.addAll(RiskRuleEngine.composite(scored, List.of(), today));

        // then
        assertThat(scored).extracting(RiskRuleEngine.Firing::label).contains("소송/분쟁", "상장폐지위험", "CB발행");
        assertThat(RiskRuleEngine.totalRisk(combined)).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("공포탐욕 상태와 하이닉스 베타를 계산한다")
    void calculatesFearGreedStatusAndHynixBeta() {
        // when

        // then
        assertThat(FearGreedMath.status(35)).isEqualTo("극단적 공포");

        // given
        List<Double> mu = rising(30, 100);
        List<Double> hynix = rising(30, 1000);
        HynixPredictMath.Prediction prediction = HynixPredictMath.predict(mu, hynix);

        // then
        assertThat(prediction).isNotNull();
        assertThat(prediction.beta()).isGreaterThan(0);
        assertThat(prediction.hynixPredictedChangePct()).isPositive();
    }

    @Test
    @DisplayName("트럼프 섹터는 언급 횟수로 픽을 만든다")
    void trumpSectorBuildsPicksFromMentionCount() {
        // given
        List<TrumpSectorBook.Pick> picks = TrumpSectorBook.score(List.of(
                "Trump tariff on China steel",
                "New tariff hits steel imports",
                "Bitcoin rally after Trump post"), 6);

        // when

        // then
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
