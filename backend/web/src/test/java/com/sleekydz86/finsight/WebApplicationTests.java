package com.sleekydz86.finsight;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = WebApplication.class)
@Disabled("로컬 MySQL이 없으면 웹 컨텍스트가 뜨지 않음. 통합 테스트 환경 정리 예정")
class WebApplicationTests {

    @Test
    void contextLoads() {
    }

}
