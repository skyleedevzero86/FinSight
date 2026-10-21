package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.economypick.domain.EconomyPickAssets;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickBoard;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickBrief;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickChart;
import com.sleekydz86.finsight.core.economypick.domain.EconomyPickSales;
import com.sleekydz86.finsight.core.economypick.domain.KosdaqDashboard;
import com.sleekydz86.finsight.core.economypick.domain.QuantSnapshot;
import com.sleekydz86.finsight.core.economypick.domain.SpaceXSnapshot;
import com.sleekydz86.finsight.core.economypick.domain.TrumpSnapshot;
import com.sleekydz86.finsight.core.economypick.service.EconomyPickBoardService;
import com.sleekydz86.finsight.core.economypick.service.EconomyPickBriefService;
import com.sleekydz86.finsight.core.economypick.service.EconomyPickChartService;
import com.sleekydz86.finsight.core.economypick.service.EconomyPickPanelService;
import com.sleekydz86.finsight.core.economypick.service.KosdaqSignalService;
import com.sleekydz86.finsight.core.economypick.service.QuantBoardService;
import com.sleekydz86.finsight.core.economypick.service.SpaceXQuantService;
import com.sleekydz86.finsight.core.economypick.service.TrumpPickService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.EconomyPickNewsView;
import com.sleekydz86.finsight.core.news.service.EconomyPickNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "경제PICK", description = "코스닥 시그널, 퀀트, SpaceX, Trump Pick, 종목뉴스")
@RestController
@RequestMapping("/api/v1/economy-pick")
public class EconomyPickController {

    private final EconomyPickNewsService economyPickNewsService;
    private final EconomyPickBoardService economyPickBoardService;
    private final KosdaqSignalService kosdaqSignalService;
    private final QuantBoardService quantBoardService;
    private final SpaceXQuantService spaceXQuantService;
    private final TrumpPickService trumpPickService;
    private final EconomyPickPanelService economyPickPanelService;
    private final EconomyPickChartService economyPickChartService;
    private final EconomyPickBriefService economyPickBriefService;

    public EconomyPickController(EconomyPickNewsService economyPickNewsService,
                                 EconomyPickBoardService economyPickBoardService,
                                 KosdaqSignalService kosdaqSignalService,
                                 QuantBoardService quantBoardService,
                                 SpaceXQuantService spaceXQuantService,
                                 TrumpPickService trumpPickService,
                                 EconomyPickPanelService economyPickPanelService,
                                 EconomyPickChartService economyPickChartService,
                                 EconomyPickBriefService economyPickBriefService) {
        this.economyPickNewsService = economyPickNewsService;
        this.economyPickBoardService = economyPickBoardService;
        this.kosdaqSignalService = kosdaqSignalService;
        this.quantBoardService = quantBoardService;
        this.spaceXQuantService = spaceXQuantService;
        this.trumpPickService = trumpPickService;
        this.economyPickPanelService = economyPickPanelService;
        this.economyPickChartService = economyPickChartService;
        this.economyPickBriefService = economyPickBriefService;
    }

    @Operation(summary = "경제PICK 보드", description = "코스닥 시그널, 퀀트, SpaceX, Trump Pick을 한 목록으로 반환합니다.")
    @GetMapping("/board")
    public ResponseEntity<ApiResponse<EconomyPickBoard>> board() {
        return ResponseEntity.ok(ApiResponse.success(economyPickBoardService.load(), "경제PICK 보드를 조회했습니다"));
    }

    @Operation(summary = "코스닥 시그널", description = "DART 12개월 공시 룰로 고위험 시그널을 계산합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<KosdaqDashboard>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(kosdaqSignalService.dashboard(), "코스닥 시그널을 조회했습니다"));
    }

    @Operation(summary = "퀀트 대시보드", description = "ARDS-X, AMQS, 섹터 심리, MU-하이닉스 예측을 계산합니다.")
    @GetMapping("/quant")
    public ResponseEntity<ApiResponse<QuantSnapshot>> quant() {
        return ResponseEntity.ok(ApiResponse.success(quantBoardService.current(), "퀀트 대시보드를 조회했습니다"));
    }

    @Operation(summary = "SpaceX 퀀트", description = "우주·방산 종목의 Williams %R, RSI, 모멘텀 점수를 계산합니다.")
    @GetMapping("/spacex")
    public ResponseEntity<ApiResponse<SpaceXSnapshot>> spacex() {
        return ResponseEntity.ok(ApiResponse.success(spaceXQuantService.current(), "SpaceX 퀀트를 조회했습니다"));
    }

    @Operation(summary = "Trump Pick", description = "트럼프 관련 뉴스 피드와 섹터 관심종목 점수를 반환합니다.")
    @GetMapping("/trump")
    public ResponseEntity<ApiResponse<TrumpSnapshot>> trump() {
        return ResponseEntity.ok(ApiResponse.success(trumpPickService.snapshot(), "Trump Pick을 조회했습니다"));
    }

    @Operation(summary = "자산비율", description = "로그인한 사용자의 포트폴리오 자산·부채 비율입니다. 없으면 예시 비율입니다.")
    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<EconomyPickAssets>> assets(
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        Long userId = currentUser == null ? null : currentUser.getId();
        return ResponseEntity.ok(ApiResponse.success(economyPickPanelService.assets(userId), "자산비율을 조회했습니다"));
    }

    @Operation(summary = "매출 구성", description = "선택한 종목 기준으로 추정한 매출 구성을 반환합니다.")
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<EconomyPickSales>> sales(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String symbol) {
        return ResponseEntity.ok(ApiResponse.success(economyPickPanelService.sales(name, symbol), "매출 구성을 조회했습니다"));
    }

    @Operation(summary = "차트", description = "야후 시세 봉을 기간에 맞춰 반환합니다. 3분 버튼은 야후 2분봉입니다.")
    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<EconomyPickChart>> chart(
            @RequestParam(defaultValue = "") String symbol,
            @RequestParam(defaultValue = "") String market,
            @RequestParam(defaultValue = "일") String period) {
        return ResponseEntity.ok(ApiResponse.success(
                economyPickChartService.load(symbol, market, period), "차트를 조회했습니다"));
    }

    @Operation(summary = "AI 추천", description = "Ollama가 켜져 있으면 Llama 문장, 아니면 섹터 규칙 추천을 반환합니다.")
    @GetMapping("/brief")
    public ResponseEntity<ApiResponse<EconomyPickBrief>> brief(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String symbol) {
        return ResponseEntity.ok(ApiResponse.success(
                economyPickBriefService.brief(name, symbol), "추천을 조회했습니다"));
    }

    @Operation(summary = "종목뉴스 추천", description = "관심 카테고리가 있으면 그 뉴스를, 없거나 비회원이면 벡터 추천을 반환합니다.")
    @GetMapping("/news")
    public ResponseEntity<ApiResponse<EconomyPickNewsView>> news(
            @RequestParam(defaultValue = "6") int limit,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        String email = currentUser == null ? null : currentUser.getEmail();
        EconomyPickNewsView view = economyPickNewsService.recommend(email, limit);
        return ResponseEntity.ok(ApiResponse.success(view, "종목뉴스를 조회했습니다"));
    }
}
