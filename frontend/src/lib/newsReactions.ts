import { unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson, readUsableAccessToken } from "@/lib/finsightToken"

export type NewsReaction = {
  myReaction: "LIKE" | "DISLIKE" | null
  likeCount: number
  dislikeCount: number
}

const EMPTY: NewsReaction = { myReaction: null, likeCount: 0, dislikeCount: 0 }

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function countOf(value: unknown): number {
  const count = typeof value === "number" ? value : Number(value)
  return Number.isFinite(count) && count > 0 ? count : 0
}

function parseReaction(payload: unknown): NewsReaction {
  const data = unwrapApiData<Record<string, unknown>>(payload) ?? asRecord(payload)
  if (!data) return EMPTY
  const mine = data.myReaction === "LIKE" || data.myReaction === "DISLIKE" ? data.myReaction : null
  return {
    myReaction: mine,
    likeCount: countOf(data.likeCount),
    dislikeCount: countOf(data.dislikeCount),
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

export async function fetchNewsReaction(newsId: number): Promise<NewsReaction> {
  const res = await fetch(`/api/v1/news/${newsId}/reactions`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  })
  if (!res.ok) return EMPTY
  return parseReaction(await res.json().catch(() => null))
}

export async function toggleNewsReaction(
  newsId: number,
  reaction: "LIKE" | "DISLIKE",
): Promise<NewsReaction> {
  if (!readUsableAccessToken()) {
    throw new Error("로그인이 필요합니다.")
  }
  const res = await fetch(`/api/v1/news/${newsId}/reactions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...authHeadersJson(),
    },
    body: JSON.stringify({ reaction }),
    cache: "no-store",
  })
  if (!res.ok) {
    throw new Error(await readMessage(res, "반응 저장에 실패했습니다."))
  }
  return parseReaction(await res.json().catch(() => null))
}
