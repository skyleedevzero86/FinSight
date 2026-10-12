package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.service.AiModelSelectionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsAiAnalysisRequesterAdapterTest {

    @Test
    void 방금_실패한_모델은_대체_호출에서_빼다() {
        CountingRequester chatgpt = new CountingRequester(AiModel.CHATGPT);
        NewsAiAnalysisRequesterAdapter adapter = new NewsAiAnalysisRequesterAdapter(
                List.of(chatgpt),
                new AiModelSelectionService(List.of(chatgpt)));

        assertThatThrownBy(() -> adapter.analyseNewses(AiModel.CHATGPT, new Content("제목", "본문")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHATGPT");
        assertThat(chatgpt.calls.get()).isEqualTo(1);
    }

    private static final class CountingRequester implements NewsAiRequester {
        private final AiModel model;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingRequester(AiModel model) {
            this.model = model;
        }

        @Override
        public AiModel supports() {
            return model;
        }

        @Override
        public AiChatResponse request(AiChatRequest aiChatRequest) {
            calls.incrementAndGet();
            throw new IllegalStateException("모델 장애");
        }
    }
}
