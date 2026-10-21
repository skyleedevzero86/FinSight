package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoSearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LiveVodMcpToolsTest {

    @Test
    @DisplayName("라이브 VOD 목록은 구간을 펼쳐 반환한다")
    void listLiveVod_flattensSections() {
        // given
        LiveVodMcpTools tools = new LiveVodMcpTools(new FixedLiveVodQuery());

        // when
        LiveVodMcpTools.LiveVodListResult result = tools.listLiveVod("market");

        // then
        assertThat(result.tab()).isEqualTo("MARKET");
        assertThat(result.items()).containsExactly(
                new LiveVodMcpTools.LiveVodItem("v1", "시장 브리핑", "채널", "/live-vod/watch/v1", "추천"));
    }

    private static final class FixedLiveVodQuery implements YoutubeMediaQueryUseCase {
        @Override
        public LiveVodFeedResponse getLiveVodFeed(String tab) {
            return new LiveVodFeedResponse(
                    "실시간 VOD",
                    "MARKET",
                    null,
                    null,
                    null,
                    List.of(new LiveVodFeedResponse.LiveVodSectionResponse(
                            "추천",
                            List.of(new LiveVodFeedResponse.LiveVodItemResponse(
                                    "v1", "시장 브리핑", null, "/live-vod/watch/v1", null, "채널")))));
        }

        @Override
        public PaginationResponse<YoutubeVideoListResponse> getPublishedVideos(YoutubeVideoSearchRequest request) {
            throw new UnsupportedOperationException("이 테스트는 해당 조회를 사용하지 않습니다.");
        }

        @Override
        public YoutubeVideoDetailResponse getPublishedVideoDetail(Long boardId) {
            throw new UnsupportedOperationException("이 테스트는 해당 조회를 사용하지 않습니다.");
        }
    }
}
