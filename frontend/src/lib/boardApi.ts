import { authHeadersJson } from "@/lib/finsightToken"
import { keywordsFromViewedPosts } from "@/lib/searchKeywords"

export type BoardTypeCode = "NOTICE" | "FREE" | "QNA" | "COMMUNITY" | "MEDIA"

export type BoardListItem = {
  id: number
  title: string
  authorEmail: string
  boardType: BoardTypeCode
  status: string
  viewCount: number
  likeCount: number
  dislikeCount: number
  commentCount: number
  hashtags: string[]
  timeAgo: string
  createdAt: string
  updatedAt: string
  highlighted?: boolean
}

export type BoardPagination = {
  content: BoardListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext?: boolean
  hasPrevious?: boolean
}

export type BoardDetail = {
  id: number
  title: string
  content: string
  renderedHtml: string
  plainTextPreview: string
  authorEmail: string
  boardType: BoardTypeCode
  status: string
  viewCount: number
  likeCount: number
  dislikeCount: number
  commentCount: number
  reportCount: number
  hashtags: string[]
  files: unknown[]
  createdAt: string
  updatedAt: string
  highlighted?: boolean
  navigation: {
    previous: {
      id: number
      title: string
      authorEmail: string
      createdAt: string
    } | null
    next: {
      id: number
      title: string
      authorEmail: string
      createdAt: string
    } | null
  } | null
}

export type ApiEnvelope<T> = {
  success?: boolean
  data?: T
  message?: string
}

export function unwrapApiData<T>(payload: unknown): T | null {
  if (!payload || typeof payload !== "object") return null
  const o = payload as ApiEnvelope<T>
  if (o.data === undefined || o.data === null) return null
  return o.data
}

export function formatBoardDate(iso: string): string {
  if (!iso) return ""
  const d = iso.slice(0, 10).replace(/-/g, ".")
  if (d.length >= 8) return d.slice(2)
  return d
}

export function formatAuthor(email: string): string {
  if (!email) return ""
  const at = email.indexOf("@")
  if (at <= 0) return email
  const local = email.slice(0, at)
  if (local.length <= 2) return `${local[0] ?? ""}*@${email.slice(at + 1)}`
  return `${local.slice(0, 2)}***@${email.slice(at + 1)}`
}

