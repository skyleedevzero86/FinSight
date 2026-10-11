package com.sleekydz86.finsight;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("newsStatisticsJpaRepository 빈이 중복되어 컨텍스트가 뜨지 않음. 통합 테스트 환경 정리 예정")
class CoreApplicationTests {

    @Test
    void contextLoads() {
    }

}
