"use client"

import { COMPANY_INFO_ROWS, INVESTOR_ROWS, MAIN_TABS, SUB_TABS_STOCK, THEME_PILLS } from "@/data/finsightEconomyPickData"
import type { EconomyPickDesk as Desk, SpaceStock } from "@/lib/economyPickBoard"
import type { EconomyPickRow } from "@/lib/economyPickBoard"
import { briefSourceLabel, type EconomyPickBrief } from "@/lib/economyPickBrief"
import { rsi14, sma, type PriceBar } from "@/lib/economyPickChart"
import type { EconomyPickNewsItem } from "@/lib/economyPickNews"
import {
  donutBackground,
  percentText,
  type EconomyPickAssets,
  type EconomyPickSales,
} from "@/lib/economyPickPanel"

type Stock = Extract<EconomyPickRow, { kind: "stock" }>

const INFO_TITLES = ["시가총액", "기업순위", "주식수", "외국인비중", "산업군", "세부산업군", "52주 최저", "52주 최고"]
const SALES_COLORS = ["#e57373", "#ffb74d", "#4db6ac", "#546e7a", "#7e57c2"]
const PILL = "inline-flex items-center rounded-full border border-gray-200 bg-white px-2.5 py-0.5 text-xs font-medium text-gray-700"

type Props = {
  mainTab: string
  subTab: string
  infoTab: string
  onMain: (tab: string) => void
  onSub: (tab: string) => void
  onInfo: (tab: string) => void
  selected: Stock
  desk: Desk | null
  assets: EconomyPickAssets
  sales: EconomyPickSales
  brief: EconomyPickBrief | null
  bars: PriceBar[]
  peers: Stock[]
  news: EconomyPickNewsItem[]
  newsNote: string
}

