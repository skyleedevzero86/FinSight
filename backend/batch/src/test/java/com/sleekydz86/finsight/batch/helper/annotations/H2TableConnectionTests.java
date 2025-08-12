package com.sleekydz86.finsight.batch.helper.annotations;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@IntegrationTest
public class H2TableConnectionTests {

    private static final Logger log = LoggerFactory.getLogger(H2TableConnectionTests.class);

    @Autowired
    private EntityManager entityManager;

    @Test
    public void printTableSchemas() {
        log.info("테이블 정보 조회 테스트");

        @SuppressWarnings("unchecked")
        List<String> tables = entityManager
                .createNativeQuery(
                        "SELECT TABLE_NAME " +
                                "FROM INFORMATION_SCHEMA.TABLES " +
                                "WHERE TABLE_SCHEMA = 'PUBLIC'"
                )
                .getResultList();

        log.info("=== H2 Database Schema 조회 ===");

        for (String tableName : tables) {
            @SuppressWarnings("unchecked")
            List<Object[]> columns = entityManager
                    .createNativeQuery(
                            "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                                    "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = '" + tableName + "' " +
                                    "ORDER BY ORDINAL_POSITION"
                    )
                    .getResultList();

            log.info("Table: {}", tableName);

            for (Object[] column : columns) {
                log.info("  Column: {}, Type: {}, Length: {}, Nullable: {}",
                        column[0], column[1], column[2], column[3]);
            }
        }

        log.info("=========================");
    }
}