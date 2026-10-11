package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class LiveVodMcpTools {

    private static final int MAX_ITEMS = 30;

    private final YoutubeMediaQueryUseCase youtubeMediaQueryUseCase;

    public LiveVodMcpTools(YoutubeMediaQueryUseCase youtubeMediaQueryUseCase) {
        this.youtubeMediaQueryUseCase = youtubeMediaQueryUseCase;
    }

    @Tool(description = "실시간 VOD 탭 영상을 조회합니다. tab 예: ALL, LIVE, MARKET, GOMHEE, BOOTYFUL, THEME, MACRO, SYUKA. 읽기 전용입니다.")
    public LiveVodListResult listLiveVod(
            @ToolParam(description = "탭. 생략 시 ALL", required = false) String tab) {
        LiveVodFeedResponse feed = youtubeMediaQueryUseCase.getLiveVodFeed(tab);
        if (feed == null) {
            return new LiveVodListResult(null, tab, 0, List.of());
        }
        List<LiveVodItem> items = new ArrayList<>();
        for (LiveVodFeedResponse.LiveVodSectionResponse section : feed.sections()) {
            if (section == null || section.items() == null) {
                continue;
            }
            for (LiveVodFeedResponse.LiveVodItemResponse item : section.items()) {
                if (items.size() >= MAX_ITEMS) {
                    break;
                }
                items.add(new LiveVodItem(
                        item.videoId(),
                        item.title(),
                        item.channelTitle(),
                        item.watchUrl(),
                        section.heading()));
            }
        }
        return new LiveVodListResult(feed.title(), feed.tab(), items.size(), items);
    }

    public record LiveVodItem(
            String videoId,
            String title,
            String channelTitle,
            String watchUrl,
            String section) {
    }

    public record LiveVodListResult(String title, String tab, int count, List<LiveVodItem> items) {
    }
}
