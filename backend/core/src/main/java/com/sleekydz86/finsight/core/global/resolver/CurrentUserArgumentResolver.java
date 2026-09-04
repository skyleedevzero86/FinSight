package com.sleekydz86.finsight.core.global.resolver;

import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserArgumentResolver.class);

    private final UserPersistencePort userPersistencePort;
    private final JwtTokenUtil jwtTokenUtil;

    public CurrentUserArgumentResolver(
            UserPersistencePort userPersistencePort,
            JwtTokenUtil jwtTokenUtil) {
        this.userPersistencePort = userPersistencePort;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        CurrentUser currentUserAnnotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (currentUserAnnotation == null) {
            return null;
        }

        String email = resolveEmail(webRequest);
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            if (currentUserAnnotation.required()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }
            return null;
        }

        try {
            Optional<User> userOpt = userPersistencePort.findByEmail(email)
                    .or(() -> userPersistencePort.findByUsername(email));
            if (userOpt.isEmpty()) {
                if (currentUserAnnotation.required()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
                }
                return null;
            }

            User user = userOpt.get();
            String role = user.getRole() != null ? user.getRole().name() : "USER";
            return AuthenticatedUser.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                    .role(role)
                    .authProvider(user.getAuthProvider() != null
                            ? user.getAuthProvider()
                            : com.sleekydz86.finsight.core.user.domain.AuthProvider.WEB)
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("현재 사용자 정보를 확인하지 못했습니다: email={}", email, e);
            if (currentUserAnnotation.required()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }
            return null;
        }
    }

    private String resolveEmail(NativeWebRequest webRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticatedPrincipal(authentication)) {
            String name = authentication.getName();
            if (StringUtils.hasText(name) && !"anonymousUser".equalsIgnoreCase(name)) {
                return name;
            }
        }

        String bearer = webRequest.getHeader("Authorization");
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            return null;
        }
        String token = bearer.substring(7).trim();
        if (!StringUtils.hasText(token) || !jwtTokenUtil.validateToken(token)) {
            return null;
        }
        String tokenType = jwtTokenUtil.getTokenType(token);
        if (tokenType != null && !"ACCESS".equals(tokenType)) {
            return null;
        }
        return jwtTokenUtil.getEmailFromToken(token);
    }

    private boolean isAuthenticatedPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return !(authentication instanceof AnonymousAuthenticationToken);
    }
}
