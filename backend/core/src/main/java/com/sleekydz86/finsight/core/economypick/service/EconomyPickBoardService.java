package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.domain.EconomyPickBoard;
import com.sleekydz86.finsight.core.economypick.domain.PickRow;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import com.sleekydz86.finsight.core.economypick.domain.QuantSnapshot;
import com.sleekydz86.finsight.core.economypick.domain.SpaceXSnapshot;
import com.sleekydz86.finsight.core.economypick.domain.TrumpSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EconomyPickBoardService {

    private static final Logger log = LoggerFactory.getLogger(EconomyPickBoardService.class);

    private final KosdaqSignalService kosdaqSignalService;
    private final QuantBoardService quantBoardService;
    private final SpaceXQuantService spaceXQuantService;
    private final TrumpPickService trumpPickService;
    private final QuoteBook quoteBook;

    public EconomyPickBoardService(KosdaqSignalService kosdaqSignalService,
                                   QuantBoardService quantBoardService,
                                   SpaceXQuantService spaceXQuantService,
                                   TrumpPickService trumpPickService,
                                   QuoteBook quoteBook) {
        this.kosdaqSignalService = kosdaqSignalService;
        this.quantBoardService = quantBoardService;
        this.spaceXQuantService = spaceXQuantService;
        this.trumpPickService = trumpPickService;
        this.quoteBook = quoteBook;
    }

    public EconomyPickBoard load() {
        TrumpSnapshot trump = trumpPickService.snapshot();
        Map<String, List<PriceBar>> book = quoteBook.load(symbols(trump));
        QuantSnapshot quant = quantBoardService.snapshot(book);
        SpaceXSnapshot spacex = spaceXQuantService.snapshot(book);
        List<PickRow> rows = new ArrayList<>();
        rows.addAll(kosdaqSignalService.rows());
        rows.addAll(quantBoardService.rows(quant, book));
        rows.addAll(spaceXQuantService.rows(spacex));
        rows.addAll(trumpPickService.rows(trump, book));
        log.info("경제PICK 보드 구성 dashboard={} quant={} spacex={} trump={}",
                count(rows, "DASHBOARD"), count(rows, "QUANT"), count(rows, "SPACEX"), count(rows, "TRUMP"));
        return new EconomyPickBoard(rows, quantBoardService.tapes(book), kosdaqSignalService.dashboard(), quant, spacex, trump);
    }

    private Set<String> symbols(TrumpSnapshot trump) {
        Set<String> symbols = new LinkedHashSet<>();
        symbols.addAll(quantBoardService.symbols());
        symbols.addAll(spaceXQuantService.symbols());
        for (var pick : trump.picks()) {
            symbols.add(pick.ticker());
        }
        return symbols;
    }

    private int count(List<PickRow> rows, String lane) {
        return (int) rows.stream().filter(row -> lane.equals(row.lane())).count();
    }
}
