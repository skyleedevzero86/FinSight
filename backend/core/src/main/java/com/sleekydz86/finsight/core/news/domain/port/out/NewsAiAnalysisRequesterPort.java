package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;

import java.util.List;

public interface NewsAiAnalysisRequesterPort {
    List<News> analyseNewses(AiModel activeAiModel, Content originalNewsContent);
}
