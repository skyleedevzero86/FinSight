package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioShareViewsTest {

    @Test
    void 저장된_문구만_있으면_종목명_없이_세_구간을_만든다() {
        PortfolioShareJpaEntity share = new PortfolioShareJpaEntity(
                0L,
                "month_park",
                "이번 달 순자산 변화",
                "PUBLIC",
                "1억 만들기",
                "약 2,000만원",
                20,
                "+3.7%",
                "20%",
                true,
                false,
                LocalDateTime.of(2026, 10, 9, 8, 40));

        PortfolioShareView view = PortfolioShareViews.from(share);

        assertThat(view.showExactNames()).isFalse();
        assertThat(view.showDebt()).isFalse();
        assertThat(view.amountMode()).isEqualTo("BAND");
        assertThat(view.recordMonth()).isEqualTo("2026년 10월");
        assertThat(view.totalLiabilitiesLabel()).isEqualTo("비공개");
        assertThat(view.assets()).hasSize(1);
        assertThat(view.assets().get(0).name()).isEmpty();
        assertThat(view.assets().get(0).amountLabel()).isEqualTo("약 2,000만원");
        assertThat(view.history()).hasSize(1);
        assertThat(view.history().get(0).statusLabel()).isEqualTo("확정");
        assertThat(view.history().get(0).deltaLabel()).isEqualTo("+3.7%");
    }
}
