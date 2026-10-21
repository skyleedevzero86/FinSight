package com.sleekydz86.finsight.core.economypick.domain;

import com.sleekydz86.finsight.core.economypick.domain.EconomyPickSales.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SalesMixMath {

    private record Book(String headline, String[] labels, int[] weights) {
    }

    private static final Book SPACE = new Book(
            "우주관광, 발사, 정부 수주 순으로 매출이 나뉩니다.",
            new String[]{"우주관광", "발사 서비스", "정부·위성 수주", "기타"},
            new int[]{28, 34, 26, 12});
    private static final Book DEFENSE = new Book(
            "항공체계와 미사일 비중이 큰 방산 매출로 추정합니다.",
            new String[]{"항공체계", "미사일·화력", "우주·위성", "정비 서비스"},
            new int[]{32, 28, 22, 18});
    private static final Book CHIP = new Book(
            "데이터센터 수요가 매출의 중심으로 추정됩니다.",
            new String[]{"데이터센터", "소비자 기기", "자동차·산업", "기타"},
            new int[]{46, 22, 18, 14});
    private static final Book FINANCE = new Book(
            "이자이익과 수수료가 매출의 대부분으로 추정됩니다.",
            new String[]{"이자이익", "수수료", "트레이딩", "기타"},
            new int[]{44, 28, 16, 12});
    private static final Book ENERGY = new Book(
            "원유·가스 생산이 매출의 중심입니다.",
            new String[]{"원유·가스", "정제", "화학", "기타"},
            new int[]{48, 24, 16, 12});
    private static final Book DISPLAY = new Book(
            "TV·모니터, 메모리, OLED 순으로 매출이 나뉩니다.",
            new String[]{"TV, 모니터 [DX부문]", "DRAM, NAND [DS부문]", "스마트폰용 OLED [SDC]", "부문간 내부거래 [기타]"},
            new int[]{66, 26, 12, 9});
    private static final Book DEFAULT = new Book(
            "주력 제품과 용역 중심으로 매출이 나뉩니다.",
            new String[]{"주력 제품", "용역", "수출", "기타"},
            new int[]{48, 27, 18, 7});

    private SalesMixMath() {
    }

    public static EconomyPickSales predict(String name, String symbol) {
        String label = blank(name, symbol);
        Book book = book(fold(label));
        int[] weights = normalize(tilt(book.weights, fold(symbol)));
        return new EconomyPickSales(blank(symbol, ""), label, note(label, book.headline), slices(book.labels, weights));
    }

    private static Book book(String text) {
        if (hits(text, "버진", "갤럭틱", "spce", "우주", "로켓", "rklb", "asts", "lunr", "astr", "spcx", "arkx", "rdw")) {
            return SPACE;
        }
        if (hits(text, "방산", "록히드", "lmt", "noc", "rtx", "boeing", "보잉", "레이시온")) {
            return DEFENSE;
        }
        if (hits(text, "반도체", "엔비디아", "nvda", "하이닉스", "mu", "tsm", "amd", "avgo", "asml", "qcom")) {
            return CHIP;
        }
        if (hits(text, "금융", "은행", "jpm", "모건")) {
            return FINANCE;
        }
        if (hits(text, "석유", "xom", "엑슨")) {
            return ENERGY;
        }
        if (hits(text, "삼성", "005930", "samsung")) {
            return DISPLAY;
        }
        return DEFAULT;
    }

    private static int[] tilt(int[] base, String symbol) {
        int[] copy = base.clone();
        int shift = Math.floorMod(symbol.hashCode(), 7) - 3;
        copy[0] = Math.max(8, copy[0] + shift);
        copy[copy.length - 1] = Math.max(4, copy[copy.length - 1] - shift);
        return copy;
    }

    private static int[] normalize(int[] weights) {
        int sum = 0;
        for (int weight : weights) {
            sum += Math.max(weight, 0);
        }
        int[] scaled = new int[weights.length];
        int used = 0;
        for (int index = 0; index < weights.length - 1; index++) {
            scaled[index] = (int) Math.round(Math.max(weights[index], 0) * 100.0 / sum);
            used += scaled[index];
        }
        scaled[weights.length - 1] = Math.max(0, 100 - used);
        return scaled;
    }

    private static List<Slice> slices(String[] labels, int[] weights) {
        List<Slice> rows = new ArrayList<>();
        for (int index = 0; index < labels.length; index++) {
            rows.add(new Slice(labels[index], weights[index], weights[index] < 0 ? "minus" : "plus"));
        }
        return List.copyOf(rows);
    }

    private static String note(String name, String headline) {
        return name + " 선택 시 " + headline;
    }

    private static boolean hits(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static String fold(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String blank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
