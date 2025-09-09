package com.sleekydz86.finsight.core.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    @Value("${redis.connection-timeout:2000}")
    private int connectionTimeout;

    @Value("${redis.command-timeout:2000}")
    private int commandTimeout;

    @Value("${redis.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redis.retry-delay:1000}")
    private int retryDelay;

    @Bean
    @Primary
    public LettuceConnectionFactory lettuceConnectionFactory() {
        try {
            logger.info("Redis 연결 설정 시작 - Host: {}, Port: {}", redisHost, redisPort);

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);

            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                config.setPassword(redisPassword);
            }

            // Lettuce 클라이언트 옵션 설정
            ClientOptions options = ClientOptions.builder()
                    .protocolVersion(ProtocolVersion.RESP2)
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .autoReconnect(true)
                    .build();

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .clientOptions(options)
                    .commandTimeout(Duration.ofMillis(commandTimeout))
                    .shutdownTimeout(Duration.ofMillis(100))
                    .build();

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
            factory.setValidateConnection(true);
            factory.setShareNativeConnection(true);

            logger.info("Redis 연결 팩토리 생성 완료");
            return factory;

        } catch (Exception e) {
            logger.error("Redis 연결 팩토리 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Redis 연결 설정 실패", e);
        }
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // 직렬화 설정
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

            template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();

            logger.info("Redis 템플릿 생성 완료");
            return template;

        } catch (Exception e) {
            logger.error("Redis 템플릿 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Redis 템플릿 설정 실패", e);
        }
    }
}