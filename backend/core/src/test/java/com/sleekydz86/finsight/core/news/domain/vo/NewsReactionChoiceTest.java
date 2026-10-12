package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsReactionChoiceTest {

    @Test
    void firstChoiceKeepsRequestedType() {
        assertThat(NewsReactionChoice.apply(null, "like")).isEqualTo("LIKE");
    }

    @Test
    void sameChoiceClearsReaction() {
        assertThat(NewsReactionChoice.apply("LIKE", "LIKE")).isNull();
    }

    @Test
    void oppositeChoiceSwitchesType() {
        assertThat(NewsReactionChoice.apply("LIKE", "dislike")).isEqualTo("DISLIKE");
    }

    @Test
    void blankRequestIsRejected() {
        assertThatThrownBy(() -> NewsReactionChoice.normalize(" "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("LIKE");
    }
}
