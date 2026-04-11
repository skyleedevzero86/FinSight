package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class YoutubeVideoSearchRequest {
    private int page = 0;
    private int size = 20;
    private String category;
}
