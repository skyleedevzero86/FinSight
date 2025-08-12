package com.sleekydz86.finsight.core.news.domain.port.out.requester;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.dto.AiChatResponse;

public interface NewsAiRequester {

    AiModel supports();

    AiChatResponse request(AiChatRequest aiChatRequest);
}