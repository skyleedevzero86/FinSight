package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.NewsOpenAiAnalysisRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@StepScope
public class OpenAiAnalysisTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAnalysisTasklet.class);

    private final NewsPersistencePort newsPersistencePort;
    private final NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester;

    public OpenAiAnalysisTasklet(NewsPersistencePort newsPersistencePort,
                                 NewsOpenAiAnalysisRequester newsOpenAiAnalysisRequester) {
        this.newsPersistencePort = newsPersistencePort;
        this.newsOpenAiAnalysisRequester = newsOpenAiAnalysisRequester;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            List<News> unAnalyzedNewses = newsPersistencePort.findByOverviewIsNull().getNewses();

            if (!unAnalyzedNewses.isEmpty()) {
                log.info("AI 분석을 진행합니다.");
                processNewsWithAi(unAnalyzedNewses);
            } else {
                log.info("AI 분석할 새로운 뉴스가 없습니다");
            }

            return RepeatStatus.FINISHED;
        } catch (Exception e) {
            log.error("AI 분석 중 오류 발생", e);
            throw e;
        }
    }

    public void processNewsWithAi(List<News> unAnalyzedNewses) {
        try {
            AiChatResponse analysisResults = newsOpenAiAnalysisRequester
                    .request(createAiRequest(unAnalyzedNewses))
                    .block();

            List<News> updatedNewses = updateNewsWithResults(unAnalyzedNewses, analysisResults);
            newsPersistencePort.saveAllNews(updatedNewses);
        } catch (Exception e) {
            log.error("벌크 분석 실패: {}", e.getMessage());
            throw e;
        }
    }

    private AiChatRequest createAiRequest(List<News> newses) {
        List<AiChatRequest.NewsItemRequest> newsItems = newses.stream()
                .map(news -> new AiChatRequest.NewsItemRequest(
                        news.getOriginalContent().getTitle(),
                        news.getOriginalContent().getContent()
                ))
                .collect(Collectors.toList());

        return new AiChatRequest(newsItems);
    }

    public List<News> updateNewsWithResults(List<News> newses, AiChatResponse response) {
        return IntStream.range(0, newses.size())
                .mapToObj(i -> {
                    News news = newses.get(i);
                    AiChatResponse.Analysis analysis = response.getAnalyses().get(i);

                    return news.updateAiAnalysis(
                            analysis.getOverView(),
                            analysis.getTranslatedTitle(),
                            analysis.getTranslatedContent(),
                            analysis.getCategories(),
                            analysis.getSentimentType(),
                            analysis.getSentimentRatio()
                    );
                })
                .collect(Collectors.toList());
    }
}