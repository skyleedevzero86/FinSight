package com.sleekydz86.finsight.web.news;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsRepositoryImpl;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewsCrawlMysqlVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlMysqlVerificationTest.class);
    private static final String DUP_SLASH = "https://news.example/dup/";
    private static final String DUP_PLAIN = "https://news.example/dup";
    private static final String KEPT_OTHER = "https://news.example/keep";
    private static final String SHARED = "https://news.example/race";
    private static final String OWN_A = "https://news.example/race-a";
    private static final String OWN_B = "https://news.example/race-b";
    private static final String RESTART = "https://news.example/restart";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    private static JdbcTemplate jdbc;
    private static SelectGateDataSource gated;
    private static AnnotationConfigApplicationContext context;
    private static NewsRepositoryImpl newsRepository;
    private static long keptId;
    private static long droppedId;

    @BeforeAll
    static void schema() throws Exception {
        DriverManagerDataSource raw = new DriverManagerDataSource();
        raw.setUrl(MYSQL.getJdbcUrl());
        raw.setUsername(MYSQL.getUsername());
        raw.setPassword(MYSQL.getPassword());
        raw.setDriverClassName(MYSQL.getDriverClassName());
        jdbc = new JdbcTemplate(raw);
        gated = new SelectGateDataSource(raw);
        try (Connection connection = raw.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(schemaSql().getBytes()));
        }
        AnnotationConfigApplicationContext started = new AnnotationConfigApplicationContext();
        started.registerBean(DataSource.class, () -> gated);
        started.register(NewsMysqlSlice.class);
        started.refresh();
        context = started;
        newsRepository = started.getBean(NewsRepositoryImpl.class);
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("V40은 중복 주소를 합치고 자식을 옮긴다")
    @Order(1)
    void v40CollapsesDuplicateUrlsAndMovesChildren() throws Exception {
        // given
        keptId = insertNews(DUP_SLASH, 2);
        droppedId = insertNews(DUP_PLAIN, 5);
        long otherId = insertNews(KEPT_OTHER, 1);

        // when
        seedChildren(keptId, droppedId);

        // given
        Path directory = Files.createTempDirectory("v40-only");
        try (InputStream input = new ClassPathResource("db/migration/V40__news_source_url_unique.sql").getInputStream()) {
            Files.copy(input, directory.resolve("V40__news_source_url_unique.sql"));
        }
        String location = "filesystem:" + directory.toAbsolutePath().toString().replace('\\', '/');

        // when
        Flyway.configure()
                .dataSource(jdbc.getDataSource())
                .locations(location)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();

        // then
        assertThat(count("select count(*) from news")).isEqualTo(2);
        assertThat(count("select count(*) from news where id = ?", droppedId)).isZero();
        assertThat(text("select source_url from news where id = ?", keptId)).isEqualTo(DUP_PLAIN);
        assertThat(integer("select view_count from news where id = ?", keptId)).isEqualTo(7);
        assertThat(count("select count(*) from news where id = ?", otherId)).isEqualTo(1);
        assertThat(count("select count(*) from news_target_categories where news_id = ?", keptId)).isEqualTo(2);
        assertThat(count("select count(*) from news_target_categories where news_id = ?", droppedId)).isZero();
        assertThat(count("select count(*) from news_reactions where news_id = ?", keptId)).isEqualTo(2);
        assertThat(count("select count(*) from news_reactions where news_id = ?", droppedId)).isZero();
        assertThat(integer("select like_count from news_statistics where news_id = ?", keptId)).isEqualTo(22);
        assertThat(count("select count(*) from news_statistics where news_id = ?", droppedId)).isZero();
        assertThat(longValue("select target_id from comments where comment_type = 'NEWS'")).isEqualTo(keptId);
        assertThat(longValue("select target_id from comments where comment_type = 'BOARD'")).isEqualTo(droppedId);
        assertThat(longValue("select news_id from notifications")).isEqualTo(keptId);
        assertThat(count("""
                select count(*) from information_schema.TABLE_CONSTRAINTS
                where TABLE_SCHEMA = DATABASE() and TABLE_NAME = 'news'
                  and CONSTRAINT_NAME = 'uk_news_source_url' and CONSTRAINT_TYPE = 'UNIQUE'
                """)).isEqualTo(1);
        assertThat(integer("select success from flyway_schema_history where version = '40'")).isEqualTo(1);
        assertThatThrownBy(() -> insertNews(DUP_PLAIN, 1))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);

        // given
        String collation = text("select @@collation_database");

        // when
        boolean caseInsensitive = collation.endsWith("_ci");

        // given
        boolean secondInserted = insertCaseVariant();

        // when
        log.info("V40 검증 collation={} 대소문자 두 번째 행 삽입={}", collation, secondInserted);

        // then
        assertThat(secondInserted).isEqualTo(!caseInsensitive);
    }

    @Test
    @DisplayName("동시 저장에서 유니크 충돌이 나도 다른 기사는 남긴다")
    @Order(2)
    void concurrentSaveKeepsSiblingWhenUniqueConflicts() throws Exception {
        // when
        gated.arm(2);

        // given
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // then
        try {
            Future<List<String>> first = pool.submit(save(SHARED, OWN_A));
            Future<List<String>> second = pool.submit(save(SHARED, OWN_B));
            List<String> saved = new ArrayList<>();
            saved.addAll(first.get());
            saved.addAll(second.get());
            assertThat(saved).contains(OWN_A, OWN_B);
            assertThat(saved.stream().filter(SHARED::equals).count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            gated.disarm();
        }
        assertThat(count("select count(*) from news where source_url = ?", SHARED)).isEqualTo(1);
        assertThat(count("select count(*) from news where source_url = ?", OWN_A)).isEqualTo(1);
        assertThat(count("select count(*) from news where source_url = ?", OWN_B)).isEqualTo(1);

        // when
        log.info("동시 저장 검증 shared=1 ownA=1 ownB=1");
    }

    @Test
    @DisplayName("실패한 단계를 다시 실행해도 커밋된 뉴스를 중복 저장하지 않는다")
    @Order(3)
    void failedStepRestartKeepsCommittedNewsWithoutDuplicate() throws Exception {
        // given
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
        }

        // when
        PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);

        // given
        JobRepository repository = jobRepository();
        AtomicBoolean failed = new AtomicBoolean(false);

        // when
        Tasklet tasklet = (StepContribution contribution, ChunkContext chunkContext) -> {
            newsRepository.saveAllNews(List.of(article(RESTART)));
            if (failed.compareAndSet(false, true)) {
                throw new IllegalStateException("재시작 검증을 위해 첫 실행을 실패시킵니다");
            }
            return RepeatStatus.FINISHED;
        }
