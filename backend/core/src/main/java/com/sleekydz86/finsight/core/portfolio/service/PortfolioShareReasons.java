package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.global.exception.ValidationException;

import java.util.List;
import java.util.Locale;

public final class PortfolioShareReasons {

    public static final List<String> LABELS = List.of(
            "오픈채팅 URL",
            "과장 수익문구",
            "리딩방 패턴",
            "투자 사기",
            "개인정보",
            "광고");

    private PortfolioShareReasons() {
    }

    public static String require(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("신고 사유를 선택해 주세요.", List.of());
        }
        String trimmed = reason.trim();
        if (!LABELS.contains(trimmed)) {
            throw new ValidationException("신고 사유가 올바르지 않습니다. 입력값: " + reason, List.of());
        }
        return trimmed;
    }

    public static List<String> match(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return LABELS.stream().filter(label -> contains(text, label)).toList();
    }

    private static boolean contains(String text, String label) {
        if ("오픈채팅 URL".equals(label)) {
            return text.contains("open.kakao.com") || text.contains("오픈채팅") || text.contains("오픈 채팅");
        }
        if ("과장 수익문구".equals(label)) {
            return text.contains("무조건") || text.contains("10배") || text.contains("수익 보장") || text.contains("원금 보장");
        }
        if ("리딩방 패턴".equals(label)) {
            return text.contains("리딩방") || text.contains("리딩") || text.contains("단타방");
        }
        if ("투자 사기".equals(label)) {
            return text.contains("투자 사기") || text.contains("사기");
        }
        if ("개인정보".equals(label)) {
            return text.contains("계좌") || text.contains("주민번호") || text.contains("전화번호");
        }
        return text.contains("홍보") || text.contains("광고") || text.contains("가입하면");
    }
}
