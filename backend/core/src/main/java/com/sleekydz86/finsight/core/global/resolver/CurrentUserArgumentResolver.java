package com.sleekydz86.finsight.core.global.resolver;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaMapper;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserArgumentResolver.class);

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public CurrentUserArgumentResolver(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userJpaMapper = userJpaMapper;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        CurrentUser currentUserAnnotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (currentUserAnnotation == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            if (currentUserAnnotation.required()) {
                throw new IllegalStateException("No authenticated user found");
            }
            return null;
        }

        String email = authentication.getName();
        if (email == null || email.trim().isEmpty()) {
            if (currentUserAnnotation.required()) {
                throw new IllegalStateException("No user email found in authentication");
            }
            return null;
        }

        try {
            Optional<UserJpaEntity> userEntityOpt = userJpaRepository.findByEmail(email);
            if (userEntityOpt.isEmpty()) {
                if (currentUserAnnotation.required()) {
                    throw new IllegalStateException("User not found: " + email);
                }
                return null;
            }

            UserJpaEntity userEntity = userEntityOpt.get();
            User user = userJpaMapper.toDomain(userEntity);

            return AuthenticatedUser.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getUsername())
                    .role(user.getRole().name())
                    .build();

        } catch (Exception e) {
            log.error("Error resolving current user for email: {}", email, e);
            if (currentUserAnnotation.required()) {
                throw new IllegalStateException("Failed to resolve current user", e);
            }
            return null;
        }
    }
}