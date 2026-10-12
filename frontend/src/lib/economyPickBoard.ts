export type EconomyPickTape = {
  symbol: string
  name: string
  price: string
  changePct: string
  direction: "up" | "down" | "zero"
  logo: string
}

export type EconomyPickRow =
  | { kind: "splitter"; title: string }
  | {
      kind: "stock"
      lane: string
      name: string
      symbol: string
      price: string
      changePct: string
      direction: "up" | "down" | "zero"
      logo: string
      market: string
      volume: string
      detail: string
      metrics: string[]
    }

type Json = Record<string, unknown>

const LANES: Record<string, string> = {
  DASHBOARD: "코스닥 시그널",
  QUANT: "퀀트",
  SPACEX: "SpaceX",
  TRUMP: "Trump Pick",
}

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Json
  return null
}

function text(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

const PLAIN_MARK = new Set(["VIX", "FG"])

export function marketLogo(symbol: string, logo: string): string {
  const ticker = usTicker(symbol) || usTicker(symbolFromLogo(logo)) || usTicker(symbolFromParqet(logo))
  if (PLAIN_MARK.has(ticker)) return ""
  if (!ticker) return logo
  return `https://assets.parqet.com/logos/symbol/${ticker}`
}

function usTicker(symbol: string): string {
  const cleaned = symbol.trim().replace(/^\^/, "").toUpperCase()
  return /^[A-Z][A-Z0-9.-]{0,11}$/.test(cleaned) ? cleaned : ""
}

function symbolFromLogo(logo: string): string {
  const matched = logo.match(/\/stock_logo\/us\/([^/]+)\.png$/i)
  return matched ? matched[1] : ""
}

function symbolFromParqet(logo: string): string {
  const matched = logo.match(/\/logos\/symbol\/([^/?#]+)$/i)
  return matched ? matched[1] : ""
}

function directionOf(value: string): "up" | "down" | "zero" {
  if (value === "up" || value === "down" || value === "zero") return value
  return "zero"
}

function metricsOf(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is string => typeof item === "string")
}

function stockOf(row: unknown): Extract<EconomyPickRow, { kind: "stock" }> | null {
  const record = asRecord(row)
  if (!record) return null
  const symbol = text(record.symbol)
  const name = text(record.name)
  if (!symbol || !name) return null
  return {
    kind: "stock",
    lane: text(record.lane),
    name,
    symbol,
    price: text(record.price) || "—",
    changePct: text(record.changePct) || "—",
    direction: directionOf(text(record.direction)),
    logo: marketLogo(symbol, text(record.logo)),
    market: text(record.market),
    volume: text(record.volume),
    detail: text(record.detail),
    metrics: metricsOf(record.metrics),
  }
}

export function boardRows(rows: unknown[]): EconomyPickRow[] {
  const grouped: EconomyPickRow[] = []
  let lane = ""
  for (const row of rows) {
    const stock = stockOf(row)
    if (!stock) continue
    if (stock.lane !== lane) {
      lane = stock.lane
      grouped.push({ kind: "splitter", title: LANES[lane] || lane || "종목" })
    }
    grouped.push(stock)
  }
  return grouped
}

export type QuantStock = {
  ticker: string
  name: string
  price: string
  changePct: string
  signal: string
  rsi: number
  score: number
}

export type QuantView = {
  regimeLabel: string
  regimeSignal: string
  vix: number
  fearGreed: number
  fearGreedStatus: string
  longWeight: number
  hedge: number
  safe: number
  amqs: QuantStock[]
  hynixSignal: string
  hynixPredictedChangePct: number
}

export type SpaceStock = {
  symbol: string
  name: string
  group: string
  price: string
  changePct: string
  signal: string
  score: number
  rsi: number | null
  williamsR: number | null
}

export type TrumpPick = {
  ticker: string
  name: string
  action: string
  confidence: number
  reason: string
}

export type TrumpView = {
  summary: string
  mood: string
  marketImpact: string
  picks: TrumpPick[]
}

export type DashSignal = {
  name: string
  symbol: string
  price: string
  risk: number
  rules: string
  detail: string
  highRisk: boolean
}

export type DashView = {
  companyCount: number
  highRiskCount: number
  signals: DashSignal[]
}

export type EconomyPickDesk = {
  quant: QuantView | null
  spacex: SpaceStock[]
  trump: TrumpView | null
  dashboard: DashView | null
}

function num(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0
}

function quantOf(value: unknown): QuantView | null {
  const record = asRecord(value)
  const hedge = asRecord(record?.hedge)
  const hynix = asRecord(record?.hynix)
  if (!record || !text(record.regimeLabel)) return null
  const amqs = Array.isArray(record.amqs) ? record.amqs.flatMap((row) => {
    const item = asRecord(row)
    if (!item || !text(item.name)) return []
    return [{
      ticker: text(item.ticker),
      name: text(item.name),
      price: text(item.price),
      changePct: text(item.changePct),
      signal: text(item.signal),
      rsi: num(item.rsi),
      score: num(item.score),
    }]
  }) : []
  return {
    regimeLabel: text(record.regimeLabel),
    regimeSignal: text(record.regimeSignal),
    vix: num(record.vix),
    fearGreed: num(record.fearGreed),
    fearGreedStatus: text(record.fearGreedStatus),
    longWeight: num(hedge?.longWeight),
    hedge: num(hedge?.hedge),
    safe: num(hedge?.safe),
    amqs,
    hynixSignal: text(hynix?.hynixSignal),
    hynixPredictedChangePct: num(hynix?.hynixPredictedChangePct),
  }
}

function spaceOf(value: unknown): SpaceStock[] {
  const record = asRecord(value)
  if (!record || !Array.isArray(record.stocks)) return []
  return record.stocks.flatMap((row) => {
    const item = asRecord(row)
    if (!item || !text(item.name)) return []
    return [{
      symbol: text(item.symbol),
      name: text(item.name),
      group: text(item.group),
      price: text(item.price),
      changePct: text(item.changePct),
      signal: text(item.signal),
      score: num(item.score),
      rsi: typeof item.rsi === "number" ? item.rsi : null,
      williamsR: typeof item.williamsR === "number" ? item.williamsR : null,
    }]
  })
}

function trumpOf(value: unknown): TrumpView | null {
  const record = asRecord(value)
  if (!record) return null
  const picks = Array.isArray(record.picks) ? record.picks.flatMap((row) => {
    const item = asRecord(row)
    if (!item || !text(item.name)) return []
    return [{
      ticker: text(item.ticker),
      name: text(item.name),
      action: text(item.action),
      confidence: num(item.confidence),
      reason: text(item.reason),
    }]
  }) : []
  if (!text(record.summary) && picks.length === 0) return null
  return {
    summary: text(record.summary),
    mood: text(record.mood),
    marketImpact: text(record.marketImpact),
    picks,
  }
}

function dashboardOf(value: unknown): DashView | null {
  const record = asRecord(value)
  if (!record || !Array.isArray(record.signals)) return null
  const signals = record.signals.flatMap((row) => {
    const item = asRecord(row)
    if (!item || !text(item.name)) return []
    return [{
      name: text(item.name),
      symbol: text(item.symbol),
      price: text(item.price),
      risk: num(item.risk),
      rules: text(item.rules),
      detail: text(item.detail),
      highRisk: item.highRisk === true,
    }]
  })
  return {
    companyCount: num(record.companyCount),
    highRiskCount: num(record.highRiskCount),
    signals,
  }
}

export function deskOf(data: Json): EconomyPickDesk {
  return {
    quant: quantOf(data.quant),
    spacex: spaceOf(data.spacex),
    trump: trumpOf(data.trump),
    dashboard: dashboardOf(data.dashboard),
  }
}

export async function fetchEconomyPickBoard(): Promise<{
  rows: EconomyPickRow[]
  tickers: EconomyPickTape[]
  desk: EconomyPickDesk
} | null> {
  try {
    const res = await fetch("/api/v1/economy-pick/board", { cache: "no-store" })
    if (!res.ok) return null
    const root = asRecord(await res.json())
    const data = asRecord(root?.data) ?? root
    if (!data) return null
    const rows = boardRows(Array.isArray(data.rows) ? data.rows : [])
    const tickers = (Array.isArray(data.tickers) ? data.tickers : [])
      .map((row) => {
        const record = asRecord(row)
        if (!record || !text(record.name)) return null
        return {
          symbol: text(record.symbol),
          name: text(record.name),
          price: text(record.price),
          changePct: text(record.changePct),
          direction: directionOf(text(record.direction)),
          logo: marketLogo("", text(record.logo)),
        }
      })
      .filter((row): row is EconomyPickTape => row !== null)
    return rows.length > 0 ? { rows, tickers, desk: deskOf(data) } : null
  } catch {
    return null
  }
}
