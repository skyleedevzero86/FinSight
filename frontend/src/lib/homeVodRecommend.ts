import {
  fetchLiveVodFeed,
  flattenLiveVodFeedItems,
  shuffleLiveVodItems,
  type LiveVodItem,
} from "@/lib/liveVod"
import type { TargetCategory } from "@/lib/registration"

export const HOME_VOD_RECOMMEND_TABS = [
  "LIVE",
  "MARKET",
  "GOMHEE",
  "BOOTYFUL",
  "THEME",
  "MACRO",
  "SYUKA",
] as const

export type HomeVodRecommendTab = (typeof HOME_VOD_RECOMMEND_TABS)[number]

export type RecommendedLiveVodItem = LiveVodItem & {
  sourceTab: HomeVodRecommendTab
}

const CATEGORY_KEYWORDS: Record<TargetCategory, string[]> = {
  GENERAL: [],
  NONE: [],
  SPY: ["s&p", "sp500", "s&p500", "spy", "증시", "시장", "브리핑", "etf", "지수"],
  QQQ: ["nasdaq", "나스닥", "qqq", "테크", "기술주", "etf"],
  BTC: ["bitcoin", "btc", "비트코인", "암호화폐", "가상자산", "코인"],
  BITCOIN: ["bitcoin", "btc", "비트코인", "암호화폐", "가상자산", "코인"],
  AAPL: ["apple", "aapl", "애플"],
  MSFT: ["microsoft", "msft", "마이크로소프트"],
  NVDA: ["nvidia", "nvda", "엔비디아"],
  GOOGL: ["google", "alphabet", "googl", "구글", "알파벳"],
  META: ["meta", "facebook", "메타", "페이스북"],
  TSLA: ["tesla", "tsla", "테슬라"],
}

const CATEGORY_PREFERRED_TABS: Partial<Record<TargetCategory, HomeVodRecommendTab[]>> = {
  SPY: ["MARKET", "THEME", "MACRO", "LIVE"],
  QQQ: ["MARKET", "THEME", "MACRO", "LIVE"],
  BTC: ["MACRO", "THEME", "LIVE"],
  BITCOIN: ["MACRO", "THEME", "LIVE"],
  AAPL: ["THEME", "MARKET", "GOMHEE", "SYUKA"],
  MSFT: ["THEME", "MARKET", "GOMHEE", "SYUKA"],
  NVDA: ["THEME", "MARKET", "MACRO", "SYUKA"],
  GOOGL: ["THEME", "MARKET", "MACRO"],
  META: ["THEME", "MARKET", "MACRO"],
  TSLA: ["THEME", "MARKET", "GOMHEE", "BOOTYFUL"],
}

function dedupeItems(items: RecommendedLiveVodItem[]): RecommendedLiveVodItem[] {
  const seen = new Set<string>()
  const out: RecommendedLiveVodItem[] = []
  for (const item of items) {
    if (!item.videoId || seen.has(item.videoId)) continue
    seen.add(item.videoId)
    out.push(item)
  }
  return out
}

export async function fetchHomeRecommendPool(): Promise<RecommendedLiveVodItem[]> {
  const batches = await Promise.all(
    HOME_VOD_RECOMMEND_TABS.map(async (tab) => {
      const result = await fetchLiveVodFeed(tab)
      if (!result.ok) return [] as RecommendedLiveVodItem[]
      return flattenLiveVodFeedItems(result.data).map((item) => ({
        ...item,
        sourceTab: tab,
      }))
    }),
  )
  return dedupeItems(batches.flat())
}

function haystackOf(item: LiveVodItem): string {
  return `${item.title || ""} ${item.channelTitle || ""}`.toLowerCase()
}

function relevanceScore(item: RecommendedLiveVodItem, watchlist: TargetCategory[]): number {
  if (!watchlist.length) return 0
  const hay = haystackOf(item)
  let score = 0
  for (const category of watchlist) {
    if (category === "GENERAL" || category === "NONE") continue
    const keywords = CATEGORY_KEYWORDS[category] || []
    for (const keyword of keywords) {
      if (keyword && hay.includes(keyword.toLowerCase())) {
        score += 3
        break
      }
    }
    const preferred = CATEGORY_PREFERRED_TABS[category]
    if (preferred?.includes(item.sourceTab)) score += 1
  }
  return score
}

export function pickRecommendedItems(
  pool: RecommendedLiveVodItem[],
  options: {
    watchlist: TargetCategory[]
    loggedIn: boolean
    limit: number
  },
): RecommendedLiveVodItem[] {
  if (pool.length === 0) return []
  const meaningful = options.watchlist.filter((c) => c !== "GENERAL" && c !== "NONE")
  if (!options.loggedIn || meaningful.length === 0) {
    return shuffleLiveVodItems(pool).slice(0, options.limit)
  }

  const scored = pool
    .map((item) => ({ item, score: relevanceScore(item, meaningful) }))
    .sort((a, b) => b.score - a.score)

  const matched = scored.filter((row) => row.score > 0).map((row) => row.item)
  if (matched.length >= options.limit) {
    return shuffleLiveVodItems(matched.slice(0, Math.min(matched.length, options.limit * 2))).slice(
      0,
      options.limit,
    )
  }

  const matchedIds = new Set(matched.map((item) => item.videoId))
  const fillers = shuffleLiveVodItems(pool.filter((item) => !matchedIds.has(item.videoId)))
  return [...matched, ...fillers].slice(0, options.limit)
}
