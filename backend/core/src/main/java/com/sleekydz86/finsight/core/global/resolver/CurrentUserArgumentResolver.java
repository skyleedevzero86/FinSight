package com.sleekydz86.finsight.core.global.resolver;

import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Arrays;
import java.util.Optional;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserArgumentResolver.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final UserJpaRepository userJpaRepository;

    public CurrentUserArgumentResolver(JwtTokenUtil jwtTokenUtil, UserJpaRepository userJpaRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userJpaRepository = userJpaRepository;
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
        boolean required = currentUserAnnotation != null ? currentUserAnnotation.required() : true;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            log.debug("SecurityContext에서 인증 정보 확인 - principal: {}", authentication.getName());

            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                String token = extractTokenFromRequest(request);
                if (token != null) {
                    try {
                        if (!jwtTokenUtil.validateToken(token)) {
                            log.warn("Invalid token provided for CurrentUser resolution");
                            return required ? null : AuthenticatedUser.system();
                        }

                        String tokenType = jwtTokenUtil.getTokenType(token);
                        if (!"ACCESS".equals(tokenType)) {
                            log.warn("Invalid token type for CurrentUser: {}", tokenType);
                            return required ? null : AuthenticatedUser.system();
                        }

                        String email = jwtTokenUtil.getEmailFromToken(token);
                        UserRole role = jwtTokenUtil.getRoleFromToken(token);

                        Optional<User> userOpt = userJpaRepository.findByEmail(email);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            AuthenticatedUser authenticatedUser = AuthenticatedUser.of(
                                    user.getId(), user.getEmail(), user.getUsername(), user.getRole());

                            log.debug("CurrentUser 생성 완료 - id: {}, email: {}",
                                    authenticatedUser.getId(), authenticatedUser.getEmail());
                            return authenticatedUser;
                        } else {
                            log.warn("사용자를 찾을 수 없음 - email: {}", email);
                            return required ? null : AuthenticatedUser.system();
                        }

                    } catch (Exception e) {
                        log.error("토큰에서 사용자 정보 추출 실패: {}", e.getMessage());
                        return required ? null : AuthenticatedUser.system();
                    }
                }
            }
        }

        log.warn("CurrentUser 정보를 생성할 수 없음 - 인증되지 않은 요청");
        return required ? null : AuthenticatedUser.system();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> tokenCookie = Arrays.stream(cookies)
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst();

            if (tokenCookie.isPresent()) {
                return tokenCookie.get().getValue();
            }
        }

        return null;
    }
}