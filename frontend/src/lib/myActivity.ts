import { unwrapApiData, type BoardListItem } from "@/lib/boardApi"
import { boardHref, fetchMyBoardScraps, fetchMyLiveVodFavorites } from "@/lib/favoritesApi"
import { authHeadersJson } from "@/lib/finsightToken"
import { fetchInboxPage, type InboxCategory } from "@/lib/inbox"
import { listLiveVodHistory } from "@/lib/liveVodHistory"
import { formatRelativeKo } from "@/lib/myInfoHome"

export type ActivityWatchItem = {
  key: string
  title: string
  href: string
  image: string | null
  source: string
  timeLabel: string
}

export type ActivityPostItem = {
  key: string
  title: string
  href: string
  boardLabel: string
  views: number
  comments: number
  likes: number
  timeLabel: string
}

export type ActivityAlertItem = {
  key: string
  title: string
  detail: string
  href: string
  timeLabel: string
  category: InboxCategory | "COMMENT"
  unread: boolean
}

export type MyActivityData = {
  watches: ActivityWatchItem[]
  favorites: ActivityWatchItem[]
  posts: ActivityPostItem[]
  alerts: ActivityAlertItem[]
}

const EMPTY: MyActivityData = {
  watches: [],
  favorites: [],
  posts: [],
  alerts: [],
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readText(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

function boardLabel(boardType: string): string {
  const kind = boardType.toUpperCase()
  if (kind === "NOTICE") return "공지사항"
  if (kind === "QNA") return "투자 Q&A"
  if (kind === "FREE") return "자유게시판"
  return "게시글"
}

function toPost(row: BoardListItem): ActivityPostItem {
  return {
    key: `board-${row.id}`,
    title: row.title,
    href: boardHref(row.boardType, row.id),
    boardLabel: boardLabel(row.boardType),
    views: row.viewCount,
    comments: row.commentCount,
    likes: row.likeCount,
    timeLabel: row.timeAgo || formatRelativeKo(row.createdAt),
  }
}

async function loadPosts(): Promise<ActivityPostItem[]> {
  try {
    const res = await fetch("/api/v1/boards/my-boards?page=0&size=5", {
      headers: { Accept: "application/json", ...authHeadersJson() },
      cache: "no-store",
    })
    if (!res.ok) return []
    const data = unwrapApiData<{ content?: BoardListItem[] }>(await res.json())
    const rows = Array.isArray(data?.content) ? data.content : []
    return rows.slice(0, 5).map(toPost)
  } catch (error) {
    console.error("내 활동 게시글을 불러오지 못했습니다.", error)
    return []
  }
}

async function loadAlerts(): Promise<ActivityAlertItem[]> {
  const inbox = await fetchInboxPage({ page: 0, size: 5 })
  const fromInbox: ActivityAlertItem[] = inbox.ok
    ? inbox.page.content.slice(0, 5).map((item) => ({
        key: `inbox-${item.id}`,
        title: item.title,
        detail: item.body || item.actorName || "알림",
        href: item.linkUrl && item.linkUrl.startsWith("/") ? item.linkUrl : "/myinfo/posts",
        timeLabel: formatRelativeKo(item.createdAt),
        category: item.category,
        unread: !item.read,
      }))
    : []
  if (fromInbox.length) return fromInbox
  return loadMyComments()
}

async function loadMyComments(): Promise<ActivityAlertItem[]> {
  try {
    const res = await fetch("/api/v1/comments/my-comments?page=0&size=5", {
      headers: { Accept: "application/json", ...authHeadersJson() },
      cache: "no-store",
    })
    if (!res.ok) return []
    const data = unwrapApiData<{ content?: unknown[] }>(await res.json())
    const rows = Array.isArray(data?.content) ? data.content : []
    return rows
      .map((raw, index) => {
        const row = asRecord(raw)
        const content = readText(row?.content)
        if (!content) return null
        const id = readText(row?.id) || String(index)
        return {
          key: `comment-${id}`,
          title: content,
          detail: "내가 남긴 댓글",
          href: "/myinfo/posts",
          timeLabel: formatRelativeKo(readText(row?.createdAt)),
          category: "COMMENT" as const,
          unread: false,
        }
      })
      .filter((row): row is ActivityAlertItem => Boolean(row))
      .slice(0, 5)
  } catch (error) {
    console.error("내 활동 댓글을 불러오지 못했습니다.", error)
    return []
  }
}

async function loadFavorites(): Promise<ActivityWatchItem[]> {
  try {
    const [vod, scraps] = await Promise.all([
      fetchMyLiveVodFavorites(0, 5).catch(() => null),
      fetchMyBoardScraps(0, 5).catch(() => null),
    ])
    const videos: ActivityWatchItem[] = (vod?.items ?? []).map((item) => ({
      key: `fav-${item.videoId}`,
      title: item.title,
      href: `/live-vod/watch/${encodeURIComponent(item.videoId)}`,
      image: item.thumbnailUrl,
      source: item.channelTitle || "실시간 VOD",
      timeLabel: formatRelativeKo(item.savedAt),
    }))
    const boards: ActivityWatchItem[] = (scraps?.items ?? []).map((item) => ({
      key: `scrap-${item.id}`,
      title: item.title,
      href: boardHref(item.boardType, item.id),
      image: null,
      source: boardLabel(item.boardType),
      timeLabel: item.timeAgo || formatRelativeKo(item.createdAt),
    }))
    return [...videos, ...boards].slice(0, 5)
  } catch (error) {
    console.error("내 활동 즐겨찾기를 불러오지 못했습니다.", error)
    return []
  }
}

function loadWatches(): ActivityWatchItem[] {
  return listLiveVodHistory()
    .slice(0, 5)
    .map((item) => ({
      key: item.videoId,
      title: item.title,
      href: `/live-vod/watch/${encodeURIComponent(item.videoId)}`,
      image: item.thumbnailUrl,
      source: item.channelTitle || "실시간 VOD",
      timeLabel: formatRelativeKo(item.watchedAt),
    }))
}

export async function loadMyActivity(): Promise<MyActivityData> {
  const [favorites, posts, alerts] = await Promise.all([
    loadFavorites(),
    loadPosts(),
    loadAlerts(),
  ])
  return {
    ...EMPTY,
    watches: loadWatches(),
    favorites,
    posts,
    alerts,
  }
}
