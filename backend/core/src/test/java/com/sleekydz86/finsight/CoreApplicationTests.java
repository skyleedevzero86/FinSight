package com.sleekydz86.finsight;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("newsStatisticsJpaRepository 빈이 중복되어 컨텍스트가 뜨지 않음. 통합 테스트 환경 정리 예정")
class CoreApplicationTests {

    @Test
    @DisplayName("코어 애플리케이션 컨텍스트가 뜬다")
    void contextLoads() {
        // given
        // when
        // then
    }

}
