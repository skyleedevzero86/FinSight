package com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "youtube.api")
public class YoutubeApiProperties {

    private String baseUrl = "https://www.googleapis.com/youtube/v3";
    private String apiKey = "";
    private int maxResultsPerSource = 25;
    private String liveChannelHandle = "";
    private String liveChannelId = "";
    private String featuredLiveVideoId = "";
    private String moreChannelHandle = "";
    private String moreSectionHeading = "";
    private String moreSearchQuery = "경제 증시 시장";
    private int moreMaxResults = 4;
    private int moreMinDurationSeconds = 180;
    private List<MoreChannelSource> moreChannels = new ArrayList<>();
    private String liveSearchQuery = "미국 증시 브리핑";
    private int liveMaxResults = 24;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxResultsPerSource() {
        return maxResultsPerSource;
    }

    public void setMaxResultsPerSource(int maxResultsPerSource) {
        this.maxResultsPerSource = maxResultsPerSource;
    }

    public String getLiveChannelHandle() {
        return liveChannelHandle;
    }

    public void setLiveChannelHandle(String liveChannelHandle) {
        this.liveChannelHandle = liveChannelHandle;
    }

    public String getLiveChannelId() {
        return liveChannelId;
    }

    public void setLiveChannelId(String liveChannelId) {
        this.liveChannelId = liveChannelId;
    }

    public String getFeaturedLiveVideoId() {
        return featuredLiveVideoId;
    }

    public void setFeaturedLiveVideoId(String featuredLiveVideoId) {
        this.featuredLiveVideoId = featuredLiveVideoId;
    }

    public String getMoreChannelHandle() {
        return moreChannelHandle;
    }

    public void setMoreChannelHandle(String moreChannelHandle) {
        this.moreChannelHandle = moreChannelHandle;
    }

    public String getMoreSectionHeading() {
        return moreSectionHeading;
    }

    public void setMoreSectionHeading(String moreSectionHeading) {
        this.moreSectionHeading = moreSectionHeading;
    }

    public String getMoreSearchQuery() {
        return moreSearchQuery;
    }

    public void setMoreSearchQuery(String moreSearchQuery) {
        this.moreSearchQuery = moreSearchQuery;
    }

    public int getMoreMaxResults() {
        return moreMaxResults;
    }

    public void setMoreMaxResults(int moreMaxResults) {
        this.moreMaxResults = moreMaxResults;
    }

    public int getMoreMinDurationSeconds() {
        return moreMinDurationSeconds;
    }

    public void setMoreMinDurationSeconds(int moreMinDurationSeconds) {
        this.moreMinDurationSeconds = moreMinDurationSeconds;
    }

    public List<MoreChannelSource> getMoreChannels() {
        return moreChannels;
    }

    public void setMoreChannels(List<MoreChannelSource> moreChannels) {
        this.moreChannels = moreChannels;
    }

    public List<MoreChannelSource> resolveMoreChannels() {
        if (moreChannels != null && !moreChannels.isEmpty()) {
            return moreChannels.stream()
                    .filter(channel -> channel != null
                            && channel.getHandle() != null
                            && !channel.getHandle().isBlank())
                    .toList();
        }

        if (moreChannelHandle != null && !moreChannelHandle.isBlank()) {
            MoreChannelSource legacy = new MoreChannelSource();
            legacy.setHandle(moreChannelHandle);
            legacy.setHeading(
                    moreSectionHeading == null || moreSectionHeading.isBlank()
                            ? moreChannelHandle
                            : moreSectionHeading);
            legacy.setSearchQuery(moreSearchQuery);
            legacy.setMaxResults(moreMaxResults);
            legacy.setMinDurationSeconds(moreMinDurationSeconds);
            return List.of(legacy);
        }

        MoreChannelSource gomhee = new MoreChannelSource();
        gomhee.setHandle("gomhee");
        gomhee.setHeading("박곰희 TV");
        gomhee.setSearchQuery("경제 증시 시장");
        gomhee.setMaxResults(4);
        gomhee.setMinDurationSeconds(180);
        gomhee.setTabs(List.of("ALL"));

        MoreChannelSource syuka = new MoreChannelSource();
        syuka.setHandle("syukaworld");
        syuka.setHeading("슈카월드");
        syuka.setSearchQuery("경제 시사 증시");
        syuka.setMaxResults(4);
        syuka.setMinDurationSeconds(180);
        syuka.setTabs(List.of("ALL"));

        MoreChannelSource bootyful = new MoreChannelSource();
        bootyful.setHandle("money-multiple");
        bootyful.setHeading("부티플");
        bootyful.setSearchQuery("경제 투자 주식");
        bootyful.setMaxResults(4);
        bootyful.setMinDurationSeconds(180);
        bootyful.setTabs(List.of("ALL"));

        return List.of(gomhee, syuka, bootyful);
    }

    public String getLiveSearchQuery() {
        return liveSearchQuery;
    }

    public void setLiveSearchQuery(String liveSearchQuery) {
        this.liveSearchQuery = liveSearchQuery;
    }

    public int getLiveMaxResults() {
        return liveMaxResults;
    }

    public void setLiveMaxResults(int liveMaxResults) {
        this.liveMaxResults = liveMaxResults;
    }

    public static class MoreChannelSource {
        private String handle = "";
        private String heading = "";
        private String searchQuery = "경제 증시 시장";
        private int maxResults = 4;
        private int minDurationSeconds = 180;
        private List<String> tabs = new ArrayList<>();

        public String getHandle() {
            return handle;
        }

        public void setHandle(String handle) {
            this.handle = handle;
        }

        public String getHeading() {
            return heading;
        }

        public void setHeading(String heading) {
            this.heading = heading;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public void setSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getMinDurationSeconds() {
            return minDurationSeconds;
        }

        public void setMinDurationSeconds(int minDurationSeconds) {
            this.minDurationSeconds = minDurationSeconds;
        }

        public List<String> getTabs() {
            return tabs;
        }

        public void setTabs(List<String> tabs) {
            this.tabs = tabs;
        }

        public boolean matchesTab(String tab) {
            if (tabs == null || tabs.isEmpty()) {
                return true;
            }
            if (tab == null || tab.isBlank()) {
                return tabs.stream().anyMatch(value -> "ALL".equalsIgnoreCase(value));
            }
            String normalized = tab.trim().toUpperCase();
            return tabs.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toUpperCase())
                    .anyMatch(value -> value.equals(normalized));
        }
    }
}
