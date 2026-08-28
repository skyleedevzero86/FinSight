package com.sleekydz86.finsight.core.media.youtube.domain.port.in;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoSearchRequest;

public interface YoutubeMediaQueryUseCase {
    PaginationResponse<YoutubeVideoListResponse> getPublishedVideos(YoutubeVideoSearchRequest request);

    YoutubeVideoDetailResponse getPublishedVideoDetail(Long boardId);

    LiveVodFeedResponse getLiveVodFeed(String tab);
}
