"use client"

import { useMemo, useState } from "react"
import {
  COMPANY_INFO_ROWS,
  INVESTOR_ROWS,
  MAIN_TABS,
  NEWS_ITEMS,
  SALES_COMPOSITION,
  SUB_TABS_STOCK,
  THEME_PILLS,
  TICKER_ITEMS,
  type WatchlistRow,
  WATCHLIST_ROWS,
} from "@/data/finsightEconomyPickData"

function isStockRow(r: WatchlistRow): r is Extract<WatchlistRow, { kind: "stock" }> {
  return r.kind === "stock"
}

type WatchlistStockRow = Extract<WatchlistRow, { kind: "stock" }>

function defaultStock(): WatchlistStockRow {
  const samsung = WATCHLIST_ROWS.find(
    (r): r is WatchlistStockRow => isStockRow(r) && r.symbol === "005930",
  )
  if (samsung) return samsung
  const first = WATCHLIST_ROWS.find(isStockRow)
  if (!first) {
    throw new Error("finsightEconomyPickData: no stock rows")
  }
  return first
}

const btnOutline =
  "inline-flex h-8 items-center justify-center gap-1 rounded-lg border border-gray-300 bg-white px-3 text-sm font-medium text-gray-800 shadow-sm transition hover:border-finsight-secondary hover:bg-gray-50"
const btnSmOutline =
  "inline-flex h-7 items-center justify-center rounded-lg border border-gray-200 bg-white px-2.5 text-xs font-medium text-gray-700 transition hover:border-finsight-secondary hover:bg-finsight-light/50"
const pillTheme =
  "inline-flex items-center rounded-full border border-gray-200 bg-white px-2.5 py-0.5 text-xs font-medium text-gray-700"

