import { authHeadersJson } from "@/lib/finsightToken"

export type LiveVodEngagement = {
  videoId: string
  favoriteCount: number
  ratingCount: number
  avgRating: number
  favorited: boolean | null
  myStars: number | null
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

export async function fetchLiveVodEngagement(videoId: string): Promise<LiveVodEngagement> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/engagement`, {
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    videoId: typeof data.videoId === "string" ? data.videoId : videoId,
    favoriteCount: Number(data.favoriteCount) || 0,
    ratingCount: Number(data.ratingCount) || 0,
    avgRating: Number(data.avgRating) || 0,
    favorited: typeof data.favorited === "boolean" ? data.favorited : null,
    myStars: typeof data.myStars === "number" ? data.myStars : null,
  }
}

export async function toggleLiveVodFavoriteApi(
  videoId: string,
): Promise<{ favorited: boolean; favoriteCount: number }> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/favorite`, {
    method: "POST",
    headers: { Accept: "application/json", ...authHeadersJson() },
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    favorited: Boolean(data.favorited),
    favoriteCount: Number(data.favoriteCount) || 0,
  }
}

export async function rateLiveVodApi(
  videoId: string,
  stars: number,
): Promise<{ myStars: number; ratingCount: number; avgRating: number }> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/rating`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", Accept: "application/json", ...authHeadersJson() },
    body: JSON.stringify({ stars }),
  })
  const data = await readApiData<Record<string, unknown>>(res)
  return {
    myStars: Number(data.myStars) || stars,
    ratingCount: Number(data.ratingCount) || 0,
    avgRating: Number(data.avgRating) || 0,
  }
}

function parseComment(raw: unknown): LiveVodComment | null {
  const o = asRecord(raw)
  if (!o || typeof o.id !== "number") return null
  const repliesRaw = Array.isArray(o.replies) ? o.replies : []
  return {
    id: o.id,
    videoId: typeof o.videoId === "string" ? o.videoId : "",
    userEmail: typeof o.userEmail === "string" ? o.userEmail : "",
    authorNickname: typeof o.authorNickname === "string" ? o.authorNickname : null,
    content: typeof o.content === "string" ? o.content : "",
    parentId: typeof o.parentId === "number" ? o.parentId : null,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    replies: repliesRaw.map(parseComment).filter((v): v is LiveVodComment => v != null),
  }
}

export async function fetchLiveVodComments(videoId: string): Promise<LiveVodComment[]> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  })
  const data = await readApiData<unknown>(res)
  if (!Array.isArray(data)) return []
  return data.map(parseComment).filter((v): v is LiveVodComment => v != null)
}

export async function createLiveVodComment(
  videoId: string,
  content: string,
  parentId?: number | null,
): Promise<LiveVodComment> {
  const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json", ...authHeadersJson() },
    body: JSON.stringify({ content, parentId: parentId ?? null }),
  })
  const data = await readApiData<unknown>(res)
  const parsed = parseComment(data)
  if (!parsed) throw new Error("댓글 응답을 해석하지 못했습니다.")
  return parsed
}
