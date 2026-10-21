package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsReactionChoiceTest {

    @Test
    @DisplayName("처음 선택은 요청한 반응을 유지한다")
    void firstChoiceKeepsRequestedType() {
        // given

        // when

        // then
        assertThat(NewsReactionChoice.apply(null, "like")).isEqualTo("LIKE");
    }

    @Test
    @DisplayName("같은 반응을 다시 누르면 지운다")
    void sameChoiceClearsReaction() {
        // given

        // when

        // then
        assertThat(NewsReactionChoice.apply("LIKE", "LIKE")).isNull();
    }

    @Test
    @DisplayName("반대 반응은 유형을 바꾼다")
    void oppositeChoiceSwitchesType() {
        // given

        // when

        // then
        assertThat(NewsReactionChoice.apply("LIKE", "dislike")).isEqualTo("DISLIKE");
    }

    @Test
    @DisplayName("빈 요청은 거절한다")
    void blankRequestIsRejected() {
        // given

        // when

        // then
        assertThatThrownBy(() -> NewsReactionChoice.normalize(" "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("LIKE");
    }
}
