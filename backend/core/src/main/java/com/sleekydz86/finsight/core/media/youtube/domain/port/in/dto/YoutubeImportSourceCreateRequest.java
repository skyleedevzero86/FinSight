package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class YoutubeImportSourceCreateRequest {
    @NotNull
    private YoutubeImportSourceType sourceType;

    @NotBlank
    private String sourceValue;

    private String category;

    private boolean active = true;

    private boolean autoPublish = false;
}
