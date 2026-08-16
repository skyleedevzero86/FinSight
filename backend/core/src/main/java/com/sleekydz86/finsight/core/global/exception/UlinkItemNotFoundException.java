package com.sleekydz86.finsight.core.global.exception;

public class UlinkItemNotFoundException extends BaseException {
    public UlinkItemNotFoundException(String id) {
        super("통합링크 항목을 찾을 수 없습니다. ID: " + id, "ULINK_001", "ULINK", 404);
    }
}
