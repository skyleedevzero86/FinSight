export type EconomyPickNewsItem = {
  id: number
  title: string
  source: string
  time: string
  href: string
  thumb: string | null
  external: boolean
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

function relativeTime(value: string): string {
  const then = new Date(value).getTime()
  if (!Number.isFinite(then)) return ""
  const minutes = Math.floor((Date.now() - then) / 60000)
  if (minutes < 1) return "방금 전"
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  return `${Math.floor(hours / 24)}일 전`
}

function itemOf(row: unknown): EconomyPickNewsItem | null {
  const record = asRecord(row)
  if (!record) return null
  const id = typeof record.id === "number" ? record.id : Number(record.id)
  const title = text(record.title)
  if (!Number.isFinite(id) || id <= 0 || !title) return null
  const href = text(record.href) || `/news/${id}`
  const external = href.startsWith("http://") || href.startsWith("https://")
  return {
    id,
    title,
    source: text(record.source) || "뉴스",
    time: relativeTime(text(record.publishedAt)),
    href,
    thumb: text(record.imageUrl) || null,
    external,
  }
}

export async function fetchEconomyPickNews(limit = 6): Promise<{
  mode: string
  items: EconomyPickNewsItem[]
} | null> {
  try {
    const res = await fetch(`/api/v1/economy-pick/news?limit=${limit}`, {
      credentials: "include",
      cache: "no-store",
    })
    if (!res.ok) return null
    const root = asRecord(await res.json())
    const data = asRecord(root?.data) ?? root
    if (!data) return null
    const rows = Array.isArray(data.items) ? data.items : []
    return {
      mode: text(data.mode),
      items: rows.map(itemOf).filter((row): row is EconomyPickNewsItem => row !== null),
    }
  } catch {
    return null
  }
}

export function economyPickNewsNote(mode: string, count: number): string {
  if (count > 0) return ""
  if (mode === "CATEGORY") return "선택한 관심 카테고리의 뉴스가 없습니다."
  return "추천할 종목뉴스가 없습니다."
}