export default function EconomyPickBody() {
  const [selected, setSelected] = useState(defaultStock)
  const tickerDup = useMemo(() => [...TICKER_ITEMS, ...TICKER_ITEMS], [])

  const dir = selected.direction
  const chartHeights = ["62%", "38%", "71%", "45%", "58%", "33%", "67%", "52%"]

  return (
    <div className="ep-as-root bg-finsight-light text-gray-900">
      <div id="finsight-economy-root" className="finsight-economy-clone">
        <div id="service" className="service ep-as-inner">
          <div className="ep-as-layout">
            <aside className="portfolio ep-as-portfolio" aria-label="관심목록">
              <div className="header">
                <h2 className="header-name">새 관심목록</h2>
              </div>
              <div className="watchlist-menu ep-as-watchlist-menu">
                <button type="button" className={btnOutline}>
                  + 종목추가
                </button>
                <button type="button" className={btnOutline}>
                  ⚙ 설정
                </button>
              </div>
              <div className="stock-list ep-as-stock-list" id="watchlist-sortable-list">
                {WATCHLIST_ROWS.map((row, i) => {
                  if (row.kind === "splitter") {
                    return (
                      <div key={`s-${i}`} className="splitter-wrapper ep-as-splitter">
                        <span>{row.title}</span>
                      </div>
                    )
                  }
                  const active =
                    row.symbol === selected.symbol && row.name === selected.name
                  return (
                    <button
                      key={`${row.symbol}-${i}`}
                      type="button"
                      className={`watchlist-item ep-as-watchlist-item${active ? " ep-as-watchlist-item--active" : ""}`}
                      onClick={() => setSelected(row)}
                    >
                      <div className="a-logo ep-as-logo">
                        <img src={row.logo} alt="" width={28} height={28} />
                      </div>
                      <div
                        className="stock-info price-color ep-as-stock-info"
                        data-dir={row.direction}
                      >
                        <div className="row1">
                          <p className="cname">{row.name}</p>
                          <span className="price">{row.price}</span>
                        </div>
                        <div className="row2">
                          <span className="sym">{row.symbol}</span>
                          <span className="ret">{row.changePct}</span>
                        </div>
                      </div>
                    </button>
                  )
                })}
              </div>
            </aside>

            <div className="ep-as-center">
              <div id="multi-chart" className="multi-chart ep-as-multi-chart">
                <div id="stock-nav" className="stock-nav ep-as-stock-nav" data-dir={dir}>
                  <div className="stock-nav-stock-info ep-as-stock-nav-main">
                    <div className="logo-sm">
                      <img src={selected.logo} alt="" width={32} height={32} />
                    </div>
                    <div>
                      <h2 className="name">{selected.name}</h2>
                      <span className="meta">
                        {selected.symbol === "005930" ? "코스피" : "데모"}
                        <span className="dot"> ㆍ </span>
                        {selected.symbol}
                      </span>
                    </div>
                    <h2 className="price-big">{selected.price}</h2>
                    <span className="chg">{selected.changePct}</span>
                    <span className="dot">ㆍ</span>
                    <span className="vol">
                      거래량{" "}
                      {selected.symbol === "005930" ? "33,395,939" : "—"}
                    </span>
                  </div>
                </div>

                <div className="chart-nav ep-as-chart-nav">
                  <div className="tools">
                    {["도구", "지표", "전략", "비교", "분석", "예측"].map((label) => (
                      <button key={label} type="button" className={btnSmOutline}>
                        {label}
                      </button>
                    ))}
                  </div>
                  <div className="ep-as-periods">
                    {["3분", "15분", "일", "주", "월"].map((p) => (
                      <button
                        key={p}
                        type="button"
                        className={
                          p === "일"
                            ? "rounded-lg border border-finsight-secondary bg-finsight-light px-2.5 py-1 text-xs font-semibold text-finsight-primary shadow-sm"
                            : "rounded-lg border border-transparent bg-white px-2.5 py-1 text-xs font-medium text-gray-600 transition hover:border-gray-200 hover:bg-gray-50"
                        }
                      >
                        {p}
                      </button>
                    ))}
                  </div>
                </div>

                <div id="multi-chart__chart" className="multi-chart__chart-area ep-as-chart-area">
                  <div className="primechart ep-as-primechart">
                    <div className="chart__legend chart__legend--date-and-candle ep-as-chart-legend">
                      <span>2026.04.10(금)</span>
                      <span>
                        시 <span className="hl">209,000 (+2.96%)</span>
                      </span>
                      <span>
                        종 <span className="hl">207,000 (+1.97%)</span>
                      </span>
                    </div>
                    <div className="primechart__chart ep-as-chart-canvas">
                      <div className="ep-as-candles" aria-hidden>
                        {chartHeights.map((h, idx) => (
                          <div
                            key={idx}
                            className="ep-as-candle"
                            style={{ height: h }}
                          />
                        ))}
                      </div>
                    </div>
                    <div className="ep-as-subchart" />
                    <div className="primechart__botbar ep-as-botbar" />
                  </div>
                </div>
              </div>

              <div className="ticker-tape ep-as-ticker" aria-label="시세 티커">
                <div className="ep-as-ticker-track">
                  {tickerDup.map((t, idx) => (
                    <div
                      key={`${t.name}-${idx}`}
                      className="ticker-tape-contents-item ep-as-ticker-item"
                      data-dir={t.direction}
                    >
                      <div className="ep-as-ticker-logo">
                        <img src={t.logo} alt="" width={20} height={20} />
                      </div>
                      <span className="nm">{t.name}</span>
                      <span className="pr">{t.price}</span>
                      <span className="ch">{t.chg}</span>
                    </div>
                  ))}
                </div>
              </div>

              <section id="smart-tab" className="smart-tab ep-as-smart-tab">
                <nav className="main-tab-navigation flex flex-wrap gap-1 border-b border-gray-200 bg-gray-50 px-3 py-2">
                  {MAIN_TABS.map((tab) => (
                    <a
                      key={tab.label}
                      href={tab.href}
                      className={
                        tab.active
                          ? "rounded-lg bg-white px-3 py-2 text-sm font-semibold text-finsight-primary shadow-sm ring-1 ring-gray-200 transition"
                          : "rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition hover:bg-white hover:text-finsight-secondary"
                      }
                      onClick={(e) => e.preventDefault()}
                    >
                      {tab.label}
                    </a>
                  ))}
                </nav>
                <nav className="sub-tab-navigation flex flex-wrap gap-2 border-b border-gray-200 bg-white px-3 py-2">
                  {SUB_TABS_STOCK.map((tab) => (
                    <a
                      key={tab.label}
                      href="#"
                      className={
                        tab.active
                          ? "inline-block border-b-2 border-finsight-secondary px-1 pb-1 text-sm font-semibold text-finsight-primary"
                          : "inline-block px-1 pb-1 text-sm font-medium text-gray-500 hover:text-gray-800"
                      }
                      onClick={(e) => e.preventDefault()}
                    >
                      {tab.label}
                    </a>
                  ))}
                </nav>

                <section className="app-content ep-as-app-content">
                  <div id="stock-information" className="stock-information app-view">
                    <div className="header-container ep-as-si-header">
                      <div className="stock-information-header ep-as-si-title">
                        <div className="stock-logo lg">
                          <img
                            src="https://file.alphasquare.co.kr/media/images/stock_logo/kr/005930.png"
                            alt=""
                            width={48}
                            height={48}
                          />
                        </div>
                        <div>
                          <h3 className="stock-name">삼성전자</h3>
                          <div className="sub">
                            <span>코스피</span> <span>005930</span>
                          </div>
                        </div>
                      </div>
                      <div className="stock-themes ep-as-themes">
                        {THEME_PILLS.map((pill) => (
                          <span key={pill} className={pillTheme}>
                            {pill}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div className="tabs-connected-underline flex gap-6 border-b border-gray-200 pb-2">
                      <button
                        type="button"
                        className="border-b-2 border-finsight-secondary pb-2 text-sm font-semibold text-finsight-primary"
                      >
                        요약
                      </button>
                      <button
                        type="button"
                        className="border-b-2 border-transparent pb-2 text-sm font-semibold text-gray-500 hover:text-gray-800"
                      >
                        재무
                      </button>
                      <button
                        type="button"
                        className="border-b-2 border-transparent pb-2 text-sm font-semibold text-gray-500 hover:text-gray-800"
                      >
                        이슈
                      </button>
                      <button
                        type="button"
                        className="border-b-2 border-transparent pb-2 text-sm font-semibold text-gray-500 hover:text-gray-800"
                      >
                        토론
                      </button>
                    </div>

                    <div className="company-information ep-as-company-grid">
                      {COMPANY_INFO_ROWS.flatMap((pair, ri) =>
                        pair.map(([title, contents], ci) => (
                          <div key={`${ri}-${ci}-${title}`} className="ep-as-company-cell">
                            <p className="t">{title}</p>
                            <p className="v">{contents}</p>
                          </div>
                        )),
                      )}
                    </div>

                    <section id="company-description" className="ep-as-company-desc">
                      <p>
                        삼성전자는 1969년 설립된 기업으로 반도체, 전자 제품 제조·판매업을 영위하고 있다.
                      </p>
                      <p>
                        주요 매출은 스마트폰, 네트워크시스템, 컴퓨터 등을 생산하는 IM부문에서 발생하고 있으며
                        반도체, CE 부문이 뒤를 잇고 있다.
                      </p>
                    </section>

                    <h2 className="ep-as-section-title">실적현황</h2>
                    <div className="sales-information__header ep-as-sales-summary">
                      <div className="ep-as-sales-box">
                        <p className="t">최근 매출액 (전년도대비)</p>
                        <p className="v">333조 6,059억</p>
                        <p className="r">(+10.88%)</p>
                      </div>
                      <div className="ep-as-sales-box">
                        <p className="t">최근 영업이익</p>
                        <p className="v">43조 6,010억</p>
                        <p className="r">(+33.23%)</p>
                      </div>
                      <div className="ep-as-sales-box">
                        <p className="t">최근 순이익</p>
                        <p className="v">45조 2,068억</p>
                        <p className="r">(+31.22%)</p>
                      </div>
                    </div>
                    <div className="ep-as-mini-chart" aria-hidden>
                      <svg viewBox="0 0 300 80" preserveAspectRatio="none">
                        <path
                          d="M0,60 L40,45 L80,55 L120,30 L160,40 L200,25 L240,35 L280,20 L300,28"
                          fill="none"
                          stroke="#44bead"
                          strokeWidth="2"
                          opacity={0.85}
                        />
                      </svg>
                    </div>

                    <h2 className="ep-as-section-title">투자자별 매매동향</h2>
                    <div className="ep-as-table-wrap">
                      <table className="a-table ep-as-table">
                        <thead>
                          <tr>
                            <th>날짜</th>
                            <th>개인</th>
                            <th>기관</th>
                            <th>외인</th>
                          </tr>
                        </thead>
                        <tbody>
                          {INVESTOR_ROWS.map(([dt, a, b, c]) => (
                            <tr key={dt}>
                              <td>{dt}</td>
                              <td
                                className={
                                  a.startsWith("-") ? "down" : "up"
                                }
                              >
                                {a}
                              </td>
                              <td
                                className={
                                  b.startsWith("-") ? "down" : "up"
                                }
                              >
                                {b}
                              </td>
                              <td
                                className={
                                  c.startsWith("-") ? "down" : "up"
                                }
                              >
                                {c}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    <div className="ep-as-pair-grid">
                      <div>
                        <h2 className="ep-as-section-title">자산비율</h2>
                        <div className="ep-as-donut" />
                        <div className="ep-as-company-cell">
                          <p className="t">자본비중</p>
                          <p className="v">76.96%</p>
                        </div>
                        <div className="ep-as-company-cell">
                          <p className="t">부채비중</p>
                          <p className="v">23.04%</p>
                        </div>
                      </div>
                      <div>
                        <h2 className="ep-as-section-title">매출 구성</h2>
                        <div className="ep-as-donut sales" />
                        <ul className="ep-as-list-ul">
                          {SALES_COMPOSITION.map((s) => (
                            <li key={s.key}>
                              <span className="key">{s.key}</span>
                              <span className={s.sign === "plus" ? "plus" : "minus"}>
                                {s.value}
                              </span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    <h2 className="ep-as-section-title">종목뉴스</h2>
                    <div className="stock-news__container">
                      {NEWS_ITEMS.map((n) => (
                        <a
                          key={n.href}
                          href={n.href}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="stock-news__news-link ep-as-news-item"
                        >
                          <div className="ep-as-news-row">
                            {n.thumb ? (
                              <div className="ep-as-news-thumb">
                                <img src={n.thumb} alt="" width={64} height={48} />
                              </div>
                            ) : null}
                            <div>
                              <h3 className="ep-as-news-title">{n.title}</h3>
                              <div className="ep-as-news-meta">
                                {n.source} · {n.time}
                              </div>
                            </div>
                          </div>
                        </a>
                      ))}
                    </div>

                  </div>
                </section>
              </section>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
