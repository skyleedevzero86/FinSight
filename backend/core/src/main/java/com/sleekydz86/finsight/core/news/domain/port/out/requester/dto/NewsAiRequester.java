package com.sleekydz86.finsight.core.news.domain.port.out.requester.dto;

import com.sleekydz86.finsight.core.global.AiModel;

public interface NewsAiRequester {

    AiModel supports();

    AiChatResponse request(AiChatRequest aiChatRequest);
}