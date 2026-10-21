package com.sleekydz86;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("JavaMailSender 빈이 없어 배치 컨텍스트가 뜨지 않음. 메일 서버 통합 테스트 환경 정리 예정")
class BatchApplicationTests {

    @Test
    @DisplayName("배치 애플리케이션 컨텍스트가 뜬다")
    void contextLoads() {
        // given
        // when
        // then
    }

}
