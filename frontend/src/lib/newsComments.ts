import { unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson, readUsableAccessToken } from "@/lib/finsightToken"

export type NewsComment = {
  id: number
  content: string
  createdAt: string | null
  status: string
  replies: NewsComment[]
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function parseComment(raw: unknown): NewsComment | null {
  const row = asRecord(raw)
  if (!row) return null
  const id = typeof row.id === "number" ? row.id : Number(row.id)
  if (!Number.isFinite(id)) return null
  const replies = Array.isArray(row.replies) ? row.replies : []
  return {
    id,
    content: typeof row.content === "string" ? row.content : "",
    createdAt: typeof row.createdAt === "string" ? row.createdAt : null,
    status: typeof row.status === "string" ? row.status : "ACTIVE",
    replies: replies.map(parseComment).filter((item): item is NewsComment => item !== null),
  }
}

async function readMessage(res: Response, fallback: string): Promise<string> {
  try {
    const payload = asRecord(await res.json())
    if (payload && typeof payload.message === "string" && payload.message) return payload.message
  } catch {
    return fallback
  }
  return fallback
}

export async function fetchNewsComments(newsId: number): Promise<NewsComment[]> {
  const res = await fetch(`/api/v1/comments/news/${newsId}?page=0&size=50`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  })
  if (!res.ok) {
    throw new Error(await readMessage(res, "댓글을 불러오지 못했습니다."))
  }
  const payload: unknown = await res.json().catch(() => null)
  const data = unwrapApiData<Record<string, unknown>>(payload) ?? asRecord(payload)
  const list = Array.isArray(data?.comments) ? data.comments : []
  return list.map(parseComment).filter((item): item is NewsComment => item !== null && item.status !== "DELETED")
}

export async function createNewsComment(newsId: number, content: string): Promise<void> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const res = await fetch("/api/v1/comments", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...authHeadersJson(),
    },
    body: JSON.stringify({
      content,
      commentType: "NEWS",
      targetId: newsId,
      parentId: null,
    }),
    cache: "no-store",
  })
  if (!res.ok) {
    throw new Error(await readMessage(res, "댓글 등록에 실패했습니다."))
  }
}
