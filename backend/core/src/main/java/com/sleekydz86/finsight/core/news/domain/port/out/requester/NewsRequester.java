package com.sleekydz86.finsight.core.news.domain.port.out.requester;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.NewsProvider;

public interface NewsRequester {

    NewsProvider supports();

    News scrap();
}