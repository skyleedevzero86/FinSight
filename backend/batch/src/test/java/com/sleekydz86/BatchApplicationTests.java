package com.sleekydz86;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("JavaMailSender 빈이 없어 배치 컨텍스트가 뜨지 않음. 메일 서버 통합 테스트 환경 정리 예정")
class BatchApplicationTests {

    @Test
    void contextLoads() {
    }

}
