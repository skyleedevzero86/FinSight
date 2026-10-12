package com.sleekydz86.finsight.core.media.youtube.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeThumbnailUrlTest {

    @Test
    void 유튜브_영상_아이디만_통과한다() {
        assertThat(YoutubeThumbnailUrl.isVideoId("dQw4w9WgXcQ")).isTrue();
        assertThat(YoutubeThumbnailUrl.isVideoId("dummyvid06")).isFalse();
        assertThat(YoutubeThumbnailUrl.isVideoId(null)).isFalse();
    }

    @Test
    void 유튜브_아이디가_아니면_썸네일_주소를_만들지_않는다() {
        assertThat(YoutubeThumbnailUrl.displayUrl("dummyvid06", "https://i.ytimg.com/vi/dummyvid06/hqdefault.jpg"))
                .isNull();
    }

    @Test
    void 없는_고화질_썸네일은_항상_있는_크기로_바꾼다() {
        assertThat(YoutubeThumbnailUrl.displayUrl(
                "dQw4w9WgXcQ",
                "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"))
                .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg");
    }

    @Test
    void 유튜브가_준_다른_주소는_그대로_둔다() {
        String stored = "https://images.example.com/thumb.jpg";

        assertThat(YoutubeThumbnailUrl.displayUrl("dQw4w9WgXcQ", stored)).isEqualTo(stored);
    }
}
