package com.sleekydz86.finsight.core.news.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsAiAvailabilityTest {

    @Test
    void OpenAI_키가_있으면_연결_가능으로_본다() {
        assertThat(NewsAiAvailability.decide(true, false, false)).isTrue();
    }

    @Test
    void 키가_없고_Ollama가_꺼져_있으면_관리자_안내가_필요하다() {
        assertThat(NewsAiAvailability.decide(false, false, true)).isFalse();
    }

    @Test
    void 키가_없어도_Ollama가_연결되면_사용할_수_있다() {
        assertThat(NewsAiAvailability.decide(false, true, true)).isTrue();
    }

    @Test
    void Ollama가_켜져_있어도_연결이_없으면_사용할_수_없다() {
        assertThat(NewsAiAvailability.decide(false, true, false)).isFalse();
    }
}
