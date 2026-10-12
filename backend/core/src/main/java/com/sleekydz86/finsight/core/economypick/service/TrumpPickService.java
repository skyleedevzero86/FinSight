package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.adapter.TrumpFeedClient;
import com.sleekydz86.finsight.core.economypick.adapter.TrumpFeedClient.Headline;
import com.sleekydz86.finsight.core.economypick.domain.Bars;
import com.sleekydz86.finsight.core.economypick.domain.PickRow;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import com.sleekydz86.finsight.core.economypick.domain.QuoteText;
import com.sleekydz86.finsight.core.economypick.domain.TrumpSectorBook;
import com.sleekydz86.finsight.core.economypick.domain.TrumpSectorBook.Pick;
import com.sleekydz86.finsight.core.economypick.domain.TrumpSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrumpPickService {

    private static final Logger log = LoggerFactory.getLogger(TrumpPickService.class);
    private static final Duration TTL = Duration.ofMinutes(15);

    private final TrumpFeedClient trumpFeedClient;
    private final QuoteBook quoteBook;
    private volatile TrumpSnapshot cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    public TrumpPickService(TrumpFeedClient trumpFeedClient, QuoteBook quoteBook) {
        this.trumpFeedClient = trumpFeedClient;
        this.quoteBook = quoteBook;
    }

    public TrumpSnapshot snapshot() {
        if (cached != null && cachedAt.plus(TTL).isAfter(Instant.now())) {
            return cached;
        }
        TrumpSnapshot loaded = load();
        cached = loaded;
        cachedAt = Instant.now();
        log.info("트럼프 픽 갱신 mood={} picks={}", loaded.mood(), loaded.picks().size());
        return loaded;
    }

    public List<PickRow> rows(TrumpSnapshot snapshot, Map<String, List<PriceBar>> book) {
        List<PickRow> rows = new ArrayList<>();
        List<Pick> picks = snapshot.picks().isEmpty() ? fallbackPicks() : snapshot.picks();
        for (Pick pick : picks) {
            if (rows.size() == 6) {
                break;
            }
            rows.add(row(pick, snapshot, quoteBook.bars(book, pick.ticker())));
        }
        return rows;
    }

    private TrumpSnapshot load() {
        List<Headline> headlines = trumpFeedClient.headlines();
        List<String> texts = headlines.stream().map(Headline::title).toList();
        List<Pick> picks = TrumpSectorBook.score(texts, 6);
        if (picks.isEmpty()) {
            picks = fallbackPicks();
        }
        String mood = TrumpSectorBook.mood(picks);
        return new TrumpSnapshot(
                headlines.isEmpty() ? "관심 섹터" : "Google News",
                summary(headlines, mood, picks),
                mood,
                impact(picks),
                topics(headlines),
                risks(picks),
                picks,
                items(headlines));
    }

    private PickRow row(Pick pick, TrumpSnapshot snapshot, List<PriceBar> bars) {
        List<Double> closes = Bars.closes(bars);
        String price = closes.isEmpty() ? "—" : QuoteText.usd(closes.get(closes.size() - 1));
        double change = QuoteText.dayChange(closes);
        String changeText = closes.size() < 2 ? pick.action() : QuoteText.percent(change);
        return new PickRow(
                "TRUMP",
                pick.name(),
                pick.ticker(),
                price,
                changeText,
                closes.size() < 2 ? "zero" : QuoteText.direction(change),
                QuoteText.logo(pick.ticker()),
                "Trump Pick",
                pick.sector(),
                snapshot.summary(),
                List.of(price, pick.action(), pick.confidence() + "%", pick.sector(), "Trump", pick.reason(), "—", "—"));
    }

    private List<Pick> fallbackPicks() {
        List<Pick> picks = new ArrayList<>();
        for (TrumpSectorBook.Sector sector : TrumpSectorBook.SECTORS) {
            if (picks.size() == 4) {
                break;
            }
            picks.add(new Pick(sector.ticker(), sector.name(), sector.sector(), "WATCH", 30, "피드 없음, 관심 섹터"));
        }
        return picks;
    }

    private String summary(List<Headline> headlines, String mood, List<Pick> picks) {
        if (headlines.isEmpty()) {
            return "트럼프 피드를 가져오지 못했습니다. 관심 섹터를 표시합니다.";
        }
        return mood + " 기조. 픽 " + picks.size() + "개. " + headlines.get(0).title();
    }

    private String impact(List<Pick> picks) {
        if (picks.isEmpty()) {
            return "시장 영향 판단 자료가 없습니다.";
        }
        return picks.get(0).sector() + " " + picks.get(0).action() + ". " + picks.get(0).reason();
    }

    private List<String> topics(List<Headline> headlines) {
        List<String> topics = new ArrayList<>();
        for (Headline headline : headlines) {
            if (topics.size() == 3) {
                break;
            }
            topics.add(headline.title().length() > 40 ? headline.title().substring(0, 40) : headline.title());
        }
        return topics;
    }

    private List<String> risks(List<Pick> picks) {
        List<String> risks = new ArrayList<>();
        for (Pick pick : picks) {
            if ("SELL".equals(pick.action())) {
                risks.add(pick.name() + " " + pick.reason());
            }
        }
        if (risks.isEmpty()) {
            risks.add("정책 발언 변동성");
        }
        return risks;
    }

    private List<TrumpSnapshot.Item> items(List<Headline> headlines) {
        List<TrumpSnapshot.Item> items = new ArrayList<>();
        for (Headline headline : headlines) {
            items.add(new TrumpSnapshot.Item(headline.title(), headline.source(), headline.link(), "news"));
        }
        return items;
    }
}
