"use client"

import { useEffect, useMemo, useState } from "react"
import { WATCHLIST_ROWS } from "@/data/finsightEconomyPickData"
import {
  fetchEconomyPickBoard,
  type EconomyPickDesk as DeskData,
  type EconomyPickRow,
  type EconomyPickTape,
} from "@/lib/economyPickBoard"
import { fetchEconomyPickBrief, type EconomyPickBrief } from "@/lib/economyPickBrief"
import { candlesOf, fetchEconomyPickChart, legendOf, type PriceBar } from "@/lib/economyPickChart"
import { marketLogo } from "@/lib/economyPickBoard"
import { EconomyPickDesk } from "@/components/economy-pick/EconomyPickDesk"
import {
  fetchEconomyPickAssets,
  fetchEconomyPickSales,
  predictSales,
  sampleAssets,
  type EconomyPickAssets,
  type EconomyPickSales,
} from "@/lib/economyPickPanel"
import {
  economyPickNewsNote,
  fetchEconomyPickNews,
  type EconomyPickNewsItem,
} from "@/lib/economyPickNews"

type LiveStock = Extract<EconomyPickRow, { kind: "stock" }>

function isStockRow(row: EconomyPickRow): row is LiveStock {
  return row.kind === "stock"
}

function StockMark({ src, label, size }: { src: string; label: string; size: number }) {
  const [broken, setBroken] = useState(false)
  const letter = label.replace(/[^0-9A-Za-z가-힣]/g, "").slice(0, 1) || "·"
  if (!src || broken) {
    return (
      <span className="ep-as-logo-fallback" style={{ width: size, height: size }} aria-hidden>
        {letter}
      </span>
    )
  }
  return <img src={src} alt="" width={size} height={size} onError={() => setBroken(true)} />
}

function staticRows(): EconomyPickRow[] {
  return WATCHLIST_ROWS.map((row) => {
    if (row.kind === "splitter") return row
    return {
      kind: "stock",
      lane: "",
      name: row.name,
      symbol: row.symbol,
      price: row.price,
      changePct: row.changePct,
      direction: row.direction,
      logo: row.logo,
      market: row.symbol === "005930" ? "코스피" : "데모",
      volume: row.symbol === "005930" ? "33,395,939" : "—",
      detail: "",
      metrics: [],
    }
  })
}

function defaultStock(rows: EconomyPickRow[]): LiveStock {
  const samsung = rows.find((row): row is LiveStock => isStockRow(row) && row.symbol === "005930")
  if (samsung) return samsung
  const first = rows.find(isStockRow)
  if (!first) throw new Error("finsightEconomyPickData: no stock rows")
  return first
}

const btnOutline =
  "inline-flex h-8 items-center justify-center gap-1 rounded-lg border border-gray-300 bg-white px-3 text-sm font-medium text-gray-800 shadow-sm transition hover:border-finsight-secondary hover:bg-gray-50"
const btnSmOutline =
  "inline-flex h-7 items-center justify-center rounded-lg border border-gray-200 bg-white px-2.5 text-xs font-medium text-gray-700 transition hover:border-finsight-secondary hover:bg-finsight-light/50"
const PERIODS = ["3분", "15분", "일", "주", "월"]
const TOOLS = ["도구", "지표", "전략", "비교", "분석", "예측"]
const LANES = ["코스닥 시그널", "퀀트", "SpaceX", "Trump Pick", "내 종목"]
const CUSTOM_KEY = "finsight.economy-pick.custom"
const HIDDEN_KEY = "finsight.economy-pick.hidden"
const OPEN_KEY = "finsight.economy-pick.open"

function readCustom(): LiveStock[] {
  if (typeof window === "undefined") return []
  try {
    const raw = JSON.parse(sessionStorage.getItem(CUSTOM_KEY) || "[]") as unknown
    if (!Array.isArray(raw)) return []
    return raw.filter(isStoredStock)
  } catch {
    return []
  }
}

function isStoredStock(row: unknown): row is LiveStock {
  if (!row || typeof row !== "object") return false
  const stock = row as LiveStock
  return stock.kind === "stock" && Boolean(stock.symbol) && Boolean(stock.name)
}

