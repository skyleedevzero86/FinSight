package com.sleekydz86.finsight.core.news.domain.port.out.requester;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;

public interface NewsRequester {

    NewsProvider supports();

    News scrap();
}