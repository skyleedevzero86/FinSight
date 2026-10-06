package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class YoutubeSourceActiveRequest {
    @NotNull
    private Boolean active;

    private boolean rejected;
}
