export const NEWS_ADMIN_NOTICE = "관리자에게 문의주세요."

export type StoredNewsCard = {
  id: number
  title: string
  summary: string
  categories: string[]
  publishedAt: string | null
  sentiment: string | null
  aiReady: boolean
}

export type StoredNewsDetail = StoredNewsCard & {
  body: string
  sentiment: string | null
}

type Json = Record<string, unknown>

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Json
  }
  return null
}

function text(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

function koreanOrNotice(translated: string, original: string): { value: string; ready: boolean } {
  if (translated && translated !== original) {
    return { value: translated, ready: true }
  }
  return { value: original || "제목 없음", ready: false }
}

function categoriesOf(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is string => typeof item === "string" && item.trim() !== "")
}

function publishedOf(row: Json): string | null {
  const meta = asRecord(row.newsMeta)
  const raw = text(meta?.newsPublishedTime) || text(meta?.publishedTime) || text(row.publishedTime)
  return raw || null
}

export function cardFromNews(row: Json): StoredNewsCard | null {
  const id = typeof row.id === "number" ? row.id : Number(row.id)
  if (!Number.isFinite(id) || id <= 0) return null
  const original = asRecord(row.originalContent)
  const translated = asRecord(row.translatedContent)
  const overview = asRecord(row.aiOverView) ?? asRecord(row.aiOverview)
  const originalTitle = text(original?.title)
  const translatedTitle = text(translated?.title)
  const title = koreanOrNotice(translatedTitle, originalTitle)
  const summaryText = text(overview?.overview)
  return {
    id,
    title: title.value,
    summary: title.ready && summaryText ? summaryText : text(original?.content),
    categories: categoriesOf(overview?.targetCategories),
    publishedAt: publishedOf(row),
    sentiment: text(overview?.sentimentType) || null,
    aiReady: title.ready,
  }
}

export function detailFromPayload(row: Json): StoredNewsDetail | null {
  const id = typeof row.id === "number" ? row.id : Number(row.id)
  if (!Number.isFinite(id) || id <= 0) return null
  const originalTitle = text(row.originalTitle)
  const translatedTitle = text(row.translatedTitle)
  const title = koreanOrNotice(translatedTitle, originalTitle)
  const overview = text(row.overview)
  const originalBody = text(row.originalContent)
  const translatedBody = text(row.translatedContent)
  const body = koreanOrNotice(translatedBody, originalBody)
  const sentiment = text(row.sentimentType)
  return {
    id,
    title: title.value,
    summary: title.ready && overview ? overview : text(row.originalContent),
    body: title.ready && body.ready ? body.value : text(row.originalContent),
    sentiment: sentiment || null,
    categories: categoriesOf(row.categories),
    publishedAt: text(row.publishedTime) || null,
    aiReady: title.ready,
  }
}

function payloadOf(body: unknown): unknown {
  const root = asRecord(body)
  if (!root) return null
  return root.data ?? root
}

export async function fetchNewsAiDown(): Promise<boolean> {
  try {
    const res = await fetch("/api/v1/news/ai-status", { cache: "no-store" })
    if (!res.ok) return true
    const data = asRecord(payloadOf(await res.json()))
    return data?.available !== true
  } catch {
    return true
  }
}

export async function fetchStoredNews(limit = 20): Promise<StoredNewsCard[] | null> {
  try {
    const res = await fetch(`/api/v1/news/latest?limit=${limit}`, { cache: "no-store" })
    if (!res.ok) return null
    const data = payloadOf(await res.json())
    const record = asRecord(data)
    const rows = record && Array.isArray(record.newses) ? record.newses : []
    return rows
      .map((row) => cardFromNews(asRecord(row) ?? {}))
      .filter((row): row is StoredNewsCard => row !== null)
  } catch {
    return null
  }
}

export async function fetchStoredNewsDetail(id: string): Promise<StoredNewsDetail | null | "missing"> {
  try {
    const res = await fetch(`/api/v1/news/${encodeURIComponent(id)}`, { cache: "no-store" })
    if (res.status === 404) return "missing"
    if (!res.ok) return null
    const data = payloadOf(await res.json())
    const record = asRecord(data)
    if (!record) return null
    return detailFromPayload(record)
  } catch {
    return null
  }
}

const SUGGESTABLE_CATEGORIES = ["SPY", "QQQ", "BTC", "AAPL", "MSFT", "NVDA", "GOOGL", "META", "TSLA"]

const CATEGORY_LABELS: Record<string, string> = {
  SPY: "S&P 500 ETF",
  QQQ: "나스닥 100 ETF",
  BTC: "비트코인",
  AAPL: "애플",
  MSFT: "마이크로소프트",
  NVDA: "엔비디아",
  GOOGL: "구글",
  META: "메타",
  TSLA: "테슬라",
}

export function categoryLabel(code: string): string {
  return CATEGORY_LABELS[code] ?? code
}

export function sentimentLabel(type: string | null): string | null {
  if (type === "POSITIVE") return "긍정"
  if (type === "NEGATIVE") return "부정"
  if (type === "NEUTRAL") return "중립"
  return null
}

export function sentimentExpression(type: string | null): string | null {
  if (type === "POSITIVE") return "긍정 · 오를 수 있는 흐름"
  if (type === "NEGATIVE") return "부정 · 내릴 수 있는 흐름"
  if (type === "NEUTRAL") return "중립 · 방향이 아직 없음"
  return null
}

function meaningfulCategories(codes: string[]): string[] {
  return codes.filter((code) => code !== "NONE" && code !== "GENERAL" && code !== "BITCOIN")
}

export function recommendTargetCategories(
  news: { categories: string[] }[],
  selected: string[],
  roll: () => number = Math.random,
): { mode: "selected" | "random"; categories: string[] } {
  const picked = meaningfulCategories(selected.map((code) => code.toUpperCase()))
  if (picked.length > 0) {
    const related = new Map<string, number>()
    for (const item of news) {
      const codes = meaningfulCategories(item.categories.map((code) => code.toUpperCase()))
      if (!codes.some((code) => picked.includes(code))) continue
      for (const code of codes) {
        if (picked.includes(code)) continue
        related.set(code, (related.get(code) ?? 0) + 1)
      }
    }
    const extra = [...related.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([code]) => code)
      .slice(0, 3)
    return { mode: "selected", categories: extra.length > 0 ? extra : picked.slice(0, 3) }
  }
  const seen = new Set<string>()
  for (const item of news) {
    for (const code of meaningfulCategories(item.categories.map((value) => value.toUpperCase()))) {
      seen.add(code)
    }
  }
  const pool = seen.size > 0 ? [...seen] : [...SUGGESTABLE_CATEGORIES]
  const shuffled = [...pool].sort(() => roll() - 0.5)
  return { mode: "random", categories: shuffled.slice(0, 3) }
}
