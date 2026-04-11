package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class YoutubeManualImportRequest {
    @NotEmpty
    private List<String> urls = new ArrayList<>();

    private String category;

    private List<String> hashtags = new ArrayList<>();

    private boolean autoPublish = false;
}
