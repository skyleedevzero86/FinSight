package com.sleekydz86.finsight.core.global.security;

import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityAccess {

    private SecurityAccess() {
    }

    public static boolean isAdminOrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role)) {
                return true;
            }
        }
        return false;
    }

    public static void requireAdminOrManager() {
        if (!isAdminOrManager()) {
            throw new InsufficientPermissionException("관리자 권한이 필요합니다");
        }
    }
}
