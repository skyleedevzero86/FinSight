export type PriceBar = {
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export type Candle = {
  height: string
  up: boolean
}

type Json = Record<string, unknown>

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Json
  return null
}

function num(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0
}

export function candlesOf(bars: PriceBar[]): Candle[] {
  const tail = bars.slice(-40)
  if (tail.length === 0) return []
  const closes = tail.map((bar) => bar.close)
  const min = Math.min(...closes)
  const span = Math.max(...closes) - min || 1
  return tail.map((bar) => ({
    height: `${Math.max(12, ((bar.close - min) / span) * 100)}%`,
    up: bar.close >= bar.open,
  }))
}

export type ChartLegend = {
  date: string
  open: string
  high: string
  low: string
  close: string
}

export function legendOf(bars: PriceBar[], label: string, index: number | null): ChartLegend | null {
  if (bars.length === 0) return null
  const at = index == null || index < 0 || index >= bars.length ? bars.length - 1 : index
  const bar = bars[at]
  const prev = bars[at - 1]
  const change = prev && prev.close !== 0 ? ((bar.close - prev.close) / prev.close) * 100 : 0
  const mark = `${change >= 0 ? "+" : ""}${change.toFixed(2)}%`
  return {
    date: `${bar.date} · ${label || "일"}`,
    open: money(bar.open),
    high: money(bar.high),
    low: money(bar.low),
    close: `${money(bar.close)} (${mark})`,
  }
}

export function rsi14(bars: PriceBar[]): number | null {
  if (bars.length < 15) return null
  let gain = 0
  let loss = 0
  const start = bars.length - 14
  for (let i = start; i < bars.length; i += 1) {
    const diff = bars[i].close - bars[i - 1].close
    if (diff >= 0) gain += diff
    else loss -= diff
  }
  if (loss === 0) return 100
  return 100 - 100 / (1 + gain / loss)
}

export function sma(bars: PriceBar[], period: number): number | null {
  if (bars.length < period) return null
  const tail = bars.slice(-period)
  const sum = tail.reduce((acc, bar) => acc + bar.close, 0)
  return sum / period
}

function money(value: number): string {
  return value.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

export async function fetchEconomyPickChart(symbol: string, market: string, period: string): Promise<{
  label: string
  bars: PriceBar[]
} | null> {
  try {
    const query = new URLSearchParams({ symbol, market, period })
    const res = await fetch(`/api/v1/economy-pick/chart?${query}`, { cache: "no-store" })
    if (!res.ok) return null
    const data = asRecord(asRecord(await res.json())?.data)
    if (!data || !Array.isArray(data.bars)) return null
    const bars = data.bars.flatMap((row) => {
      const record = asRecord(row)
      if (!record || typeof record.date !== "string") return []
      return [{
        date: record.date,
        open: num(record.open),
        high: num(record.high),
        low: num(record.low),
        close: num(record.close),
        volume: num(record.volume),
      }]
    })
    return { label: typeof data.label === "string" ? data.label : period, bars }
  } catch {
    return null
  }
}
