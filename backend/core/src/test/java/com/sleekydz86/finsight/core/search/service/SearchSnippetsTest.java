package com.sleekydz86.finsight.core.search.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class SearchSnippetsTest {

    @Test
    @DisplayName("본문에서 검색어 주변을 잘라 보여 준다")
    void cutsSnippetAroundTheSearchWord() {
        // given
        String body = "아침 메뉴로 유부초밥을 준비했고 식초와 설탕으로 간을 맞췄습니다.";

        // when

        // then
        assertThat(SearchSnippets.keyword(" 유부%초밥_ ")).isEqualTo("유부초밥");
        assertThat(SearchSnippets.snippet(body, "제목", "유부초밥")).contains("유부초밥");
        assertThat(SearchSnippets.title("", "대체 제목")).isEqualTo("대체 제목");
    }
}
