package com.sleekydz86.finsight.batch.helper.annotations;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@IntegrationTest
@Disabled("JavaMailSender 빈이 없어 배치 컨텍스트가 뜨지 않음. 메일 서버 통합 테스트 환경 정리 예정")
public class H2TableConnectionTests {

    private static final Logger log = LoggerFactory.getLogger(H2TableConnectionTests.class);

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("H2 테이블 스키마를 출력한다")
    public void printTableSchemas() {
        // given
        log.info("테이블 정보 조회 테스트");

        // when
        List<String> tables = entityManager
                .createNativeQuery(
                        "SELECT TABLE_NAME " +
                                "FROM INFORMATION_SCHEMA.TABLES " +
                                "WHERE TABLE_SCHEMA = 'PUBLIC'"
                )
                .getResultList();

        log.info("=== H2 데이터베이스 스키마 조회 ===");

        for (String tableName : tables) {

            List<Object[]> columns = entityManager
                    .createNativeQuery(
                            "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                                    "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = '" + tableName + "' " +
                                    "ORDER BY ORDINAL_POSITION"
                    )
                    .getResultList();

            log.info("테이블: {}", tableName);

            for (Object[] column : columns) {
                log.info("  컬럼: {}, 타입: {}, 길이: {}, NULL 허용: {}",
                        column[0], column[1], column[2], column[3]);
            }
        }

        // then
        log.info("=========================");
    }
}