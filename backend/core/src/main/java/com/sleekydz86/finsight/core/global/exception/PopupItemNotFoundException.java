package com.sleekydz86.finsight.core.global.exception;

public class PopupItemNotFoundException extends BaseException {
    public PopupItemNotFoundException(String id) {
        super("팝업 항목을 찾을 수 없습니다. ID: " + id, "POPUP_001", "POPUP", 404);
    }
}
