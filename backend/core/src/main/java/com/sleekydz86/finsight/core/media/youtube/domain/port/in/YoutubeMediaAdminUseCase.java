package com.sleekydz86.finsight.core.media.youtube.domain.port.in;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAdminVideoSearchRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAiEnrichmentSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceCreateRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeManualImportRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSyncSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoPublishRequest;

import java.util.List;

public interface YoutubeMediaAdminUseCase {
    PaginationResponse<YoutubeVideoListResponse> getAdminVideos(YoutubeAdminVideoSearchRequest request);

    YoutubeVideoDetailResponse getAdminVideoDetail(Long boardId);

    List<YoutubeImportSourceResponse> getImportSources();

    YoutubeSourceReviewResponse getSourceReview(Long sourceId, YoutubeSourceReviewRequest request);

    YoutubeImportSourceResponse createImportSource(String adminEmail, YoutubeImportSourceCreateRequest request);

    YoutubeSyncSummaryResponse importManualUrls(String adminEmail, YoutubeManualImportRequest request);

    YoutubeSyncSummaryResponse syncSource(Long sourceId);

    YoutubeSyncSummaryResponse syncActiveSources();

    YoutubeAiEnrichmentSummaryResponse enrichPendingDraftVideos();

    YoutubeVideoDetailResponse publishVideo(Long boardId, String adminEmail, YoutubeVideoPublishRequest request);

    YoutubeVideoDetailResponse hideVideo(Long boardId);
}
