import { NEWS_ITEMS, TICKER_ITEMS } from "@/data/finsightEconomyPickData"
import { authHeadersJson } from "@/lib/finsightToken"
import { unwrapApiData } from "@/lib/boardApi"
import {
  fetchLiveVodFeed,
  flattenLiveVodFeedItems,
  liveVodWatchHref,
} from "@/lib/liveVod"
import { listLiveVodHistory } from "@/lib/liveVodHistory"

export type MyInfoNotice = {
  id: number
  title: string
  timeLabel: string
  href: string
}

export type MyInfoThumb = {
  key: string
  title: string
  href: string
  image: string | null
  meta: string
  external: boolean
}

export type MyInfoActivity = {
  key: string
  title: string
  href: string
  timeLabel: string
}

export type MyInfoMarket = {
  id: string
  label: string
  price: string
  change: string
  up: boolean
}

export type MyInfoHomeData = {
  notices: MyInfoNotice[]
  continueItems: MyInfoThumb[]
  interestItems: MyInfoThumb[]
  activities: MyInfoActivity[]
  recommends: MyInfoThumb[]
}

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"] as const

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readText(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

export function formatKoreanDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}년 ${month}월 ${day}일 (${WEEKDAYS[date.getDay()]})`
}

export function formatRelativeKo(iso: string | null | undefined): string {
  if (!iso) return ""
  const at = new Date(iso).getTime()
  if (!Number.isFinite(at)) return ""
  const minutes = Math.floor((Date.now() - at) / 60000)
  if (minutes < 1) return "방금 전"
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}일 전`
  return `${Math.floor(days / 30)}개월 전`
}

export function myInfoMarkets(): MyInfoMarket[] {
  const picked = ["코스피", "코스닥", "미국 USD"]
    .map((name) => TICKER_ITEMS.find((item) => item.name === name))
    .filter((item): item is (typeof TICKER_ITEMS)[number] => Boolean(item))
  return picked.map((item) => ({
    id: item.name,
    label: item.name === "미국 USD" ? "원/달러 환율" : item.name,
    price: item.price,
    change: item.chg,
    up: item.direction !== "down",
  }))
}

function envelopeData(payload: unknown): Record<string, unknown> | null {
  const root = asRecord(payload)
  if (!root) return null
  return asRecord(root.data) ?? root
}

function contentRows(payload: unknown): Record<string, unknown>[] {
  const data = envelopeData(payload)
  const rows = data?.content
  if (!Array.isArray(rows)) return []
  return rows.map(asRecord).filter((row): row is Record<string, unknown> => Boolean(row))
}

