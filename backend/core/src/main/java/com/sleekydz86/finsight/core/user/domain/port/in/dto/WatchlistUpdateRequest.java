package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistUpdateRequest {

    @NotNull(message = "카테고리 목록은 필수입니다")
    private List<TargetCategory> categories;
}