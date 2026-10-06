import { authHeadersJson } from "@/lib/finsightToken"
import type { BrowseHistoryItem } from "@/lib/browseHistory"

export type HistorySortMode = "DATE" | "POPULAR"

export type HistoryPopularityItem = {
  key: string
  kind: "LIVE_VOD" | "BOARD"
  id: string
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

export function toPopularityQueryItems(items: BrowseHistoryItem[]): HistoryPopularityItem[] {
  const out: HistoryPopularityItem[] = []
  for (const item of items) {
    if (item.kind === "LIVE_VOD") {
      const m = item.key.match(/^live-vod:(.+)$/)
      if (!m?.[1]) continue
      out.push({ key: item.key, kind: "LIVE_VOD", id: m[1] })
      continue
    }
    const m = item.key.match(/^board:(\d+)$/)
    if (!m?.[1]) continue
    out.push({ key: item.key, kind: "BOARD", id: m[1] })
  }
  return out
}

export async function fetchHistoryPopularityScores(
  items: HistoryPopularityItem[],
): Promise<Record<string, number>> {
  if (items.length === 0) return {}
  try {
    const res = await fetch("/api/v1/media/history/popularity", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...authHeadersJson(),
      },
      body: JSON.stringify({ items }),
      cache: "no-store",
    })
    if (!res.ok) return {}
    const json: unknown = await res.json().catch(() => null)
    const root = asRecord(json)
    const data = asRecord(root?.data)
    const scoresRaw = data?.scores
    if (!Array.isArray(scoresRaw)) return {}
    const map: Record<string, number> = {}
    for (const row of scoresRaw) {
      const o = asRecord(row)
      if (!o || typeof o.key !== "string" || !o.key) continue
      map[o.key] = Number(o.score) || 0
    }
    return map
  } catch {
    return {}
  }
}