export function EconomyPickDesk(props: Props) {
  return (
    <section id="smart-tab" className="smart-tab ep-as-smart-tab">
      <nav className="main-tab-navigation flex flex-wrap gap-1 border-b border-gray-200 bg-gray-50 px-3 py-2">
        {MAIN_TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            className={tabClass(props.mainTab === tab.label)}
            onClick={() => {
              props.onMain(tab.label)
              document.getElementById("smart-tab")?.scrollIntoView({ block: "nearest" })
            }}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      {props.mainTab === "종목분석" ? <StockPane {...props} /> : null}
      {props.mainTab === "시장정보" ? <MarketPane desk={props.desk} /> : null}
      {props.mainTab === "종목발굴" ? <DiscoverPane desk={props.desk} /> : null}
      {props.mainTab === "커뮤니티" ? <NewsPane news={props.news} note={props.newsNote} trump={props.desk?.trump?.summary ?? ""} /> : null}
      {props.mainTab === "트레이딩" ? <TradePane desk={props.desk} /> : null}
    </section>
  )
}

function stockTabs(subTab: string): { label: string }[] {
  if (subTab === "도구" || subTab === "비교") return [...SUB_TABS_STOCK, { label: subTab, active: true }]
  return SUB_TABS_STOCK
}

function tabClass(active: boolean): string {
  return active
    ? "rounded-lg bg-white px-3 py-2 text-sm font-semibold text-finsight-primary shadow-sm ring-1 ring-gray-200 transition"
    : "rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-white hover:text-finsight-secondary"
}

function StockPane(props: Props) {
  return (
    <>
      <nav className="sub-tab-navigation flex flex-wrap gap-2 border-b border-gray-200 bg-white px-3 py-2">
        {stockTabs(props.subTab).map((tab) => (
          <button
            key={tab.label}
            type="button"
            className={props.subTab === tab.label
              ? "inline-block border-b-2 border-finsight-secondary px-1 pb-1 text-sm font-semibold text-finsight-primary"
              : "inline-block px-1 pb-1 text-sm font-medium text-gray-500 hover:text-gray-800"}
            onClick={() => props.onSub(tab.label)}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      <section className="app-content ep-as-app-content">
        {props.subTab === "종목정보" ? <InfoPane {...props} /> : null}
        {props.subTab === "지표분석" ? <IndicatorPane selected={props.selected} desk={props.desk} bars={props.bars} /> : null}
        {props.subTab === "AI예측" ? <BriefPane brief={props.brief} sales={props.sales} /> : null}
        {props.subTab === "도구" ? <ToolPane selected={props.selected} bars={props.bars} /> : null}
        {props.subTab === "비교" ? <ComparePane selected={props.selected} peers={props.peers} /> : null}
      </section>
    </>
  )
}

function InfoPane(props: Props) {
  const pills = props.selected.metrics.length > 0
    ? [props.selected.market, props.selected.metrics[1], props.selected.metrics[5]].filter(Boolean)
    : THEME_PILLS
  return (
    <div id="stock-information" className="stock-information app-view">
      <div className="header-container ep-as-si-header">
        <div className="stock-information-header ep-as-si-title">
          <div>
            <h3 className="stock-name">{props.selected.name}</h3>
            <div className="sub">
              <span>{props.selected.market || "코스피"}</span> <span>{props.selected.symbol}</span>
            </div>
          </div>
        </div>
        <div className="stock-themes ep-as-themes">
          {pills.map((pill, index) => <span key={`${pill}-${index}`} className={PILL}>{pill}</span>)}
        </div>
      </div>
      <div className="tabs-connected-underline flex gap-6 border-b border-gray-200 pb-2">
        {["요약", "재무", "이슈", "토론"].map((label) => (
          <button
            key={label}
            type="button"
            className={props.infoTab === label
              ? "border-b-2 border-finsight-secondary pb-2 text-sm font-semibold text-finsight-primary"
              : "border-b-2 border-transparent pb-2 text-sm font-semibold text-gray-500 hover:text-gray-800"}
            onClick={() => props.onInfo(label)}
          >
            {label}
          </button>
        ))}
      </div>
      {props.infoTab === "요약" ? <Summary selected={props.selected} /> : null}
      {props.infoTab === "재무" ? <Finance assets={props.assets} sales={props.sales} bars={props.bars} /> : null}
      {props.infoTab === "이슈" ? <NewsPane news={props.news} note={props.newsNote} trump="" /> : null}
      {props.infoTab === "토론" ? <BriefPane brief={props.brief} sales={props.sales} /> : null}
    </div>
  )
}

function Summary({ selected }: { selected: Stock }) {
  const cells = selected.metrics.length === 8
    ? INFO_TITLES.map((title, index) => [title, selected.metrics[index]] as [string, string])
    : COMPANY_INFO_ROWS.flat()
  return (
    <>
      <div className="company-information ep-as-company-grid">
        {cells.map(([title, contents]) => (
          <div key={`${selected.symbol}-${title}`} className="ep-as-company-cell">
            <p className="t">{title}</p>
            <p className="v">{contents}</p>
          </div>
        ))}
      </div>
      <section id="company-description" className="ep-as-company-desc">
        <p>{selected.detail || `${selected.name} 시세와 섹터 시그널을 이 화면에서 봅니다.`}</p>
      </section>
      {selected.metrics.length === 0 && selected.symbol === "005930" ? <DemoFlow /> : <MetricList title="시그널" rows={selected.metrics} />}
    </>
  )
}

function Finance(props: { assets: EconomyPickAssets; sales: EconomyPickSales; bars: PriceBar[] }) {
  return (
    <>
      <h2 className="ep-as-section-title">실적현황</h2>
      <Performance bars={props.bars} />
      <div className="ep-as-pair-grid">
        <AssetDonut assets={props.assets} />
        <SalesDonut sales={props.sales} />
      </div>
    </>
  )
}

function Performance({ bars }: { bars: PriceBar[] }) {
  if (bars.length < 2) {
    return <p className="ep-as-news-meta">야후 시세를 불러오면 기간 고가·저가·수익률이 표시됩니다.</p>
  }
  const first = bars[0]
  const last = bars[bars.length - 1]
  const ret = first.close === 0 ? 0 : ((last.close - first.close) / first.close) * 100
  const high = Math.max(...bars.map((bar) => bar.high))
  const low = Math.min(...bars.map((bar) => bar.low))
  return (
    <div className="sales-information__header ep-as-sales-summary">
      <Stat title="기간 수익률" value={`${ret >= 0 ? "+" : ""}${ret.toFixed(2)}%`} />
      <Stat title="기간 고가" value={high.toLocaleString()} />
      <Stat title="기간 저가" value={low.toLocaleString()} />
    </div>
  )
}

function Stat({ title, value }: { title: string; value: string }) {
  return (
    <div className="ep-as-sales-box">
      <p className="t">{title}</p>
      <p className="v">{value}</p>
    </div>
  )
}

function AssetDonut({ assets }: { assets: EconomyPickAssets }) {
  return (
    <div>
      <h2 className="ep-as-section-title">자산비율</h2>
      {assets.note ? <p className="ep-as-news-meta">{assets.note}</p> : null}
      <div
        className="ep-as-donut"
        style={{
          background: donutBackground([
            { color: "var(--ep-up)", percent: assets.equityPercent },
            { color: "var(--ep-down)", percent: assets.debtPercent },
          ]),
        }}
      />
      <div className="ep-as-company-cell"><p className="t">자본비중</p><p className="v">{percentText(assets.equityPercent)}</p></div>
      <div className="ep-as-company-cell"><p className="t">부채비중</p><p className="v">{percentText(assets.debtPercent)}</p></div>
    </div>
  )
}

function SalesDonut({ sales }: { sales: EconomyPickSales }) {
  return (
    <div>
      <h2 className="ep-as-section-title">매출 구성</h2>
      {sales.note ? <p className="ep-as-news-meta">{sales.note}</p> : null}
      <div
        className="ep-as-donut sales"
        style={{
          background: donutBackground(sales.slices.map((slice, index) => ({
            color: SALES_COLORS[index % SALES_COLORS.length],
            percent: Math.abs(slice.percent),
          }))),
        }}
      />
      <ul className="ep-as-list-ul">
        {sales.slices.map((slice) => (
          <li key={slice.label}>
            <span className="key">{slice.label}</span>
            <span className={slice.sign === "minus" ? "minus" : "plus"}>{percentText(slice.percent)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function IndicatorPane({ selected, desk, bars }: { selected: Stock; desk: Desk | null; bars: PriceBar[] }) {
  const space = desk?.spacex.find((row) => row.symbol === selected.symbol || row.name === selected.name)
  const rsi = rsi14(bars)
  const ma20 = sma(bars, 20)
  const ma50 = sma(bars, 50)
  return (
    <div>
      <h2 className="ep-as-section-title">지표분석</h2>
      <div className="company-information ep-as-company-grid">
        <Cell title="RSI 14" value={rsi == null ? "봉이 부족합니다" : rsi.toFixed(1)} />
        <Cell title="20일 이평" value={ma20 == null ? "—" : ma20.toLocaleString(undefined, { maximumFractionDigits: 2 })} />
        <Cell title="50일 이평" value={ma50 == null ? "—" : ma50.toLocaleString(undefined, { maximumFractionDigits: 2 })} />
      </div>
      <MetricList title={selected.name} rows={selected.metrics} />
      {space ? <SpaceLine stock={space} /> : <p className="ep-as-news-meta">야후 봉으로 RSI와 이평을 계산합니다.</p>}
    </div>
  )
}

function ToolPane({ selected, bars }: { selected: Stock; bars: PriceBar[] }) {
  const last = bars[bars.length - 1]
  const high = bars.length === 0 ? null : Math.max(...bars.map((bar) => bar.high))
  const low = bars.length === 0 ? null : Math.min(...bars.map((bar) => bar.low))
  return (
    <div>
      <h2 className="ep-as-section-title">도구</h2>
      <p className="ep-as-news-meta">{selected.name} 봉에 마우스를 올리면 위 범례의 시가·고가·저가·종가가 그 봉으로 바뀝니다.</p>
      <div className="company-information ep-as-company-grid">
        <Cell title="최근 시가" value={last ? last.open.toLocaleString() : "—"} />
        <Cell title="최근 종가" value={last ? last.close.toLocaleString() : selected.price} />
        <Cell title="기간 고가" value={high == null ? "—" : high.toLocaleString()} />
        <Cell title="기간 저가" value={low == null ? "—" : low.toLocaleString()} />
        <Cell title="최근 거래량" value={last && last.volume > 0 ? Math.round(last.volume).toLocaleString() : selected.volume || "—"} />
      </div>
    </div>
  )
}

function ComparePane({ selected, peers }: { selected: Stock; peers: Stock[] }) {
  return (
    <div>
      <h2 className="ep-as-section-title">비교</h2>
      <p className="ep-as-news-meta">{selected.name}과 같은 묶음의 종목 등락입니다.</p>
      <ul className="ep-as-list-ul">
        <li>
          <span className="key">{selected.name} {selected.symbol}</span>
          <span className={selected.direction === "down" ? "minus" : "plus"}>{selected.price} {selected.changePct}</span>
        </li>
        {peers.map((row) => (
          <li key={`${row.symbol}-${row.name}`}>
            <span className="key">{row.name} {row.symbol}</span>
            <span className={row.direction === "down" ? "minus" : "plus"}>{row.price} {row.changePct}</span>
          </li>
        ))}
      </ul>
      {peers.length === 0 ? <p className="ep-as-news-meta">같은 묶음에 비교할 종목이 없습니다.</p> : null}
    </div>
  )
}

function SpaceLine({ stock }: { stock: SpaceStock }) {
  return (
    <p className="ep-as-company-desc">
      {stock.name} {stock.signal} · 점수 {stock.score.toFixed(1)}
      {stock.rsi == null ? "" : ` · RSI ${Math.round(stock.rsi)}`}
      {stock.williamsR == null ? "" : ` · Williams %R ${stock.williamsR.toFixed(0)}`}
    </p>
  )
}

function BriefPane({ brief, sales }: { brief: EconomyPickBrief | null; sales: EconomyPickSales }) {
  const headline = brief?.headline || sales.note
  const source = brief ? briefSourceLabel(brief.source) : "화면 추정"
  return (
    <div>
      <h2 className="ep-as-section-title">AI예측</h2>
      <p className="ep-as-news-meta">{source}</p>
      <p className="ep-as-company-desc">{headline}</p>
      {brief?.body ? <p className="ep-as-company-desc">{brief.body}</p> : null}
      <ul className="ep-as-list-ul">
        {(brief?.points.length ? brief.points : sales.slices.map((slice) => `${slice.label} ${percentText(slice.percent)}`)).map((point, index) => (
          <li key={`${index}-${point}`}><span className="key">{point}</span></li>
        ))}
      </ul>
    </div>
  )
}

function MarketPane({ desk }: { desk: Desk | null }) {
  const quant = desk?.quant
  if (!quant) return <Empty text="퀀트 보드를 불러오지 못했습니다. 백엔드를 켜면 국면, VIX, 공포탐욕이 채워집니다." />
  return (
    <div className="ep-as-app-content">
      <h2 className="ep-as-section-title">시장정보</h2>
      <div className="company-information ep-as-company-grid">
        <Cell title="국면" value={`${quant.regimeLabel} · ${quant.regimeSignal}`} />
        <Cell title="VIX" value={quant.vix.toFixed(2)} />
        <Cell title="공포탐욕" value={`${quant.fearGreed.toFixed(1)} ${quant.fearGreedStatus}`} />
        <Cell title="헤지" value={`롱 ${quant.longWeight}% / 헤지 ${quant.hedge}% / 안전 ${quant.safe}%`} />
        <Cell title="하이닉스" value={`${quant.hynixSignal} ${quant.hynixPredictedChangePct.toFixed(2)}%`} />
      </div>
      <h2 className="ep-as-section-title">반도체 바스켓</h2>
      <ul className="ep-as-list-ul">
        {quant.amqs.map((row) => (
          <li key={row.ticker}>
            <span className="key">{row.name} RSI {row.rsi}</span>
            <span className={row.changePct.startsWith("-") ? "minus" : "plus"}>{row.signal} {row.score}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function DiscoverPane({ desk }: { desk: Desk | null }) {
  if (!desk?.dashboard && !desk?.trump) return <Empty text="종목발굴 시그널을 불러오지 못했습니다." />
  return (
    <div className="ep-as-app-content">
      <h2 className="ep-as-section-title">코스닥 위험 시그널</h2>
      <p className="ep-as-news-meta">
        기업 {desk?.dashboard?.companyCount ?? 0} · 고위험 {desk?.dashboard?.highRiskCount ?? 0}
      </p>
      <ul className="ep-as-list-ul">
        {(desk?.dashboard?.signals ?? []).slice(0, 8).map((row) => (
          <li key={row.symbol}>
            <span className="key">{row.name} {row.rules}</span>
            <span className={row.highRisk ? "minus" : "plus"}>위험 {row.risk}</span>
          </li>
        ))}
      </ul>
      <h2 className="ep-as-section-title">Trump Pick</h2>
      <p className="ep-as-news-meta">{desk?.trump?.mood} {desk?.trump?.marketImpact}</p>
      <ul className="ep-as-list-ul">
        {(desk?.trump?.picks ?? []).map((pick) => (
          <li key={pick.ticker}>
            <span className="key">{pick.name} {pick.reason}</span>
            <span className="plus">{pick.action} {pick.confidence}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function TradePane({ desk }: { desk: Desk | null }) {
  const rows = desk?.spacex ?? []
  if (rows.length === 0) return <Empty text="트레이딩 시그널을 불러오지 못했습니다." />
  return (
    <div className="ep-as-app-content">
      <h2 className="ep-as-section-title">SpaceX 시그널</h2>
      <p className="ep-as-news-meta">주문 전송은 하지 않습니다. BUY, SELL, HOLD, AVOID 판단만 보여 줍니다.</p>
      <ul className="ep-as-list-ul">
        {rows.map((row) => (
          <li key={row.symbol}>
            <span className="key">{row.name} {row.price} {row.changePct}</span>
            <span className={row.signal === "SELL" || row.signal === "AVOID" ? "minus" : "plus"}>{row.signal}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

function NewsPane({ news, note, trump }: { news: EconomyPickNewsItem[]; note: string; trump: string }) {
  return (
    <div className="ep-as-app-content">
      <h2 className="ep-as-section-title">종목뉴스</h2>
      {note ? <p className="ep-as-news-meta">{note}</p> : null}
      {trump ? <p className="ep-as-news-meta">{trump}</p> : null}
      <div className="stock-news__container">
        {news.map((item) => (
          <a
            key={item.id}
            href={item.href}
            target={item.external ? "_blank" : undefined}
            rel={item.external ? "noopener noreferrer" : undefined}
            className="stock-news__news-link ep-as-news-item"
          >
            <div className="ep-as-news-row">
              <div>
                <h3 className="ep-as-news-title">{item.title}</h3>
                <div className="ep-as-news-meta">{item.source} · {item.time}</div>
              </div>
            </div>
          </a>
        ))}
      </div>
    </div>
  )
}

function MetricList({ title, rows }: { title: string; rows: string[] }) {
  if (rows.length === 0) return null
  return (
    <>
      <h2 className="ep-as-section-title">{title}</h2>
      <ul className="ep-as-list-ul">
        {rows.map((row, index) => <li key={`${index}-${row}`}><span className="key">{row}</span></li>)}
      </ul>
    </>
  )
}

function DemoFlow() {
  return (
    <>
      <h2 className="ep-as-section-title">투자자별 매매동향</h2>
      <p className="ep-as-news-meta">실시간 수급 API가 없어 예시 행입니다.</p>
      <div className="ep-as-table-wrap">
        <table className="a-table ep-as-table">
          <thead><tr><th>날짜</th><th>개인</th><th>기관</th><th>외인</th></tr></thead>
          <tbody>
            {INVESTOR_ROWS.map(([dt, a, b, c]) => (
              <tr key={dt}>
                <td>{dt}</td>
                <td className={a.startsWith("-") ? "down" : "up"}>{a}</td>
                <td className={b.startsWith("-") ? "down" : "up"}>{b}</td>
                <td className={c.startsWith("-") ? "down" : "up"}>{c}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

function Cell({ title, value }: { title: string; value: string }) {
  return (
    <div className="ep-as-company-cell">
      <p className="t">{title}</p>
      <p className="v">{value}</p>
    </div>
  )
}

function Empty({ text }: { text: string }) {
  return <p className="ep-as-app-content ep-as-news-meta">{text}</p>
}
