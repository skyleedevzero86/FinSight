package com.sleekydz86.finsight.core.news.domain.port.in;

import com.sleekydz86.finsight.core.news.domain.Newses;

public interface NewsCommandUseCase {

    Newses scrapNewses();
}