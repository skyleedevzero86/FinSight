package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberDetectionCountsTest {

    @Test
    void 값이_있는_회원정보만_탐지_항목으로_남긴다() {
        MemberDetectionCounts counts = new MemberDetectionCounts(4, 4, 0);

        assertThat(counts.filledLabels()).containsExactly("아이디", "닉네임");
    }

    @Test
    void 이메일이_있으면_이메일_항목을_포함한다() {
        MemberDetectionCounts counts = new MemberDetectionCounts(2, 2, 1);

        assertThat(counts.filledLabels()).containsExactly("아이디", "닉네임", "이메일");
    }
}
