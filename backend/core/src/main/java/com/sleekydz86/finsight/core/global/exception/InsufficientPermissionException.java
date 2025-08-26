package com.sleekydz86.finsight.core.global.exception;

public class InsufficientPermissionException extends BaseException {
    private final String requiredRole;
    private final String currentRole;

    public InsufficientPermissionException(String requiredRole, String currentRole) {
        super("이 작업을 수행할 권한이 없습니다. 필요 권한: " + requiredRole + ", 현재 권한: " + currentRole,
                "PERM_001", "Insufficient Permission", 403);
        this.requiredRole = requiredRole;
        this.currentRole = currentRole;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getCurrentRole() {
        return currentRole;
    }
}