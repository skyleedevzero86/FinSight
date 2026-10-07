package com.sleekydz86.finsight.core.news.domain.port.in.dto;

public record NewsReactionResponse(String myReaction, long likeCount, long dislikeCount) {
}
