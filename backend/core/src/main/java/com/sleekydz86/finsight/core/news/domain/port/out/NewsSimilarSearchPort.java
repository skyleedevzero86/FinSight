package com.sleekydz86.finsight.core.news.domain.port.out;

import java.util.List;

public interface NewsSimilarSearchPort {

    List<Long> findSimilarIds(long newsId, int limit);
}