function readNames(key: string): string[] {
  if (typeof window === "undefined") return []
  try {
    const raw = JSON.parse(sessionStorage.getItem(key) || "[]") as unknown
    if (!Array.isArray(raw)) return []
    return raw.filter((item): item is string => typeof item === "string")
  } catch {
    return []
  }
}

function laneGroups(rows: EconomyPickRow[]): { title: string; stocks: LiveStock[] }[] {
  const groups: { title: string; stocks: LiveStock[] }[] = []
  for (const row of rows) {
    if (row.kind === "splitter") {
      groups.push({ title: row.title, stocks: [] })
      continue
    }
    const last = groups[groups.length - 1]
    if (last) last.stocks.push(row)
  }
  return groups
}

function laneTitle(rows: EconomyPickRow[], stock: LiveStock): string {
  let lane = ""
  for (const row of rows) {
    if (row.kind === "splitter") lane = row.title
    else if (row.symbol === stock.symbol && row.name === stock.name) return lane
  }
  return lane
}

function withCustom(rows: EconomyPickRow[], custom: LiveStock[]): EconomyPickRow[] {
  if (custom.length === 0) return rows
  return [...rows, { kind: "splitter", title: "내 종목" }, ...custom]
}

function visibleRows(rows: EconomyPickRow[], hidden: string[]): EconomyPickRow[] {
  const off = new Set(hidden)
  const shown: EconomyPickRow[] = []
  let skip = false
  for (const row of rows) {
    if (row.kind === "splitter") {
      skip = off.has(row.title)
      if (!skip) shown.push(row)
      continue
    }
    if (!skip) shown.push(row)
  }
  return shown
}

function sameLane(rows: EconomyPickRow[], selected: LiveStock): LiveStock[] {
  let lane = ""
  let mine = ""
  const grouped = new Map<string, LiveStock[]>()
  for (const row of rows) {
    if (row.kind === "splitter") {
      lane = row.title
      continue
    }
    const list = grouped.get(lane) ?? []
    list.push(row)
    grouped.set(lane, list)
    if (row.symbol === selected.symbol && row.name === selected.name) mine = lane
  }
  return (grouped.get(mine) ?? []).filter((row) => row.symbol !== selected.symbol).slice(0, 6)
}

function quoteFromBars(stock: LiveStock, bars: PriceBar[]): LiveStock {
  const last = bars[bars.length - 1]
  const prev = bars[bars.length - 2]
  if (!last) return stock
  const change = prev && prev.close !== 0 ? ((last.close - prev.close) / prev.close) * 100 : 0
  const usd = /^[A-Za-z]/.test(stock.symbol)
  const digits = last.close.toLocaleString(undefined, { maximumFractionDigits: usd ? 2 : 0 })
  return {
    ...stock,
    price: `${usd ? "$" : ""}${digits}`,
    changePct: `${change >= 0 ? "+" : ""}${change.toFixed(2)}%`,
    direction: change > 0 ? "up" : change < 0 ? "down" : "zero",
    volume: last.volume > 0 ? Math.round(last.volume).toLocaleString() : stock.volume,
  }
}

