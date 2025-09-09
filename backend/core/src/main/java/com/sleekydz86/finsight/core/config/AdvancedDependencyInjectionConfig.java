package com.sleekydz86.finsight.core.config;

import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.service.NewsQueryService;
import com.sleekydz86.finsight.core.news.service.NewsCommandService;
import com.sleekydz86.finsight.core.news.service.NewsScrapService;
import com.sleekydz86.finsight.core.news.service.NewsAiProcessingService;
import com.sleekydz86.finsight.core.news.service.NewsPersistenceService;
import com.sleekydz86.finsight.core.news.service.NewsNotificationService;
import com.sleekydz86.finsight.core.news.service.PersonalizedNewsService;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReportPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardFilePersistencePort;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.board.service.BoardCommandService;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReportPersistencePort;
import com.sleekydz86.finsight.core.comment.service.CommentQueryService;
import com.sleekydz86.finsight.core.comment.service.CommentCommandService;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.core.user.service.PasswordValidationService;
import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.health.domain.port.out.ExternalHealthCheckPort;
import com.sleekydz86.finsight.core.health.domain.port.out.HealthPersistencePort;
import com.sleekydz86.finsight.core.health.service.HealthQueryService;
import com.sleekydz86.finsight.core.health.service.HealthCommandService;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import com.sleekydz86.finsight.core.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Executor;

@Configuration
public class AdvancedDependencyInjectionConfig {

    private final NewsPersistencePort newsPersistencePort;
    private final NewsStatisticsPersistencePort newsStatisticsPersistencePort;
    private final NewsScrapService newsScrapService;
    private final NewsAiProcessingService newsAiProcessingService;
    private final NewsPersistenceService newsPersistenceService;
    private final PersonalizedNewsService personalizedNewsService;
    private final BoardPersistencePort boardPersistencePort;
    private final BoardReactionPersistencePort boardReactionPersistencePort;
    private final BoardScrapPersistencePort boardScrapPersistencePort;
    private final BoardReportPersistencePort boardReportPersistencePort;
    private final BoardFilePersistencePort boardFilePersistencePort;
    private final CommentPersistencePort commentPersistencePort;
    private final CommentReactionPersistencePort commentReactionPersistencePort;
    private final CommentReportPersistencePort commentReportPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final PasswordValidationService passwordValidationService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserJpaRepository userJpaRepository;
    private final ExternalHealthCheckPort externalHealthCheckPort;
    private final HealthPersistencePort healthPersistencePort;
    private final NotificationSenderPort notificationSenderPort;
    private final Executor newsProcessingExecutor;

    public AdvancedDependencyInjectionConfig(
            NewsPersistencePort newsPersistencePort,
            NewsStatisticsPersistencePort newsStatisticsPersistencePort,
            NewsScrapService newsScrapService,
            NewsAiProcessingService newsAiProcessingService,
            NewsPersistenceService newsPersistenceService,
            PersonalizedNewsService personalizedNewsService,
            BoardPersistencePort boardPersistencePort,
            BoardReactionPersistencePort boardReactionPersistencePort,
            BoardScrapPersistencePort boardScrapPersistencePort,
            BoardReportPersistencePort boardReportPersistencePort,
            BoardFilePersistencePort boardFilePersistencePort,
            CommentPersistencePort commentPersistencePort,
            CommentReactionPersistencePort commentReactionPersistencePort,
            CommentReportPersistencePort commentReportPersistencePort,
            UserPersistencePort userPersistencePort,
            PasswordValidationService passwordValidationService,
            AuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil,
            UserJpaRepository userJpaRepository,
            ExternalHealthCheckPort externalHealthCheckPort,
            HealthPersistencePort healthPersistencePort,
            NotificationSenderPort notificationSenderPort,
            Executor newsProcessingExecutor) {
        this.newsPersistencePort = newsPersistencePort;
        this.newsStatisticsPersistencePort = newsStatisticsPersistencePort;
        this.newsScrapService = newsScrapService;
        this.newsAiProcessingService = newsAiProcessingService;
        this.newsPersistenceService = newsPersistenceService;
        this.personalizedNewsService = personalizedNewsService;
        this.boardPersistencePort = boardPersistencePort;
        this.boardReactionPersistencePort = boardReactionPersistencePort;
        this.boardScrapPersistencePort = boardScrapPersistencePort;
        this.boardReportPersistencePort = boardReportPersistencePort;
        this.boardFilePersistencePort = boardFilePersistencePort;
        this.commentPersistencePort = commentPersistencePort;
        this.commentReactionPersistencePort = commentReactionPersistencePort;
        this.commentReportPersistencePort = commentReportPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.passwordValidationService = passwordValidationService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userJpaRepository = userJpaRepository;
        this.externalHealthCheckPort = externalHealthCheckPort;
        this.healthPersistencePort = healthPersistencePort;
        this.notificationSenderPort = notificationSenderPort;
        this.newsProcessingExecutor = newsProcessingExecutor;
    }


    @Bean
    public NewsQueryUseCase newsQueryUseCase() {
        return new NewsQueryService(newsPersistencePort, newsStatisticsPersistencePort,
                personalizedNewsService, userJpaRepository);
    }

    @Bean
    public NewsCommandUseCase newsCommandUseCase(@Lazy NewsNotificationService newsNotificationService) {
        return new NewsCommandService(newsScrapService, newsAiProcessingService, newsPersistenceService,
                newsNotificationService, newsProcessingExecutor);
    }

    @Bean
    @Qualifier("newsQueryService")
    public NewsQueryService newsQueryService() {
        return new NewsQueryService(newsPersistencePort, newsStatisticsPersistencePort,
                personalizedNewsService, userJpaRepository);
    }

    @Bean
    public NewsCommandService newsCommandService(@Lazy NewsNotificationService newsNotificationService) {
        return new NewsCommandService(newsScrapService, newsAiProcessingService, newsPersistenceService,
                newsNotificationService, newsProcessingExecutor);
    }

    @Bean
    public BoardQueryService boardQueryService() {
        return new BoardQueryService(boardPersistencePort, boardReactionPersistencePort, boardScrapPersistencePort);
    }

    @Bean
    public BoardCommandService boardCommandService() {
        return new BoardCommandService(boardPersistencePort, boardReactionPersistencePort, boardReportPersistencePort,
                boardScrapPersistencePort, boardFilePersistencePort);
    }

    @Bean
    public CommentQueryService commentQueryService() {
        return new CommentQueryService(commentPersistencePort, commentReactionPersistencePort,
                commentReportPersistencePort);
    }

    @Bean
    public CommentCommandService commentCommandService() {
        return new CommentCommandService(commentPersistencePort, commentReactionPersistencePort,
                commentReportPersistencePort);
    }

    @Bean
    public UserService userService(PasswordEncoder passwordEncoder) {
        return new UserService(userPersistencePort, passwordEncoder, passwordValidationService);
    }

    @Bean
    public AuthenticationService authenticationService(PasswordEncoder passwordEncoder) {
        return new AuthenticationService(authenticationManager, jwtTokenUtil, userJpaRepository, passwordEncoder,
                passwordValidationService);
    }

    @Bean
    public HealthQueryService healthQueryService() {
        return new HealthQueryService(externalHealthCheckPort);
    }

    @Bean
    public HealthCommandService healthCommandService() {
        return new HealthCommandService(healthPersistencePort);
    }

    @Bean
    public NotificationService notificationService() {
        return new NotificationService(userPersistencePort, notificationSenderPort);
    }
}