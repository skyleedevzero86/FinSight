package com.sleekydz86.finsight.core.news.domain.port.out.requester.dto;

import java.util.Objects;

public class AiChatRequest {
    private final String prompt;

    public AiChatRequest(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiChatRequest that = (AiChatRequest) o;
        return Objects.equals(prompt, that.prompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prompt);
    }

    @Override
    public String toString() {
        return "AiChatRequest{" +
                "prompt='" + prompt + '\'' +
                '}';
    }
}