;

        // given
        Job job = new JobBuilder("newsSaveRestartJob", repository)
                .start(new StepBuilder("saveStep", repository).tasklet(tasklet, transactionManager).build())
                .build();
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();

        // when
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(Runnable::run);
        launcher.afterPropertiesSet();

        // given
        JobParameters parameters = new JobParametersBuilder().addLong("run", 1L).toJobParameters();

        // when
        JobExecution first = launcher.run(job, parameters);

        // given
        int afterFailure = count("select count(*) from news where source_url = ?", RESTART);

        // when
        JobExecution second = launcher.run(job, parameters);

        // given
        int afterRestart = count("select count(*) from news where source_url = ?", RESTART);

        // when
        log.info("배치 재시작 검증 first={} rowsAfterFailure={} second={} rowsAfterRestart={}",
                first.getStatus(), afterFailure, second.getStatus(), afterRestart);

        // then
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(afterFailure).as(String.valueOf(first.getAllFailureExceptions())).isEqualTo(1);
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(afterRestart).isEqualTo(1);
    }

    private static Callable<List<String>> save(String shared, String own) {
        return () -> {
            List<News> stored = newsRepository.saveAllNews(List.of(article(shared), article(own))).getNewses();
            return stored.stream().map(news -> news.getNewsMeta().getSourceUrl()).toList();
        };
    }

    private static News article(String url) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, LocalDateTime.parse("2026-10-12T00:00:00"), url),
                new Content("제목", "본문"));
    }

    private static long insertNews(String url, int viewCount) {
        jdbc.update("""
                insert into news (
                    news_provider, news_published_time, source_url, scraped_time,
                    original_title, original_content, view_count, created_at, updated_at,
                    ai_sentiment_attempts, ai_embedding_attempts)
                values ('YAHOO_FINANCE', '2026-10-11 00:00:00', ?, '2026-10-11 00:00:00',
                        '기존', '본문', ?, now(6), now(6), 0, 0)
                """, url, viewCount);
        return jdbc.queryForObject("select id from news where source_url = ? order by id desc limit 1", Long.class, url);
    }

    private static boolean insertCaseVariant() {
        try {
            insertNews("https://news.example/Case", 1);
            insertNews("https://news.example/case", 1);
            return true;
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            return false;
        }
    }

    private static void seedChildren(long keep, long drop) {
        jdbc.update("insert into news_target_categories (news_id, category) values (?, 'SPY')", keep);
        jdbc.update("insert into news_target_categories (news_id, category) values (?, 'SPY')", drop);
        jdbc.update("insert into news_target_categories (news_id, category) values (?, 'QQQ')", drop);
        jdbc.update("""
                insert into news_reactions (news_id, user_email, reaction_type, created_at)
                values (?, 'same@example.com', 'LIKE', now(6))
                """, keep);
        jdbc.update("""
                insert into news_reactions (news_id, user_email, reaction_type, created_at)
                values (?, 'same@example.com', 'LIKE', now(6))
                """, drop);
        jdbc.update("""
                insert into news_reactions (news_id, user_email, reaction_type, created_at)
                values (?, 'only-drop@example.com', 'DISLIKE', now(6))
                """, drop);
        jdbc.update("""
                insert into news_statistics (news_id, view_count, like_count, dislike_count, comment_count, created_at)
                values (?, 1, 2, 3, 4, now(6))
                """, keep);
        jdbc.update("""
                insert into news_statistics (news_id, view_count, like_count, dislike_count, comment_count, created_at)
                values (?, 10, 20, 30, 40, now(6))
                """, drop);
        jdbc.update("""
                insert into comments (content, author_email, comment_type, target_id, status, like_count, dislike_count, report_count, created_at)
                values ('뉴스댓글', 'reader@example.com', 'NEWS', ?, 'ACTIVE', 0, 0, 0, now(6))
                """, drop);
        jdbc.update("""
                insert into comments (content, author_email, comment_type, target_id, status, like_count, dislike_count, report_count, created_at)
                values ('다른댓글', 'reader@example.com', 'BOARD', ?, 'ACTIVE', 0, 0, 0, now(6))
                """, drop);
        jdbc.update("""
                insert into notifications (user_id, news_id, type, title, status, channel, priority, created_at)
                values (1, ?, 'NEWS', '알림', 'SENT', 'PUSH', 'NORMAL', now(6))
                """, drop);
    }

    private static JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(jdbc.getDataSource());
        factory.setTransactionManager(new DataSourceTransactionManager(jdbc.getDataSource()));
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    private static int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static int integer(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static long longValue(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private static String text(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }

    private static String schemaSql() {
        return """
                create table news (
                    id bigint not null auto_increment primary key,
                    news_provider varchar(32) not null,
                    news_published_time datetime(6) not null,
                    source_url varchar(500) not null,
                    image_url varchar(1000) null,
                    scraped_time datetime(6) not null,
                    original_title varchar(500) not null,
                    original_content text not null,
                    ai_translated_title text null,
                    ai_translated_content text null,
                    ai_overview text null,
                    ai_sentiment_type varchar(32) null,
                    ai_sentiment_score double null,
                    ai_sentiment_status varchar(16) null,
                    ai_sentiment_confidence double null,
                    ai_sentiment_positive double null,
                    ai_sentiment_neutral double null,
                    ai_sentiment_negative double null,
                    ai_sentiment_model varchar(200) null,
                    ai_sentiment_attempts int not null default 0,
                    ai_embedding_status varchar(16) null,
                    ai_embedding_model varchar(200) null,
                    ai_embedding_attempts int not null default 0,
                    view_count int not null default 0,
                    created_at datetime(6) not null,
                    updated_at datetime(6) null
                );
                create table news_target_categories (
                    news_id bigint not null,
                    category varchar(32) not null,
                    primary key (news_id, category)
                );
                create table news_reactions (
                    id bigint not null auto_increment primary key,
                    news_id bigint not null,
                    user_email varchar(255) not null,
                    reaction_type varchar(16) not null,
                    created_at datetime(6) not null,
                    updated_at datetime(6) null,
                    unique key uk_news_rxn_user_news (user_email, news_id)
                );
                create table news_statistics (
                    id bigint not null auto_increment primary key,
                    news_id bigint not null,
                    view_count int not null,
                    like_count int not null,
                    dislike_count int not null,
                    comment_count int not null,
                    created_at datetime(6) null,
                    updated_at datetime(6) null
                );
                create table comments (
                    id bigint not null auto_increment primary key,
                    content text not null,
                    author_email varchar(255) not null,
                    comment_type varchar(32) not null,
                    target_id bigint not null,
                    status varchar(32) not null,
                    like_count int not null,
                    dislike_count int not null,
                    report_count int not null,
                    created_at datetime(6) null,
                    updated_at datetime(6) null
                );
                create table notifications (
                    id bigint not null auto_increment primary key,
                    user_id bigint not null,
                    news_id bigint null,
                    type varchar(64) not null,
                    title varchar(500) not null,
                    status varchar(32) not null,
                    channel varchar(32) not null,
                    priority varchar(32) not null,
                    created_at datetime(6) null,
                    updated_at datetime(6) null
                );
                """;
    }
}
