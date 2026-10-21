package com.sleekydz86.finsight.core.auth.util;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieSupportTest {

    @Test
    @DisplayName("액세스와 리프레시는 httpOnly이고 힌트 쿠키는 아니다")
    void accessAndRefreshAreHttpOnlyAndHintCookieIsNot() {
        // given
        JwtCookieSupport support = new JwtCookieSupport(3_600_000L, 2_592_000_000L, false);
        HttpHeaders headers = new HttpHeaders();

        // when
        support.writeAuthCookies(headers, JwtToken.builder()
                .accessToken("access-example")
                .refreshToken("refresh-example")
                .expiresIn(LocalDateTime.of(2026, 10, 9, 2, 0))
                .build());
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);

        // then
        assertThat(cookie(cookies, "accessToken")).contains("HttpOnly");
        assertThat(cookie(cookies, "refreshToken")).contains("HttpOnly");
        assertThat(cookie(cookies, "finsight_auth")).doesNotContain("HttpOnly");
    }

    private static String cookie(List<String> cookies, String name) {
        assertThat(cookies).isNotNull();
        return cookies.stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
    }
}
