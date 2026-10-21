package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collections;

class NewsTest {

    @Test
    @DisplayName("뉴스 제공자가 포함되고 카테고리가 포함된다면 true를 반환한다")
    void returnsTrueWhenProviderAndCategoryMatch() {
        // given
        News news = NewsFixture.bloombergBitcoinNews();

        // when
        boolean result = news.isContainsNewsProviderAndCategories(
                Collections.singletonList(TargetCategory.BTC),
                Collections.singletonList(NewsProvider.BLOOMBERG)
        );

        // then
        Assertions.assertThat(result).isTrue();
    }
}
