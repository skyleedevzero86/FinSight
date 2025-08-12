package com.sleekydz86.finsight.core.news.fixture;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.*;

import java.time.LocalDateTime;
import java.util.Collections;

public class NewsFixture {

    public static AiOverview 인공지능_오버뷰_생성_비트코인_긍정적_30프로() {
        return new AiOverview(
                "overview",
                SentimentType.POSITIVE,
                0.3,
                Collections.singletonList(TargetCategory.BTC)
        );
    }

    public static News 뉴스_생성_블룸버그_비트코인() {
        return new News(
                1L,
                NewsProvider.BLOOMBERG,
                LocalDateTime.now(),
                new Content("originalTitle", "originalContent"),
                new Content("translatedTitle", "translatedContent"),
                인공지능_오버뷰_생성_비트코인_긍정적_30프로()
        );
    }

    public static News 뉴스_생성_블룸버그_테슬라() {
        return new News(
                1L,
                NewsProvider.BLOOMBERG,
                LocalDateTime.now(),
                new Content("originalTitle", "originalContent"),
                new Content("translatedTitle", "translatedContent"),
                인공지능_오버뷰_생성_비트코인_긍정적_30프로()
                        .copyWithTargetCategories(Collections.singletonList(TargetCategory.TSLA))
        );
    }
}