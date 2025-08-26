package com.sleekydz86.finsight.core.global.exception;

public class DatabaseConnectionException extends BaseException {
    private final String databaseType;

    public DatabaseConnectionException(String databaseType, String reason) {
        super("데이터베이스 연결에 실패했습니다. 타입: " + databaseType + ", 사유: " + reason,
                "SYS_002", "Database Connection Error", 503);
        this.databaseType = databaseType;
    }

    public String getDatabaseType() {
        return databaseType;
    }
}