async function readPayload(path: string): Promise<unknown | null> {
  try {
    const res = await fetch(path, {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (!res.ok) return null
    return await res.json()
  } catch (error) {
    console.error("마이페이지 데이터를 불러오지 못했습니다.", path, error)
    return null
  }
}

function parseNotices(payload: unknown, limit: number): MyInfoNotice[] {
  return contentRows(payload)
    .map((row) => {
      const id = Number(row.id)
      const title = readText(row.title)
      if (!Number.isFinite(id) || !title) return null
      const timeAgo = readText(row.timeAgo)
      return {
        id,
        title,
        timeLabel: timeAgo || formatRelativeKo(readText(row.createdAt)),
        href: `/community/notice/${id}`,
      }
    })
    .filter((row): row is MyInfoNotice => Boolean(row))
    .slice(0, limit)
}

export async function loadNoticeCards(limit = 5): Promise<MyInfoNotice[]> {
  return parseNotices(await readPayload(`/api/v1/boards?boardType=NOTICE&page=0&size=${limit}`), limit)
}

function newsFields(row: Record<string, unknown>): {
  title: string
  image: string | null
  url: string | null
} {
  const translated = asRecord(row.translatedContent)
  const original = asRecord(row.originalContent)
  const meta = asRecord(row.newsMeta)
  const title = readText(translated?.title) || readText(original?.title)
  const image = readText(translated?.imageUrl) || readText(original?.imageUrl)
  const url =
    readText(translated?.url) || readText(original?.url) || readText(meta?.sourceUrl)
  return { title, image: image || null, url: url || null }
}

function parseNews(payload: unknown): MyInfoThumb[] {
  const data = unwrapApiData<unknown>(payload) ?? envelopeData(payload)
  const bag = asRecord(data)
  const rows = Array.isArray(bag?.newses) ? bag.newses : []
  return rows
    .map((raw, index) => {
      const row = asRecord(raw)
      if (!row) return null
      const fields = newsFields(row)
      if (!fields.title) return null
      const external = Boolean(fields.url && /^https?:\/\//.test(fields.url))
      const when = formatRelativeKo(readText(row.scrapedTime))
      return {
        key: `news-${readText(row.id) || index}`,
        title: fields.title,
        href: external && fields.url ? fields.url : "/news",
        image: fields.image,
        meta: when ? `뉴스 · ${when}` : "뉴스",
        external,
      }
    })
    .filter((row): row is MyInfoThumb => Boolean(row))
}

function fallbackNews(): MyInfoThumb[] {
  return NEWS_ITEMS.slice(0, 3).map((item) => ({
    key: item.href,
    title: item.title,
    href: item.href,
    image: item.thumb ?? null,
    meta: `${item.source} · ${item.time}`,
    external: item.href.startsWith("http"),
  }))
}

function memberBoardHref(boardType: string, id: number): string {
  const kind = boardType.toUpperCase()
  if (kind === "NOTICE") return `/community/notice/${id}`
  if (kind === "QNA") return `/community/qna/${id}`
  return `/community/free/${id}`
}

function parseActivities(inboxPayload: unknown, boardPayload: unknown): MyInfoActivity[] {
  const fromInbox = contentRows(inboxPayload)
    .map((row) => {
      const title = readText(row.title)
      const id = readText(row.id) || title
      if (!title) return null
      const link = readText(row.linkUrl)
      return {
        key: `inbox-${id}`,
        title,
        href: link && link.startsWith("/") ? link : "/myinfo/posts",
        timeLabel: formatRelativeKo(readText(row.createdAt)),
      }
    })
    .filter((row): row is MyInfoActivity => Boolean(row))
  if (fromInbox.length) return fromInbox.slice(0, 3)

  return contentRows(boardPayload)
    .map((row) => {
      const title = readText(row.title)
      const id = Number(row.id)
      if (!title || !Number.isFinite(id)) return null
      const timeAgo = readText(row.timeAgo)
      return {
        key: `board-${id}`,
        title,
        href: memberBoardHref(readText(row.boardType), id),
        timeLabel: timeAgo || formatRelativeKo(readText(row.createdAt)),
      }
    })
    .filter((row): row is MyInfoActivity => Boolean(row))
    .slice(0, 3)
}

function historyThumbs(): MyInfoThumb[] {
  return listLiveVodHistory().slice(0, 3).map((item) => ({
    key: item.videoId,
    title: item.title,
    href: `/live-vod/watch/${encodeURIComponent(item.videoId)}`,
    image: item.thumbnailUrl,
    meta: item.channelTitle || "이어보기",
    external: false,
  }))
}

function vodThumbs(
  items: { videoId: string; title: string; thumbnailUrl: string; channelTitle: string | null }[],
  meta: string,
): MyInfoThumb[] {
  return items.map((item) => ({
    key: item.videoId,
    title: item.title,
    href: liveVodWatchHref(item),
    image: item.thumbnailUrl,
    meta: item.channelTitle ? `${meta} · ${item.channelTitle}` : meta,
    external: false,
  }))
}

export async function loadMyInfoHome(): Promise<MyInfoHomeData> {
  const [notices, personalPayload, latestPayload, inboxPayload, boardPayload, vod] =
    await Promise.all([
      loadNoticeCards(),
      readPayload("/api/v1/news/personalized?limit=6"),
      readPayload("/api/v1/news/latest?limit=6"),
      readPayload("/api/v1/inbox?page=0&size=3"),
      readPayload("/api/v1/boards/my-boards?page=0&size=3"),
      fetchLiveVodFeed("ALL"),
    ])

  const vodItems = vod.ok ? flattenLiveVodFeedItems(vod.data) : []
  const history = historyThumbs()
  const continueItems = history.length ? history : vodThumbs(vodItems.slice(0, 3), "실시간 VOD")
  const used = new Set(continueItems.map((item) => item.key))
  const interestPool = vodItems.filter((item) => !used.has(item.videoId))
  const news = [...parseNews(personalPayload), ...parseNews(latestPayload)]
  const uniqueNews = news.filter((item, index) => news.findIndex((row) => row.title === item.title) === index)

  return {
    notices,
    continueItems,
    interestItems: vodThumbs(interestPool.slice(0, 3), "VOD"),
    activities: parseActivities(inboxPayload, boardPayload),
    recommends: (uniqueNews.length ? uniqueNews : fallbackNews()).slice(0, 3),
  }
}
