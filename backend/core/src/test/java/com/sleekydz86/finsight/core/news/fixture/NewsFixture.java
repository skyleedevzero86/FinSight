package com.sleekydz86.finsight.core.news.fixture;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.*;
import java.time.LocalDateTime;
import java.util.Collections;

public class NewsFixture {

    public static AiOverview positiveBitcoinOverview() {
        return new AiOverview(
                "overview",
                SentimentType.POSITIVE,
                0.3,
                Collections.singletonList(TargetCategory.BTC)
        );
    }

    public static News bloombergBitcoinNews() {
        return new News(
                1L,
                new NewsMeta(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "sourceUrl"
                ),
                LocalDateTime.now(),
                new Content("originalTitle", "originalContent"),
                new Content("translatedTitle", "translatedContent"),
                positiveBitcoinOverview()
        );
    }

    public static News bloombergTeslaNews() {
        return new News(
                1L,
                new NewsMeta(
                        NewsProvider.BLOOMBERG,
                        LocalDateTime.now(),
                        "sourceUrl"
                ),
                LocalDateTime.now(),
                new Content("originalTitle", "originalContent"),
                new Content("translatedTitle", "translatedContent"),
                positiveBitcoinOverview()
                        .copyWithTargetCategories(Collections.singletonList(TargetCategory.TSLA))
        );
    }
}