import { authHeadersJson, readUsableAccessToken } from "@/lib/finsightToken"

export const ROOT_COMMENT_PAGE_SIZE = 15
export const REPLY_PAGE_SIZE = 5

export type LiveVodEngagement = {
  videoId: string
  favoriteCount: number
  commentCount: number
  favorited: boolean | null
  likeCount: number
  dislikeCount: number
  myReaction: "LIKE" | "DISLIKE" | null
}

export type LiveVodComment = {
  id: number
  videoId: string
  userEmail: string
  authorNickname: string | null
  content: string
  parentId: number | null
  createdAt: string | null
  replies: LiveVodComment[]
  replyCount: number
  replyPage: number
  replyTotalPages: number
}

export type LiveVodCommentPage = {
  items: LiveVodComment[]
  page: number
  size: number
  totalElements: number
  totalComments: number
  hasNext: boolean
}

export type LiveVodReplyPage = {
  parentId: number
  items: LiveVodComment[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrev: boolean
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

async function readApiData<T>(res: Response): Promise<T> {
  const json = (await res.json()) as { success?: boolean; data?: T; message?: string }
  if (!res.ok || json.success === false) {
    throw new Error(json.message || `요청 실패 (${res.status})`)
  }
  return json.data as T
}

function parseReaction(value: unknown): "LIKE" | "DISLIKE" | null {
  if (value === "LIKE" || value === "DISLIKE") return value
  return null
}

export async function fetchLiveVodEngagement(videoId: string): Promise<LiveVodEngagement> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/engagement`, {
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    videoId: typeof data.videoId === "string" ? data.videoId : videoId,
    favoriteCount: Number(data.favoriteCount) || 0,
    commentCount: Number(data.commentCount) || 0,
    favorited: typeof data.favorited === "boolean" ? data.favorited : null,
    likeCount: Number(data.likeCount) || 0,
    dislikeCount: Number(data.dislikeCount) || 0,
    myReaction: parseReaction(data.myReaction),
  }
}

export async function toggleLiveVodFavoriteApi(
  videoId: string,
): Promise<{ favorited: boolean; favoriteCount: number }> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/favorite`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...authHeadersJson(),
    },
    body: "{}",
    cache: "no-store",
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    favorited: Boolean(data.favorited),
    favoriteCount: Number(data.favoriteCount) || 0,
  }
}

export async function toggleLiveVodReactionApi(
  videoId: string,
  reaction: "LIKE" | "DISLIKE",
): Promise<{ myReaction: "LIKE" | "DISLIKE" | null; likeCount: number; dislikeCount: number }> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/reaction`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...authHeadersJson(),
    },
    body: JSON.stringify({ reaction }),
    cache: "no-store",
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    myReaction: parseReaction(data.myReaction),
    likeCount: Number(data.likeCount) || 0,
    dislikeCount: Number(data.dislikeCount) || 0,
  }
}

function parseComment(raw: unknown): LiveVodComment | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "number" ? o.id : Number(o.id)
  if (!Number.isFinite(id)) return null
  const repliesRaw = Array.isArray(o.replies) ? o.replies : []
  const parentRaw = o.parentId
  const parentId =
    typeof parentRaw === "number"
      ? parentRaw
      : parentRaw == null
        ? null
        : Number(parentRaw)
  return {
    id,
    videoId: typeof o.videoId === "string" ? o.videoId : "",
    userEmail: typeof o.userEmail === "string" ? o.userEmail : "",
    authorNickname: typeof o.authorNickname === "string" ? o.authorNickname : null,
    content: typeof o.content === "string" ? o.content : "",
    parentId: parentId != null && Number.isFinite(parentId) ? parentId : null,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    replies: repliesRaw.map(parseComment).filter((v): v is LiveVodComment => v != null),
    replyCount: Number(o.replyCount) || 0,
    replyPage: Number(o.replyPage) || 0,
    replyTotalPages: Number(o.replyTotalPages) || 0,
  }
}

export async function fetchLiveVodComments(
  videoId: string,
  page = 0,
  size = ROOT_COMMENT_PAGE_SIZE,
): Promise<LiveVodCommentPage> {
  const qs = new URLSearchParams({
    page: String(Math.max(page, 0)),
    size: String(size),
  })
  const res = await fetch(
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments?${qs.toString()}`,
    {
      headers: { Accept: "application/json" },
      cache: "no-store",
    },
  )
  const data = await readApiData<Record<string, unknown>>(res)
  const itemsRaw = Array.isArray(data.items) ? data.items : []
  return {
    items: itemsRaw.map(parseComment).filter((v): v is LiveVodComment => v != null),
    page: Number(data.page) || 0,
    size: Number(data.size) || size,
    totalElements: Number(data.totalElements) || 0,
    totalComments: Number(data.totalComments) || 0,
    hasNext: Boolean(data.hasNext),
  }
}

export async function fetchLiveVodReplies(
  videoId: string,
  parentId: number,
  page = 0,
  size = REPLY_PAGE_SIZE,
): Promise<LiveVodReplyPage> {
  const qs = new URLSearchParams({
    page: String(Math.max(page, 0)),
    size: String(size),
  })
  const res = await fetch(
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments/${parentId}/replies?${qs.toString()}`,
    {
      headers: { Accept: "application/json" },
      cache: "no-store",
    },
  )
  const data = await readApiData<Record<string, unknown>>(res)
  const itemsRaw = Array.isArray(data.items) ? data.items : []
  return {
    parentId: Number(data.parentId) || parentId,
    items: itemsRaw.map(parseComment).filter((v): v is LiveVodComment => v != null),
    page: Number(data.page) || 0,
    size: Number(data.size) || size,
    totalElements: Number(data.totalElements) || 0,
    totalPages: Number(data.totalPages) || 0,
    hasNext: Boolean(data.hasNext),
    hasPrev: Boolean(data.hasPrev),
  }
}

export async function createLiveVodComment(
  videoId: string,
  content: string,
  parentId?: number | null,
): Promise<LiveVodComment> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...authHeadersJson(),
    },
    body: JSON.stringify({ content, parentId: parentId ?? null }),
    cache: "no-store",
  })
  const data = await readApiData<unknown>(res)
  const parsed = parseComment(data)
  if (!parsed) throw new Error("댓글 응답을 해석하지 못했습니다.")
  return parsed
}
