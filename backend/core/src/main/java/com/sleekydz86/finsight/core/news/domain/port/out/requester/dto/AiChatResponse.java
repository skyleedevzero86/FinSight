package com.sleekydz86.finsight.core.news.domain.port.out.requester.dto;

import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.util.List;
import java.util.Objects;

public class AiChatResponse {
    private final String overView;
    private final String translatedTitle;
    private final String translatedContent;
    private final List<TargetCategory> categories;
    private final SentimentType sentimentType;
    private final Double sentimentRatio;

    public AiChatResponse(String overView, String translatedTitle, String translatedContent,
                          List<TargetCategory> categories, SentimentType sentimentType, Double sentimentRatio) {
        this.overView = overView;
        this.translatedTitle = translatedTitle;
        this.translatedContent = translatedContent;
        this.categories = categories;
        this.sentimentType = sentimentType;
        this.sentimentRatio = sentimentRatio;
    }

    public String getOverView() {
        return overView;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public List<TargetCategory> getCategories() {
        return categories;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public Double getSentimentRatio() {
        return sentimentRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiChatResponse that = (AiChatResponse) o;
        return Objects.equals(overView, that.overView) &&
                Objects.equals(translatedTitle, that.translatedTitle) &&
                Objects.equals(translatedContent, that.translatedContent) &&
                Objects.equals(categories, that.categories) &&
                sentimentType == that.sentimentType &&
                Objects.equals(sentimentRatio, that.sentimentRatio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overView, translatedTitle, translatedContent, categories, sentimentType, sentimentRatio);
    }

    @Override
    public String toString() {
        return "AiChatResponse{" +
                "overView='" + overView + '\'' +
                ", translatedTitle='" + translatedTitle + '\'' +
                ", translatedContent='" + translatedContent + '\'' +
                ", categories=" + categories +
                ", sentimentType=" + sentimentType +
                ", sentimentRatio=" + sentimentRatio +
                '}';
    }
}