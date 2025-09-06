package com.sleekydz86.finsight.core.config;

import com.sleekydz86.finsight.core.auth.service.AuthenticationService;
import com.sleekydz86.finsight.core.board.service.BoardCommandService;
import com.sleekydz86.finsight.core.board.service.BoardQueryService;
import com.sleekydz86.finsight.core.comment.service.CommentCommandService;
import com.sleekydz86.finsight.core.comment.service.CommentQueryService;
import com.sleekydz86.finsight.core.news.service.NewsQueryService;
import com.sleekydz86.finsight.core.user.service.UserService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.sleekydz86.finsight.core")
@EnableJpaRepositories(basePackages = "com.sleekydz86.finsight.core")
public class CoreConfig {
}