package com.sleekydz86.finsight.web.news;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlLockStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class NewsCrawlIncidentMysqlTest {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlIncidentMysqlTest.class);
    private static final String MIGRATION = "finsight_tables.sql";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    private static JdbcTemplate jdbc;
    private static NewsCrawlLockStore locks;

    @BeforeAll
    static void migrate() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());
        dataSource.setDriverClassName(MYSQL.getDriverClassName());
        jdbc = new JdbcTemplate(dataSource);
        Path source = Path.of("src/main/resources/db/migration").resolve(MIGRATION);
        if (!java.nio.file.Files.exists(source)) {
            source = Path.of("web/src/main/resources/db/migration").resolve(MIGRATION);
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(source));
        }
        log.info("뉴스 수집 장애 테이블을 적용했습니다");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(NewsCrawlLockMysqlSlice.class);
        context.refresh();
        locks = context.getBean(NewsCrawlLockStore.class);
    }

    @Test
    @DisplayName("장애 사건과 실행 시도 잠금 테이블을 만든다")
    void createsIncidentAttemptAndLockTables() {
        // given

        // then
        assertThat(comment("news_crawl_incident", "cause")).contains("HTTP_429");
        assertThat(comment("news_crawl_attempt", "status")).contains("SKIPPED");
        assertThat(comment("news_crawl_provider_lock", "fencing_token")).contains("펜싱 토큰");
        assertThat(tableComment("news_crawl_incident")).contains("장애 사건");

        // when
        log.info("뉴스 수집 장애 사건, 실행 시도, 제공처 잠금 테이블을 확인했습니다");
    }

    @Test
    @DisplayName("같은 제공처를 두 소유자가 동시에 선점하면 이전 토큰은 해제되지 않는다")
    void previousTokenCannotReleaseWhenTwoOwnersAcquireTheSameProvider() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 10, 16, 0, 0);

        // when
        jdbc.update("""
                insert into news_crawl_provider_lock (provider, owner, fencing_token, expires_at)
                values ('MARKETAUX', 'seed', 1, '2000-01-01 00:00:00.000000')
                """);

        // given
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // then
        try {
            Future<Optional<Long>> batch = pool.submit(acquire("batch-mysql", now, start));
            Future<Optional<Long>> manual = pool.submit(acquire("manual-mysql", now, start));
            start.countDown();
            List<Optional<Long>> results = List.of(batch.get(), manual.get());
            List<Optional<Long>> won = results.stream().filter(Optional::isPresent).toList();
            assertThat(won).hasSize(1);
            String owner = text("select owner from news_crawl_provider_lock where provider = 'MARKETAUX'");
            long token = longValue("select fencing_token from news_crawl_provider_lock where provider = 'MARKETAUX'");
            String loser = "batch-mysql".equals(owner) ? "manual-mysql" : "batch-mysql";
            boolean loserReleased = locks.release(NewsProvider.MARKETAUX, loser, token);
            if (loserReleased) {
                log.error("잠금을 얻지 못한 소유자가 펜싱 토큰을 해제했습니다 owner={} token={}", loser, token);
            } else {
                log.info("잠금을 얻지 못한 소유자의 해제를 거부했습니다 owner={} token={}", loser, token);
            }
            assertThat(loserReleased).isFalse();

            jdbc.update("update news_crawl_provider_lock set expires_at = '2000-01-01 00:00:00' where provider = 'MARKETAUX'");
            Optional<Long> stolen = locks.acquire(
                    NewsProvider.MARKETAUX, "agent-mysql", now.plusMinutes(10), now.plusMinutes(5));
            assertThat(stolen).isPresent();
            assertThat(stolen.get()).isNotEqualTo(token);
            boolean previousReleased = locks.release(NewsProvider.MARKETAUX, owner, token);
            if (previousReleased) {
                log.error("만료된 이전 소유자가 펜싱 토큰을 해제했습니다 owner={} token={}", owner, token);
            } else {
                log.info("만료된 이전 소유자의 해제를 거부했습니다 owner={} token={}", owner, token);
            }
            assertThat(previousReleased).isFalse();
            assertThat(locks.held(NewsProvider.MARKETAUX, "agent-mysql", stolen.get(), now.plusMinutes(5))).isTrue();
            log.info("새 소유자가 잠금을 유지합니다 이전소유자={} 이전토큰={} 새토큰={}", owner, token, stolen.get());
        } finally {
            pool.shutdownNow();
        }
    }

    private static Callable<Optional<Long>> acquire(String owner, LocalDateTime now, CountDownLatch start) {
        return () -> {
            start.await();
            return locks.acquire(NewsProvider.MARKETAUX, owner, now.plusMinutes(2), now);
        };
    }

    private static int integer(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private static long longValue(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private static String text(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private static String comment(String table, String column) {
        return jdbc.queryForObject("""
                select COLUMN_COMMENT from information_schema.COLUMNS
                where TABLE_SCHEMA = DATABASE() and TABLE_NAME = ? and COLUMN_NAME = ?
                """, String.class, table, column);
    }

    private static String tableComment(String table) {
        return jdbc.queryForObject("""
                select TABLE_COMMENT from information_schema.TABLES
                where TABLE_SCHEMA = DATABASE() and TABLE_NAME = ?
                """, String.class, table);
    }
}
