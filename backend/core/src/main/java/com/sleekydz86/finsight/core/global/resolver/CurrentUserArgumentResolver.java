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

    /**
     * Creates a resolver for loading authenticated users from the current request.
     *
     * @param userPersistencePort the port used to retrieve users by email
     * @param jwtTokenUtil        the utility used to validate and parse bearer access tokens
     */
    public CurrentUserArgumentResolver(UserPersistencePort userPersistencePort, JwtTokenUtil jwtTokenUtil) {
        this.userPersistencePort = userPersistencePort;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Determines whether the parameter is supported as a current authenticated user.
     *
     * @param parameter the method parameter to inspect
     * @return {@code true} if the parameter has the {@link CurrentUser} annotation and
     *         is typed as {@link AuthenticatedUser}, {@code false} otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    /**
     * Resolves the current user from the request authentication context.
     *
     * @param parameter    the controller method parameter annotated with {@code CurrentUser}
     * @param mavContainer the model and view container
     * @param webRequest   the current web request
     * @param binderFactory the web data binder factory
     * @return an {@code AuthenticatedUser} containing the authenticated user's email when authentication is optional, or the user's complete profile when authentication is required; {@code null} when the annotation is absent or optional authentication has no valid email
     */
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

        if (!currentUserAnnotation.required()) {
            return AuthenticatedUser.builder()
                    .email(email)
                    .build();
        }

        try {
            Optional<User> userOpt = userPersistencePort.findByEmail(email);
            if (userOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }

            User user = userOpt.get();
            return AuthenticatedUser.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname() != null ? user.getNickname() : user.getUsername())
                    .role(user.getRole().name())
                    .authProvider(user.getAuthProvider() != null
                            ? user.getAuthProvider()
                            : com.sleekydz86.finsight.core.user.domain.AuthProvider.WEB)
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Error resolving current user for email: {}", email, e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    /**
     * Resolves the current user's email from the authenticated principal or a bearer token.
     *
     * @param webRequest the request containing the optional Authorization header
     * @return the resolved email, or {@code null} when no valid authentication is available
     */
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

    /**
     * Determines whether the authentication represents an authenticated, non-anonymous principal.
     *
     * @param authentication the authentication to evaluate
     * @return {@code true} if the authentication is valid and non-anonymous, {@code false} otherwise
     */
    private boolean isAuthenticatedPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return !(authentication instanceof AnonymousAuthenticationToken);
    }
}
