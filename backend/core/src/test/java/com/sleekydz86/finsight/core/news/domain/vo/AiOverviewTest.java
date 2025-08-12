package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class AiOverviewTest {

    @Test
    void 현재_Category가_포함되어_있으면_true를_반환한다() {
        // given
        AiOverview aiOverview = NewsFixture.인공지능_오버뷰_생성_비트코인_긍정적_30프로();

        // when
        boolean result = aiOverview.isMatchedCategory(Arrays.asList(TargetCategory.BTC, TargetCategory.TSLA));

        // then
        Assertions.assertThat(result).isTrue();
    }
}