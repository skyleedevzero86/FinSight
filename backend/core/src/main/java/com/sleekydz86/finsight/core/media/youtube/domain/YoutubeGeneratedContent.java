package com.sleekydz86.finsight.core.media.youtube.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeGeneratedContent {
    private String summary;
    private String editorComment;
    @Builder.Default
    private List<String> keyPoints = new ArrayList<>();
}
