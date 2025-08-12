package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class NewsTest {

    @Test
    void 뉴스_제공자가_포함되고_카테고리가_포함된다면_true를_반환한다() {
        // given
        News news = NewsFixture.뉴스_생성_블룸버그_비트코인();

        // when
        boolean result = news.isContainsNewsProviderAndCategories(
                Collections.singletonList(TargetCategory.BTC),
                Collections.singletonList(NewsProvider.BLOOMBERG)
        );

        // then
        Assertions.assertThat(result).isTrue();
    }
}