export type BoardReactionStatus = {
  liked: boolean
  disliked: boolean
  scrapped: boolean
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

export type PopularBoardCard = {
  id: number
  title: string
  boardType: BoardTypeCode
  timeAgo: string
}

const BOARD_TYPES: BoardTypeCode[] = ["NOTICE", "FREE", "QNA", "COMMUNITY", "MEDIA"]

function asBoardType(value: unknown): BoardTypeCode | null {
  const code = typeof value === "string" ? value.toUpperCase() : ""
  return BOARD_TYPES.find((item) => item === code) ?? null
}

export function boardDetailPath(boardType: string, id: number): string {
  const t = boardType.toUpperCase()
  if (t === "NOTICE") return `/community/notice/${id}`
  if (t === "FREE") return `/community/free/${id}`
  return `/community/qna/${id}`
}

export function boardListPath(boardType: string): string {
  const t = boardType.toUpperCase()
  if (t === "NOTICE") return "/community/notice"
  if (t === "FREE") return "/community/free"
  if (t === "QNA") return "/community/qna"
  return "/community/notice"
}

type ScoredPopularCard = PopularBoardCard & { viewCount: number; hashtags: string[] }

function parsePopularCards(data: unknown): ScoredPopularCard[] {
  if (!Array.isArray(data)) return []
  const cards: ScoredPopularCard[] = []
  for (const row of data) {
    const record = asRecord(row)
    const id = Number(record?.id)
    const boardType = asBoardType(record?.boardType)
    if (!record || !Number.isFinite(id) || id <= 0 || !boardType) continue
    cards.push({
      id,
      title: typeof record.title === "string" ? record.title : "",
      boardType,
      timeAgo: typeof record.timeAgo === "string" ? record.timeAgo : "",
      viewCount: Number(record.viewCount) || 0,
      hashtags: Array.isArray(record.hashtags)
        ? record.hashtags.filter((item): item is string => typeof item === "string")
        : [],
    })
  }
  return cards
}

function toPopularCard(card: ScoredPopularCard): PopularBoardCard {
  return {
    id: card.id,
    title: card.title,
    boardType: card.boardType,
    timeAgo: card.timeAgo,
  }
}

function rankPopularCards(rows: ScoredPopularCard[], limit: number): PopularBoardCard[] {
  return [...rows]
    .sort((a, b) => b.viewCount - a.viewCount || b.id - a.id)
    .slice(0, limit)
    .map(toPopularCard)
}

async function fetchBoardTypeCards(boardType: BoardTypeCode): Promise<ScoredPopularCard[]> {
  try {
    const res = await fetch(`/api/v1/boards?boardType=${boardType}&page=0&size=50`, {
      cache: "no-store",
    })
    if (!res.ok) return []
    const payload: unknown = await res.json().catch(() => null)
    const data = asRecord(unwrapApiData(payload))
    return parsePopularCards(data?.content)
  } catch {
    return []
  }
}

async function fetchPopularEndpoint(limit: number): Promise<ScoredPopularCard[]> {
  try {
    const res = await fetch(`/api/v1/boards/popular?limit=${limit}`, { cache: "no-store" })
    if (!res.ok) return []
    const payload: unknown = await res.json().catch(() => null)
    return parsePopularCards(unwrapApiData(payload))
  } catch {
    return []
  }
}

export function watchPopularBoardCards(
  limit: number,
  onCards: (cards: PopularBoardCard[]) => void,
  onSettled: () => void,
  onKeywords?: (keywords: string[]) => void,
): () => void {
  const size = Math.min(100, Math.max(1, limit))
  let cancelled = false
  let pending = BOARD_TYPES.length + 1
  const scored = new Map<BoardTypeCode, ScoredPopularCard[]>()
  const finishOne = () => {
    pending -= 1
    if (!cancelled && pending <= 0) onSettled()
  }
  const emitKeywords = (rows: ScoredPopularCard[]) => {
    if (!onKeywords || cancelled) return
    const keywords = keywordsFromViewedPosts(rows, 10)
    if (keywords.length > 0) onKeywords(keywords)
  }
  const publish = () => {
    if (cancelled) return
    const rows = [...scored.values()].flat()
    const cards = rankPopularCards(rows, size)
    if (cards.length > 0) onCards(cards)
    emitKeywords(rows)
  }
  void fetchPopularEndpoint(size).then((rows) => {
    if (!cancelled && rows.length > 0) {
      onCards(rankPopularCards(rows, size))
      emitKeywords(rows)
    }
    finishOne()
  })
  for (const boardType of BOARD_TYPES) {
    void fetchBoardTypeCards(boardType).then((rows) => {
      scored.set(boardType, rows)
      publish()
      finishOne()
    })
  }
  return () => {
    cancelled = true
  }
}

export async function fetchBoardReactionStatus(
  boardId: number,
): Promise<BoardReactionStatus | null> {
  try {
    const res = await fetch(`/api/v1/boards/${boardId}/reaction-status`, {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (!res.ok) return null
    const payload: unknown = await res.json().catch(() => null)
    const data = asRecord(unwrapApiData(payload) ?? payload)
    if (!data) return null
    return {
      liked: data.liked === true,
      disliked: data.disliked === true,
      scrapped: data.scrapped === true,
    }
  } catch {
    return null
  }
}

async function readReactionCounts(
  res: Response,
): Promise<{ likeCount: number; dislikeCount: number } | null> {
  const payload: unknown = await res.json().catch(() => null)
  const data = asRecord(unwrapApiData(payload) ?? payload)
  if (!data) return null
  return {
    likeCount: Number(data.likeCount) || 0,
    dislikeCount: Number(data.dislikeCount) || 0,
  }
}

export async function likeBoard(
  boardId: number,
): Promise<
  | { ok: true; likeCount: number; dislikeCount: number }
  | { ok: false; message: string }
> {
  try {
    const res = await fetch(`/api/v1/boards/${boardId}/like`, {
      method: "POST",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403) {
      return { ok: false, message: "로그인이 필요합니다." }
    }
    if (!res.ok) {
      const payload: unknown = await res.json().catch(() => null)
      const root = asRecord(payload)
      const message =
        typeof root?.message === "string" && root.message
          ? root.message
          : "좋아요 처리에 실패했습니다."
      return { ok: false, message }
    }
    const counts = await readReactionCounts(res)
    if (!counts) return { ok: false, message: "좋아요 응답을 해석하지 못했습니다." }
    return { ok: true, ...counts }
  } catch {
    return { ok: false, message: "좋아요 처리에 실패했습니다." }
  }
}

export async function dislikeBoard(
  boardId: number,
): Promise<
  | { ok: true; likeCount: number; dislikeCount: number }
  | { ok: false; message: string }
> {
  try {
    const res = await fetch(`/api/v1/boards/${boardId}/dislike`, {
      method: "POST",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403) {
      return { ok: false, message: "로그인이 필요합니다." }
    }
    if (!res.ok) {
      const payload: unknown = await res.json().catch(() => null)
      const root = asRecord(payload)
      const message =
        typeof root?.message === "string" && root.message
          ? root.message
          : "싫어요 처리에 실패했습니다."
      return { ok: false, message }
    }
    const counts = await readReactionCounts(res)
    if (!counts) return { ok: false, message: "싫어요 응답을 해석하지 못했습니다." }
    return { ok: true, ...counts }
  } catch {
    return { ok: false, message: "싫어요 처리에 실패했습니다." }
  }
}

export async function scrapBoard(
  boardId: number,
): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    const res = await fetch(`/api/v1/boards/${boardId}/scrap`, {
      method: "POST",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403) {
      return { ok: false, message: "로그인이 필요합니다." }
    }
    if (!res.ok) {
      const payload: unknown = await res.json().catch(() => null)
      const root = asRecord(payload)
      const message =
        typeof root?.message === "string" && root.message
          ? root.message
          : "즐겨찾기 저장에 실패했습니다."
      return { ok: false, message }
    }
    return { ok: true }
  } catch {
    return { ok: false, message: "즐겨찾기 저장에 실패했습니다." }
  }
}

export async function unscrapBoard(
  boardId: number,
): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    const res = await fetch(`/api/v1/boards/${boardId}/scrap`, {
      method: "DELETE",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403) {
      return { ok: false, message: "로그인이 필요합니다." }
    }
    if (!res.ok) {
      const payload: unknown = await res.json().catch(() => null)
      const root = asRecord(payload)
      const message =
        typeof root?.message === "string" && root.message
          ? root.message
          : "즐겨찾기 해제에 실패했습니다."
      return { ok: false, message }
    }
    return { ok: true }
  } catch {
    return { ok: false, message: "즐겨찾기 해제에 실패했습니다." }
  }
}
