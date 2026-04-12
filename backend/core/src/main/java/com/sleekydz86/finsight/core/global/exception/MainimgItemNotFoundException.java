package com.sleekydz86.finsight.core.global.exception;

public class MainimgItemNotFoundException extends BaseException {
    public MainimgItemNotFoundException(String id) {
        super("메인이미지 항목을 찾을 수 없습니다. ID: " + id, "MAINIMG_001", "MAINIMG", 404);
    }
}
