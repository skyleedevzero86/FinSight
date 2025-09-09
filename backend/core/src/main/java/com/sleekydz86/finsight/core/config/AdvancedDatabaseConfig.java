package com.sleekydz86.finsight.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(basePackages = "com.sleekydz86.finsight.core")
@EnableTransactionManagement
public class AdvancedDatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${hibernate.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("FinSightHikariCP");

        if (isH2Database()) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
        }

        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.sleekydz86.finsight.core");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        vendorAdapter.setGenerateDdl(!"validate".equals(ddlAuto) && !"none".equals(ddlAuto));

        if (isH2Database()) {
            vendorAdapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        } else {
            vendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
        }

        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());

        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);

        if (isH2Database()) {
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        } else {
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        }

        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.default_batch_fetch_size", "100");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        configureCacheSettings(properties);

        return properties;
    }

    private void configureCacheSettings(Properties properties) {
        if (cacheEnabled) {
            try {

                Class.forName("org.hibernate.cache.jcache.JCacheRegionFactory");
                Class.forName("org.ehcache.jsr107.EhcacheCachingProvider");

                properties.setProperty("hibernate.cache.use_second_level_cache", "true");
                properties.setProperty("hibernate.cache.use_query_cache", "true");
                properties.setProperty("hibernate.cache.region.factory_class",
                        "org.hibernate.cache.jcache.JCacheRegionFactory");
                properties.setProperty("hibernate.javax.cache.provider",
                        "org.ehcache.jsr107.EhcacheCachingProvider");
                properties.setProperty("hibernate.javax.cache.uri", "classpath:ehcache.xml");
                properties.setProperty("hibernate.cache.use_minimal_puts", "true");
                properties.setProperty("hibernate.cache.use_structured_entries", "true");
                properties.setProperty("hibernate.cache.default_cache_concurrency_strategy", "read-write");

                System.out.println("✓ Hibernate 2차 캐시 활성화 (JCache + EhCache)");
            } catch (ClassNotFoundException e) {
                System.out.println("⚠ JCache/EhCache 의존성이 없어 2차 캐시를 비활성화합니다");
                properties.setProperty("hibernate.cache.use_second_level_cache", "false");
                properties.setProperty("hibernate.cache.use_query_cache", "false");
            }
        } else {
            properties.setProperty("hibernate.cache.use_second_level_cache", "false");
            properties.setProperty("hibernate.cache.use_query_cache", "false");
            System.out.println("ℹ Hibernate 2차 캐시가 설정으로 비활성화되었습니다");
        }
    }

    private boolean isH2Database() {
        return driverClassName != null && driverClassName.contains("h2");
    }
}