package com.sleekydz86.finsight.core.media.youtube.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeThumbnailUrlTest {

    @Test
    @DisplayName("유튜브 영상 아이디만 통과한다")
    void acceptsOnlyYoutubeVideoId() {
        // given

        // when

        // then
        assertThat(YoutubeThumbnailUrl.isVideoId("dQw4w9WgXcQ")).isTrue();
        assertThat(YoutubeThumbnailUrl.isVideoId("dummyvid06")).isFalse();
        assertThat(YoutubeThumbnailUrl.isVideoId(null)).isFalse();
    }

    @Test
    @DisplayName("유튜브 아이디가 아니면 썸네일 주소를 만들지 않는다")
    void doesNotBuildThumbnailUrlForNonYoutubeId() {
        // given

        // when

        // then
        assertThat(YoutubeThumbnailUrl.displayUrl("dummyvid06", "https://i.ytimg.com/vi/dummyvid06/hqdefault.jpg"))
                .isNull();
    }

    @Test
    @DisplayName("없는 고화질 썸네일은 항상 있는 크기로 바꾼다")
    void replacesMissingHqThumbnailWithAlwaysAvailableSize() {
        // given

        // when

        // then
        assertThat(YoutubeThumbnailUrl.displayUrl(
                "dQw4w9WgXcQ",
                "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"))
                .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg");
    }

    @Test
    @DisplayName("유튜브가 준 다른 주소는 그대로 둔다")
    void keepsOtherUrlProvidedByYoutube() {
        // given
        String stored = "https://images.example.com/thumb.jpg";

        // when

        // then
        assertThat(YoutubeThumbnailUrl.displayUrl("dQw4w9WgXcQ", stored)).isEqualTo(stored);
    }
}
