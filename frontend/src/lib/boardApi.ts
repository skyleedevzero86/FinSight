import { authHeadersJson } from "@/lib/finsightToken"

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
