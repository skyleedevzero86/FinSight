package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

class AiOverviewTest {

    @Test
    @DisplayName("현재 Category가 포함되어 있으면 true를 반환한다")
    void returnsTrueWhenCurrentCategoryIsIncluded() {
        // given
        AiOverview aiOverview = NewsFixture.positiveBitcoinOverview();

        // when
        boolean result = aiOverview.isMatchedCategory(Arrays.asList(TargetCategory.BTC, TargetCategory.TSLA));

        // then
        Assertions.assertThat(result).isTrue();
    }
}
