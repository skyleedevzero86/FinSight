package com.sleekydz86.finsight.core.economypick.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TrumpSectorBook {

    public record Sector(String ticker, String name, String sector, String[] words) {
    }

    public record Pick(String ticker, String name, String sector, String action, int confidence, String reason) {
    }

    public static final List<Sector> SECTORS = List.of(
            new Sector("XOM", "엑슨모빌", "에너지/석유", words("oil", "energy", "opec", "석유", "원유")),
            new Sector("LMT", "록히드마틴", "방산/항공", words("defense", "military", "nato", "방산", "무기")),
            new Sector("JPM", "JP모건", "금융/은행", words("bank", "fed", "rate", "금융", "금리")),
            new Sector("X", "US스틸", "철강/소재", words("steel", "tariff", "철강", "관세")),
            new Sector("NVDA", "엔비디아", "반도체", words("chip", "nvidia", "semiconductor", "반도체", "엔비디아")),
            new Sector("AAPL", "애플", "빅테크", words("apple", "big tech", "애플", "빅테크")),
            new Sector("BABA", "알리바바", "중국/신흥", words("china", "beijing", "중국", "베이징")),
            new Sector("MSTR", "마이크로스트래티지", "암호화자산", words("bitcoin", "crypto", "비트코인", "암호화")),
            new Sector("DJT", "트럼프미디어", "트럼프미디어", words("truth social", "media", "트루스", "미디어")),
            new Sector("JNJ", "존슨앤존슨", "의료", words("drug", "medicare", "제약", "의료"))
    );

    private static final String[] NEGATIVE = {"sanction", "ban", "war", "crash", "selloff", "제재", "금지", "전쟁"};

    private TrumpSectorBook() {
    }

    public static List<Pick> score(List<String> texts, int limit) {
        List<Pick> picks = new ArrayList<>();
        for (Sector sector : SECTORS) {
            Pick pick = scoreSector(sector, texts);
            if (pick != null) {
                picks.add(pick);
            }
        }
        picks.sort((left, right) -> Integer.compare(right.confidence(), left.confidence()));
        if (picks.size() <= limit) {
            return picks;
        }
        return List.copyOf(picks.subList(0, limit));
    }

    public static String mood(List<Pick> picks) {
        if (picks == null || picks.isEmpty()) {
            return "불확실";
        }
        long sells = picks.stream().filter(pick -> "SELL".equals(pick.action())).count();
        long buys = picks.stream().filter(pick -> "BUY".equals(pick.action())).count();
        if (sells > buys) {
            return "강경";
        }
        if (buys > sells) {
            return "완화";
        }
        return "중립";
    }

    private static Pick scoreSector(Sector sector, List<String> texts) {
        int hits = 0;
        int negative = 0;
        if (texts == null) {
            return null;
        }
        for (String text : texts) {
            String folded = fold(text);
            if (!contains(folded, sector.words())) {
                continue;
            }
            hits++;
            if (contains(folded, NEGATIVE)) {
                negative++;
            }
        }
        if (hits == 0) {
            return null;
        }
        String action = negative > hits / 2 ? "SELL" : hits >= 2 ? "BUY" : "WATCH";
        int confidence = Math.min(90, 40 + hits * 15);
        String reason = sector.sector() + " 언급 " + hits + "건";
        return new Pick(sector.ticker(), sector.name(), sector.sector(), action, confidence, reason);
    }

    private static boolean contains(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static String fold(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private static String[] words(String... values) {
        return values;
    }
}
