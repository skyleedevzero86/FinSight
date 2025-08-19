package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.NewsOpenAiAnalysisRequester;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsAiAnalysisRequesterAdapter implements NewsAiAnalysisRequesterPort {

    private final NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester;

    public NewsAiAnalysisRequesterAdapter(NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester) {
        this.newsOpenAiAnalysisRequester = newsOpenAiAnalysisRequester;
    }

    @Override
    public List<News> analyseNewses(AiModel activeAiModel, Content originalNewsContent) {
        try {
            AiChatRequest request = new AiChatRequest(
                    "news_analysis",
                    List.of(new AiChatRequest.NewsItemRequest(
                            originalNewsContent.getTitle(),
                            originalNewsContent.getContent())));

            AiChatResponse response = newsOpenAiAnalysisRequester.request(request);

            // 응답을 News 객체로 변환하는 로직
            return response.getAnalyses().stream()
                    .map(analysis -> News.createWithoutAI(
                            null, // NewsMeta는 별도로 설정 필요
                            originalNewsContent).updateAiAnalysis(
                                    analysis.getOverView(),
                                    analysis.getTranslatedTitle(),
                                    analysis.getTranslatedContent(),
                                    analysis.getCategories(),
                                    analysis.getSentimentType(),
                                    analysis.getSentimentRatio()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("AI 분석 중 오류 발생", e);
        }
    }
}