package com.sleekydz86.finsight.core.config;

import com.sleekydz86.finsight.core.news.service.*;
import com.sleekydz86.finsight.core.board.service.*;
import com.sleekydz86.finsight.core.comment.service.*;
import com.sleekydz86.finsight.core.user.service.*;
import com.sleekydz86.finsight.core.auth.service.*;
import com.sleekydz86.finsight.core.health.service.*;
import com.sleekydz86.finsight.core.notification.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AdvancedDependencyInjectionConfig {

    @Bean
    @Primary
    public NewsQueryService newsQueryService() {
        return new NewsQueryService();
    }

    @Bean
    @Primary
    public NewsCommandService newsCommandService() {
        return new NewsCommandService();
    }

    @Bean
    @Primary
    public BoardQueryService boardQueryService() {
        return new BoardQueryService();
    }

    @Bean
    @Primary
    public BoardCommandService boardCommandService() {
        return new BoardCommandService();
    }

    @Bean
    @Primary
    public CommentQueryService commentQueryService() {
        return new CommentQueryService();
    }

    @Bean
    @Primary
    public CommentCommandService commentCommandService() {
        return new CommentCommandService();
    }

    @Bean
    @Primary
    public UserService userService() {
        return new UserService();
    }

    @Bean
    @Primary
    public AuthenticationService authenticationService() {
        return new AuthenticationService();
    }

    @Bean
    @Primary
    public HealthQueryService healthQueryService() {
        return new HealthQueryService();
    }

    @Bean
    @Primary
    public HealthCommandService healthCommandService() {
        return new HealthCommandService();
    }

    @Bean
    @Primary
    public NotificationService notificationService() {
        return new NotificationService();
    }
}