package com.sleekydz86.finsight.core.global.exception;

public class InsufficientPermissionException extends BaseException {
    private final String requiredPermission;

    public InsufficientPermissionException(String requiredPermission) {
        super("권한이 부족합니다. 필요한 권한: " + requiredPermission,
                "INSUFFICIENT_PERMISSION",
                "Permission Error",
                403);
        this.requiredPermission = requiredPermission;
    }

    public InsufficientPermissionException(String requiredPermission, String message) {
        super("권한이 부족합니다: " + message + " (필요한 권한: " + requiredPermission + ")",
                "INSUFFICIENT_PERMISSION",
                "Permission Error",
                403);
        this.requiredPermission = requiredPermission;
    }

    public InsufficientPermissionException(String requiredPermission, String message, Throwable cause) {
        super("권한이 부족합니다: " + message + " (필요한 권한: " + requiredPermission + ")",
                "INSUFFICIENT_PERMISSION",
                "Permission Error",
                403,
                cause);
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}