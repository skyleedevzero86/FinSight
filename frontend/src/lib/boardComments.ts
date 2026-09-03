import { authHeadersJson, readUsableAccessToken } from "@/lib/finsightToken"
import { unwrapApiData } from "@/lib/boardApi"

export type BoardComment = {
  id: number
  content: string
  authorEmail: string
  parentId: number | null
  likeCount: number
  dislikeCount: number
  createdAt: string | null
  replies: BoardComment[]
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function parseComment(raw: unknown): BoardComment | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "number" ? o.id : Number(o.id)
  if (!Number.isFinite(id)) return null
  const parentRaw = o.parentId
  const parentId =
    parentRaw == null || parentRaw === ""
      ? null
      : typeof parentRaw === "number"
        ? parentRaw
        : Number(parentRaw)
  const repliesRaw = Array.isArray(o.replies) ? o.replies : []
  return {
    id,
    content: typeof o.content === "string" ? o.content : "",
    authorEmail: typeof o.authorEmail === "string" ? o.authorEmail : "",
    parentId: parentId != null && Number.isFinite(parentId) ? parentId : null,
    likeCount: Number(o.likeCount) || 0,
    dislikeCount: Number(o.dislikeCount) || 0,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    replies: repliesRaw.map(parseComment).filter((v): v is BoardComment => v != null),
  }
}

async function readMessage(res: Response, fallback: string): Promise<string> {
  try {
    const payload = await res.json()
    const root = asRecord(payload)
    if (root && typeof root.message === "string" && root.message) return root.message
  } catch {}
  return fallback
}

export async function fetchBoardComments(
  boardId: number,
  page = 0,
  size = 20,
): Promise<BoardComment[]> {
  const qs = new URLSearchParams({
    page: String(Math.max(0, page)),
    size: String(Math.min(100, Math.max(1, size))),
  })
  const res = await fetch(`/api/v1/comments/board/${boardId}?${qs.toString()}`, {
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  if (!res.ok) {
    throw new Error(await readMessage(res, "댓글을 불러오지 못했습니다."))
  }
  const payload: unknown = await res.json().catch(() => null)
  const data = unwrapApiData<Record<string, unknown>>(payload) ?? asRecord(payload)
  const list = Array.isArray(data?.comments)
    ? data.comments
    : Array.isArray(data)
      ? data
      : []
  return list.map(parseComment).filter((v): v is BoardComment => v != null)
}

export async function createBoardComment(
  boardId: number,
  content: string,
  parentId?: number | null,
): Promise<BoardComment> {
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
      commentType: "BOARD",
      targetId: boardId,
      parentId: parentId ?? null,
    }),
    cache: "no-store",
  })
  if (!res.ok) {
    throw new Error(await readMessage(res, "댓글 등록에 실패했습니다."))
  }
  const payload: unknown = await res.json().catch(() => null)
  const data = unwrapApiData<unknown>(payload) ?? payload
  const parsed = parseComment(data)
  if (!parsed) throw new Error("댓글 응답을 해석하지 못했습니다.")
  return parsed
}

export async function likeBoardComment(commentId: number): Promise<void> {
  if (!readUsableAccessToken()) throw new Error("로그인이 필요합니다.")
  const res = await fetch(`/api/v1/comments/${commentId}/like`, {
    method: "POST",
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  if (!res.ok) throw new Error(await readMessage(res, "좋아요 처리에 실패했습니다."))
}

export async function dislikeBoardComment(commentId: number): Promise<void> {
  if (!readUsableAccessToken()) throw new Error("로그인이 필요합니다.")
  const res = await fetch(`/api/v1/comments/${commentId}/dislike`, {
    method: "POST",
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  if (!res.ok) throw new Error(await readMessage(res, "싫어요 처리에 실패했습니다."))
}

export async function deleteBoardComment(commentId: number): Promise<void> {
  if (!readUsableAccessToken()) throw new Error("로그인이 필요합니다.")
  const res = await fetch(`/api/v1/comments/${commentId}`, {
    method: "DELETE",
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  if (!res.ok) throw new Error(await readMessage(res, "댓글 삭제에 실패했습니다."))
}
