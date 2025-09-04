package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.fixture.NewsFixture;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsesTest {

    @Test
    void 리스트에서_필터에_해당되는_뉴스를_조회한다() {
        News targetNews = NewsFixture.뉴스_생성_블룸버그_테슬라();

        Newses newses = new Newses(Arrays.asList(
                targetNews,
                NewsFixture.뉴스_생성_블룸버그_비트코인()
        ));

        List<News> result = newses.findAllNewsesFilteredOfCategoriesAndProviders(
                Collections.singletonList(TargetCategory.TSLA),
                Collections.singletonList(NewsProvider.ALL)
        );

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertTrue(result.contains(targetNews))
        );
    }
}