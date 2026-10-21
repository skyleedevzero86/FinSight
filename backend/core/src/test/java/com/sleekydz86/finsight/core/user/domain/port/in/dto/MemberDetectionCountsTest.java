package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class MemberDetectionCountsTest {

    @Test
    @DisplayName("값이 있는 회원정보만 탐지 항목으로 남긴다")
    void keepsOnlyMemberFieldsThatHaveValues() {
        // given
        MemberDetectionCounts counts = new MemberDetectionCounts(4, 4, 0);

        // when

        // then
        assertThat(counts.filledLabels()).containsExactly("아이디", "닉네임");
    }

    @Test
    @DisplayName("이메일이 있으면 이메일 항목을 포함한다")
    void includesEmailItemWhenEmailExists() {
        // given
        MemberDetectionCounts counts = new MemberDetectionCounts(2, 2, 1);

        // when

        // then
        assertThat(counts.filledLabels()).containsExactly("아이디", "닉네임", "이메일");
    }
}
