package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsesTest {

    @Test
    @DisplayName("리스트에서 필터에 해당되는 뉴스를 조회한다")
    void findsNewsMatchingTheListFilter() {
        // given
        News targetNews = NewsFixture.bloombergTeslaNews();
        Newses newses = new Newses(Arrays.asList(
                targetNews,
                NewsFixture.bloombergBitcoinNews()
        ));

        // when
        List<News> result = newses.findAllNewsesFilteredOfCategoriesAndProviders(
                Collections.singletonList(TargetCategory.TSLA),
                Collections.singletonList(NewsProvider.ALL)
        );

        // then
        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertTrue(result.contains(targetNews))
        );
    }
}
