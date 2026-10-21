package com.sleekydz86.finsight.web.news;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaMapper;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackageClasses = NewsJpaRepository.class,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NewsJpaRepository.class))
class NewsMysqlSlice {

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setManagedTypes(PersistenceManagedTypes.of(NewsJpaEntity.class.getName()));
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaProperties(jpaProperties());
        return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    NewsJpaMapper newsJpaMapper() {
        return new NewsJpaMapper();
    }

    @Bean
    NewsRepositoryImpl newsRepositoryImpl(NewsJpaMapper newsJpaMapper,
                                          NewsJpaRepository newsJpaRepository,
                                          JPAQueryFactory jpaQueryFactory,
                                          PlatformTransactionManager transactionManager) {
        return new NewsRepositoryImpl(newsJpaMapper, newsJpaRepository, jpaQueryFactory, transactionManager);
    }

    private static Properties jpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.mapping.default_list_semantics", "BAG");
        return properties;
    }
}