export default function EconomyPickBody() {
  const initialRows = useMemo(() => staticRows(), [])
  const [rows, setRows] = useState(initialRows)
  const [selected, setSelected] = useState(() => defaultStock(initialRows))
  const [tickers, setTickers] = useState<EconomyPickTape[]>([])
  const [tapeLane, setTapeLane] = useState<string | null>(null)
  const [stockNews, setStockNews] = useState<EconomyPickNewsItem[]>([])
  const [newsNote, setNewsNote] = useState("종목뉴스를 불러오는 중입니다.")
  const [assets, setAssets] = useState<EconomyPickAssets>(sampleAssets)
  const [sales, setSales] = useState<EconomyPickSales>(() => predictSales(selected.name, selected.symbol))
  const [desk, setDesk] = useState<DeskData | null>(null)
  const [mainTab, setMainTab] = useState("종목분석")
  const [subTab, setSubTab] = useState("종목정보")
  const [infoTab, setInfoTab] = useState("요약")
  const [tool, setTool] = useState("분석")
  const [period, setPeriod] = useState("일")
  const [bars, setBars] = useState<PriceBar[]>([])
  const [chartLabel, setChartLabel] = useState("일")
  const [chartState, setChartState] = useState<"loading" | "ready" | "empty">("loading")
  const [focus, setFocus] = useState<number | null>(null)
  const [brief, setBrief] = useState<EconomyPickBrief | null>(null)
  const [custom, setCustom] = useState<LiveStock[]>([])
  const [hidden, setHidden] = useState<string[]>([])
  const [opened, setOpened] = useState<string[]>([])
  const [menu, setMenu] = useState<"add" | "settings" | null>(null)
  const [symbolInput, setSymbolInput] = useState("")
  const [nameInput, setNameInput] = useState("")
  const [menuNote, setMenuNote] = useState("")
  const [stored, setStored] = useState(false)
  const listed = useMemo(() => visibleRows(withCustom(rows, custom), hidden), [rows, custom, hidden])
  const groups = useMemo(() => laneGroups(listed), [listed])
  const tape = useMemo(() => {
    if (!tapeLane) return tickers
    const group = groups.find((item) => item.title === tapeLane)
    if (!group || group.stocks.length === 0) return tickers
    return group.stocks.map((stock) => ({
      symbol: stock.symbol,
      name: stock.name,
      price: stock.price,
      changePct: stock.changePct,
      direction: stock.direction,
      logo: stock.logo,
    }))
  }, [tapeLane, tickers, groups])
  const tickerDup = useMemo(() => [...tape, ...tape], [tape])

  useEffect(() => {
    let cancelled = false
    fetchEconomyPickBoard().then((board) => {
      if (cancelled || !board) return
      setRows(board.rows)
      if (board.tickers.length > 0) setTickers(board.tickers)
      setDesk(board.desk)
      setSelected((current) => {
        const stocks = board.rows.filter(isStockRow)
        const match = stocks.find((row) => row.symbol === current.symbol)
        if (match) return match
        if (current.lane === "MINE") return current
        return stocks[0] ?? current
      })
    })
    fetchEconomyPickNews(6).then((result) => {
      if (cancelled) return
      if (!result) {
        setStockNews([])
        setNewsNote("종목뉴스를 불러오지 못했습니다.")
        return
      }
      setStockNews(result.items)
      setNewsNote(economyPickNewsNote(result.mode, result.items.length))
    })
    fetchEconomyPickAssets().then((ratio) => {
      if (!cancelled && ratio) setAssets(ratio)
    })
    setCustom(readCustom())
    setHidden(readNames(HIDDEN_KEY))
    setOpened(readNames(OPEN_KEY))
    setStored(true)
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!stored) return
    sessionStorage.setItem(CUSTOM_KEY, JSON.stringify(custom))
  }, [custom, stored])

  useEffect(() => {
    if (!stored) return
    sessionStorage.setItem(HIDDEN_KEY, JSON.stringify(hidden))
  }, [hidden, stored])

  useEffect(() => {
    if (!stored) return
    sessionStorage.setItem(OPEN_KEY, JSON.stringify(opened))
  }, [opened, stored])

  useEffect(() => {
    let cancelled = false
    setSales(predictSales(selected.name, selected.symbol))
    fetchEconomyPickSales(selected.name, selected.symbol).then((mix) => {
      if (!cancelled && mix) setSales(mix)
    })
    fetchEconomyPickBrief(selected.name, selected.symbol).then((next) => {
      if (!cancelled && next) setBrief(next)
    })
    return () => {
      cancelled = true
    }
  }, [selected.name, selected.symbol])

  useEffect(() => {
    let cancelled = false
    setChartState("loading")
    setFocus(null)
    const symbol = selected.symbol
    fetchEconomyPickChart(symbol, selected.market, period).then((chart) => {
      if (cancelled) return
      if (!chart || chart.bars.length === 0) {
        setBars([])
        setChartState("empty")
        return
      }
      setBars(chart.bars)
      setChartLabel(chart.label)
      setChartState("ready")
      setSelected((current) => current.symbol === symbol ? quoteFromBars(current, chart.bars) : current)
      setCustom((list) => list.map((row) => row.symbol === symbol ? quoteFromBars(row, chart.bars) : row))
    })
    return () => {
      cancelled = true
    }
  }, [selected.symbol, selected.market, period])

  const dir = selected.direction
  const shownBars = bars.slice(-40)
  const candles = candlesOf(shownBars)
  const legend = legendOf(shownBars, chartLabel, focus)
  const chartHeights = candles.length > 0
    ? candles
    : ["62%", "38%", "71%", "45%", "58%", "33%", "67%", "52%"].map((height) => ({ height, up: true }))
  const legendDate = legend?.date ?? (chartState === "loading" ? "시세 불러오는 중" : "시세를 받지 못했습니다")

  function openDesk() {
    document.getElementById("smart-tab")?.scrollIntoView({ block: "nearest" })
  }

  function applyTool(label: string) {
    setTool(label)
    if (label === "예측") {
      setMainTab("종목분석")
      setSubTab("AI예측")
    } else if (label === "지표") {
      setMainTab("종목분석")
      setSubTab("지표분석")
    } else if (label === "분석") {
      setMainTab("종목분석")
      setSubTab("종목정보")
      setInfoTab("요약")
    } else if (label === "전략") {
      setMainTab("시장정보")
    } else if (label === "비교") {
      setMainTab("종목분석")
      setSubTab("비교")
    } else {
      setMainTab("종목분석")
      setSubTab("도구")
    }
    openDesk()
  }

  function addStock() {
    const symbol = symbolInput.trim().toUpperCase()
    const name = nameInput.trim() || symbol
    if (!symbol) {
      setMenuNote("종목코드를 입력해 주세요.")
      return
    }
    const book = withCustom(rows, custom)
    const existing = book.find((row): row is LiveStock => isStockRow(row) && row.symbol === symbol)
    if (existing) {
      setSelected(existing)
      reveal(laneTitle(book, existing))
      setMenuNote("이미 관심목록에 있어 그 종목을 골랐습니다.")
      return
    }
    const stock: LiveStock = {
      kind: "stock",
      lane: "MINE",
      name,
      symbol,
      price: "—",
      changePct: "—",
      direction: "zero",
      logo: marketLogo(symbol, ""),
      market: /^\d{6}$/.test(symbol) ? "코스피" : "직접추가",
      volume: "—",
      detail: "",
      metrics: [],
    }
    setCustom((list) => [...list, stock])
    setHidden((lanes) => lanes.filter((lane) => lane !== "내 종목"))
    reveal("내 종목")
    setSelected(stock)
    setSymbolInput("")
    setNameInput("")
    setMenuNote(`${name}을 내 종목에 넣었습니다.`)
  }

  function toggleLane(lane: string) {
    setHidden((lanes) => lanes.includes(lane) ? lanes.filter((item) => item !== lane) : [...lanes, lane])
  }

  function reveal(title: string) {
    if (!title) return
    setOpened((lanes) => lanes.includes(title) ? lanes : [...lanes, title])
  }

  function toggleOpen(title: string) {
    const next = !opened.includes(title)
    setOpened((lanes) => next ? [...lanes, title] : lanes.filter((item) => item !== title))
    setTapeLane(next ? title : null)
  }

  function pickTape(item: EconomyPickTape) {
    const book = withCustom(rows, custom)
    const found = book.find((row): row is LiveStock => isStockRow(row) && (row.symbol === item.symbol || row.name === item.name))
    if (found) {
      setSelected(found)
      reveal(laneTitle(book, found))
      return
    }
    if (!item.symbol) return
    setSelected({
      kind: "stock",
      lane: "INTEREST",
      name: item.name,
      symbol: item.symbol,
      price: item.price,
      changePct: item.changePct,
      direction: item.direction,
      logo: item.logo,
      market: "관심",
      volume: "—",
      detail: "",
      metrics: [],
    })
  }
  const quant = desk?.quant
  const toolNote = tool === "예측"
    ? (brief?.headline || `${selected.name} 예측을 불러오는 중입니다.`)
    : tool === "지표"
      ? (candles.length > 0 ? `상승봉 ${candles.filter((bar) => bar.up).length}개 / 전체 ${candles.length}개` : "봉 데이터를 기다리는 중입니다.")
      : tool === "전략" && quant
        ? `롱 ${quant.longWeight}% / 헤지 ${quant.hedge}% / 안전 ${quant.safe}%`
        : tool === "비교"
          ? "티커의 나스닥100·S&P500과 이 종목 등락을 함께 봅니다."
          : tool === "도구" && legend
            ? `${legend.date} 시가 ${legend.open} 종가 ${legend.close}`
            : `${selected.name} 분석은 아래 종목분석 탭에 있습니다.`

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
                <button
                  type="button"
                  className={btnOutline}
                  aria-pressed={menu === "add"}
                  onClick={() => {
                    setMenu((current) => current === "add" ? null : "add")
                    setMenuNote("")
                  }}
                >
                  + 종목추가
                </button>
                <button
                  type="button"
                  className={btnOutline}
                  aria-pressed={menu === "settings"}
                  onClick={() => setMenu((current) => current === "settings" ? null : "settings")}
                >
                  ⚙ 설정
                </button>
              </div>
              {menu === "add" ? (
                <form
                  className="ep-as-menu-panel"
                  onSubmit={(event) => {
                    event.preventDefault()
                    addStock()
                  }}
                >
                  <input
                    value={symbolInput}
                    onChange={(event) => setSymbolInput(event.target.value)}
                    placeholder="종목코드 (005930, NVDA)"
                    aria-label="종목코드"
                  />
                  <input
                    value={nameInput}
                    onChange={(event) => setNameInput(event.target.value)}
                    placeholder="이름 (비우면 코드)"
                    aria-label="종목 이름"
                  />
                  <button type="submit" className={btnOutline}>목록에 넣기</button>
                  {menuNote ? <p className="ep-as-menu-note">{menuNote}</p> : null}
                </form>
              ) : null}
              {menu === "settings" ? (
                <div className="ep-as-menu-panel">
                  <p className="ep-as-menu-note">보일 묶음을 고릅니다.</p>
                  {LANES.map((lane) => (
                    <label key={lane} className="ep-as-lane-toggle">
                      <input
                        type="checkbox"
                        checked={!hidden.includes(lane)}
                        onChange={() => toggleLane(lane)}
                      />
                      {lane}
                    </label>
                  ))}
                  <button
                    type="button"
                    className={btnOutline}
                    onClick={() => {
                      setCustom([])
                      setMenuNote("직접 넣은 종목을 지웠습니다.")
                    }}
                  >
                    내 종목 비우기
                  </button>
                  {menuNote ? <p className="ep-as-menu-note">{menuNote}</p> : null}
                </div>
              ) : null}
              <div className="stock-list ep-as-stock-list" id="watchlist-sortable-list">
                {groups.length === 0 ? <p className="ep-as-menu-note">설정에서 묶음을 다시 켜 주세요.</p> : null}
                {groups.map((group) => {
                  const open = opened.includes(group.title)
                  return (
                    <div key={group.title}>
                      <button
                        type="button"
                        className="splitter-wrapper ep-as-splitter"
                        aria-expanded={open}
                        onClick={() => toggleOpen(group.title)}
                      >
                        <span className="ep-as-caret" aria-hidden>{open ? "▾" : "▸"}</span>
                        <span>{group.title}</span>
                        <span className="ep-as-lane-count">{group.stocks.length}</span>
                      </button>
                      {open ? group.stocks.map((row) => {
                  const active =
                    row.symbol === selected.symbol && row.name === selected.name
                  return (
                    <button
                      key={`${group.title}-${row.symbol}`}
                      type="button"
                      className={`watchlist-item ep-as-watchlist-item${active ? " ep-as-watchlist-item--active" : ""}`}
                      onClick={() => setSelected(row)}
                    >
                      <div className="a-logo ep-as-logo">
                        <StockMark src={row.logo} label={row.name} size={28} />
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
                      }) : null}
                    </div>
                  )
                })}
              </div>
            </aside>

            <div className="ep-as-center">
              <div id="multi-chart" className="multi-chart ep-as-multi-chart">
                <div id="stock-nav" className="stock-nav ep-as-stock-nav" data-dir={dir}>
                  <div className="stock-nav-stock-info ep-as-stock-nav-main">
                    <div className="logo-sm">
                      <StockMark src={selected.logo} label={selected.name} size={32} />
                    </div>
                    <div>
                      <h2 className="name">{selected.name}</h2>
                      <span className="meta">
                        {selected.market || (selected.symbol === "005930" ? "코스피" : "데모")}
                        <span className="dot"> ㆍ </span>
                        {selected.symbol}
                      </span>
                    </div>
                    <h2 className="price-big">{selected.price}</h2>
                    <span className="chg">{selected.changePct}</span>
                    <span className="dot">ㆍ</span>
                    <span className="vol">
                      거래량 {selected.volume || (selected.symbol === "005930" ? "33,395,939" : "—")}
                    </span>
                  </div>
                </div>

                <div className="chart-nav ep-as-chart-nav">
                  <div className="tools">
                    {TOOLS.map((label) => (
                      <button
                        key={label}
                        type="button"
                        className={tool === label
                          ? "inline-flex h-7 items-center justify-center rounded-lg border border-finsight-secondary bg-finsight-light px-2.5 text-xs font-semibold text-finsight-primary"
                          : btnSmOutline}
                        onClick={() => applyTool(label)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                  <div className="ep-as-periods">
                    {PERIODS.map((item) => (
                      <button
                        key={item}
                        type="button"
                        className={
                          period === item
                            ? "rounded-lg border border-finsight-secondary bg-finsight-light px-2.5 py-1 text-xs font-semibold text-finsight-primary shadow-sm"
                            : "rounded-lg border border-transparent bg-white px-2.5 py-1 text-xs font-medium text-gray-600 transition hover:border-gray-200 hover:bg-gray-50"
                        }
                        onClick={() => {
                          setPeriod(item)
                          setChartLabel(item)
                          setChartState("loading")
                          setFocus(null)
                        }}
                      >
                        {item}
                      </button>
                    ))}
                  </div>
                </div>

                <div id="multi-chart__chart" className="multi-chart__chart-area ep-as-chart-area">
                  <div className="primechart ep-as-primechart">
                    <div className="chart__legend chart__legend--date-and-candle ep-as-chart-legend">
                      <span>{legendDate}</span>
                      <span>시 <span className="hl">{legend?.open ?? "—"}</span></span>
                      <span>고 <span className="hl">{legend?.high ?? "—"}</span></span>
                      <span>저 <span className="hl">{legend?.low ?? "—"}</span></span>
                      <span>종 <span className="hl">{legend?.close ?? selected.price}</span></span>
                    </div>
                    <div className="primechart__chart ep-as-chart-canvas">
                      <div className="ep-as-candles" aria-hidden onMouseLeave={() => setFocus(null)}>
                        {chartHeights.map((bar, idx) => (
                          <div
                            key={`${shownBars[idx]?.date ?? "demo"}-${idx}`}
                            className={`ep-as-candle${focus === idx ? " ep-as-candle--on" : ""}`}
                            style={{
                              height: bar.height,
                              background: bar.up ? "var(--ep-up)" : "var(--ep-down)",
                            }}
                            onMouseEnter={() => {
                              if (shownBars[idx]) setFocus(idx)
                            }}
                          />
                        ))}
                      </div>
                    </div>
                    <p className="ep-as-news-meta">{toolNote}</p>
                    <div className="ep-as-subchart" />
                    <div className="primechart__botbar ep-as-botbar" />
                  </div>
                </div>
              </div>

              <div className="ticker-tape ep-as-ticker" aria-label="시세 티커">
                <span className="ep-as-ticker-lane">{tapeLane ?? "관심"}</span>
                <div className="ep-as-ticker-track">
                  {tickerDup.map((item, idx) => (
                    <button
                      key={`${item.symbol}-${item.name}-${idx}`}
                      type="button"
                      className="ticker-tape-contents-item ep-as-ticker-item"
                      data-dir={item.direction}
                      onClick={() => pickTape(item)}
                    >
                      <div className="ep-as-ticker-logo">
                        <StockMark src={item.logo} label={item.name} size={20} />
                      </div>
                      <span className="nm">{item.name}</span>
                      <span className="pr">{item.price}</span>
                      <span className="ch">{item.changePct}</span>
                    </button>
                  ))}
                </div>
              </div>

              <EconomyPickDesk
                mainTab={mainTab}
                subTab={subTab}
                infoTab={infoTab}
                onMain={setMainTab}
                onSub={setSubTab}
                onInfo={setInfoTab}
                selected={selected}
                desk={desk}
                assets={assets}
                sales={sales}
                brief={brief}
                bars={bars}
                peers={sameLane(listed, selected)}
                news={stockNews}
                newsNote={newsNote}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
