import { authHeadersJson, readAccessToken } from "@/lib/finsightToken"

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readMessage(payload: unknown, fallback: string): string {
  const root = asRecord(payload)
  if (!root) return fallback
  if (typeof root.message === "string" && root.message) return root.message
  return fallback
}

async function readJson(res: Response): Promise<unknown> {
  try {
    return await res.json()
  } catch {
    return null
  }
}

export type InboxCategory =
  | "YOUTUBE"
  | "NEWS"
  | "COMMENT"
  | "QNA"
  | "WATCHLIST"
  | "ADMIN"

export const INBOX_CATEGORIES: { value: InboxCategory; label: string }[] = [
  { value: "YOUTUBE", label: "유튜브" },
  { value: "NEWS", label: "뉴스" },
  { value: "COMMENT", label: "댓글" },
  { value: "QNA", label: "QnA" },
  { value: "WATCHLIST", label: "관심종목" },
  { value: "ADMIN", label: "관리" },
]

export type InboxItem = {
  id: number
  category: InboxCategory
  actorUserId: number | null
  actorName: string | null
  actorAvatarUrl: string | null
  title: string
  body: string | null
  linkUrl: string | null
  refType: string | null
  refId: number | null
  read: boolean
  createdAt: string | null
}

export type InboxPage = {
  content: InboxItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export type InboxSettings = {
  youtubeEnabled: boolean
  newsEnabled: boolean
  commentEnabled: boolean
  qnaEnabled: boolean
}

function parseItem(raw: unknown): InboxItem | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "number" ? o.id : Number(o.id)
  const title = typeof o.title === "string" ? o.title : ""
  if (!Number.isFinite(id) || !title) return null
  const category = typeof o.category === "string" ? (o.category as InboxCategory) : "NEWS"
  return {
    id,
    category,
    actorUserId: typeof o.actorUserId === "number" ? o.actorUserId : null,
    actorName: typeof o.actorName === "string" ? o.actorName : null,
    actorAvatarUrl: typeof o.actorAvatarUrl === "string" ? o.actorAvatarUrl : null,
    title,
    body: typeof o.body === "string" ? o.body : null,
    linkUrl: typeof o.linkUrl === "string" ? o.linkUrl : null,
    refType: typeof o.refType === "string" ? o.refType : null,
    refId: typeof o.refId === "number" ? o.refId : null,
    read: Boolean(o.read),
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
  }
}

function parsePage(payload: unknown): InboxPage {
  const root = asRecord(payload)
  const data = asRecord(root?.data) ?? root
  const contentRaw = Array.isArray(data?.content) ? data.content : []
  const content = contentRaw.map(parseItem).filter((x): x is InboxItem => Boolean(x))
  const page = typeof data?.page === "number" ? data.page : 0
  const size = typeof data?.size === "number" ? data.size : 20
  const totalElements = typeof data?.totalElements === "number" ? data.totalElements : content.length
  const totalPages = typeof data?.totalPages === "number" ? data.totalPages : 1
  const hasNext =
    typeof data?.hasNext === "boolean"
      ? data.hasNext
      : page + 1 < totalPages
  return { content, page, size, totalElements, totalPages, hasNext }
}

function parseSettings(payload: unknown): InboxSettings {
  const root = asRecord(payload)
  const data = asRecord(root?.data) ?? root
  return {
    youtubeEnabled: data?.youtubeEnabled !== false,
    newsEnabled: data?.newsEnabled !== false,
    commentEnabled: data?.commentEnabled !== false,
    qnaEnabled: data?.qnaEnabled !== false,
  }
}

export async function fetchInboxUnreadCount(): Promise<number | null> {
  if (!readAccessToken()) return 0
  if (typeof navigator !== "undefined" && navigator.onLine === false) {
    return null
  }
  try {
    const res = await fetch("/api/v1/inbox/unread-count", {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403 || res.status === 404) return 0
    if (!res.ok) return null
    const payload = await readJson(res)
    const root = asRecord(payload)
    const data = asRecord(root?.data)
    const count = data?.unreadCount
    return typeof count === "number" ? count : 0
  } catch {
    return null
  }
}

export async function fetchInboxPage(params: {
  page?: number
  size?: number
  unreadOnly?: boolean
}): Promise<{ ok: true; page: InboxPage } | { ok: false; message: string }> {
  const qs = new URLSearchParams()
  qs.set("page", String(params.page ?? 0))
  qs.set("size", String(params.size ?? 20))
  if (params.unreadOnly) qs.set("unreadOnly", "true")
  const res = await fetch(`/api/v1/inbox?${qs.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "알림 목록을 불러오지 못했습니다.") }
  }
  return { ok: true, page: parsePage(payload) }
}

export async function markInboxRead(id: number): Promise<boolean> {
  const res = await fetch(`/api/v1/inbox/${id}/read`, {
    method: "POST",
    headers: authHeadersJson(),
  })
  return res.ok
}

export async function markAllInboxRead(): Promise<boolean> {
  const res = await fetch("/api/v1/inbox/read-all", {
    method: "POST",
    headers: authHeadersJson(),
  })
  return res.ok
}

export async function deleteInboxItem(id: number): Promise<boolean> {
  const res = await fetch(`/api/v1/inbox/${id}`, {
    method: "DELETE",
    headers: authHeadersJson(),
  })
  return res.ok
}

export async function deleteAllInbox(): Promise<boolean> {
  const res = await fetch("/api/v1/inbox", {
    method: "DELETE",
    headers: authHeadersJson(),
  })
  return res.ok
}

export async function fetchInboxSettings(): Promise<InboxSettings | null> {
  const res = await fetch("/api/v1/inbox/settings", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return null
  return parseSettings(payload)
}

export async function updateInboxSettings(
  settings: InboxSettings
): Promise<{ ok: true; settings: InboxSettings } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/inbox/settings", {
    method: "PUT",
    headers: authHeadersJson(),
    body: JSON.stringify(settings),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "알림 설정을 저장하지 못했습니다.") }
  }
  return { ok: true, settings: parseSettings(payload) }
}

export async function fetchAdminInboxPage(params: {
  page?: number
  size?: number
}): Promise<{ ok: true; page: InboxPage } | { ok: false; message: string }> {
  const qs = new URLSearchParams()
  qs.set("page", String(params.page ?? 0))
  qs.set("size", String(params.size ?? 20))
  const res = await fetch(`/api/v1/admin/inbox?${qs.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "관리자 알림 목록을 불러오지 못했습니다.") }
  }
  return { ok: true, page: parsePage(payload) }
}

export async function broadcastInbox(input: {
  category: InboxCategory
  title: string
  body?: string
  linkUrl?: string
  actorName?: string
  allUsers?: boolean
  adminsOnly?: boolean
}): Promise<{ ok: true; createdCount: number } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/admin/inbox/broadcast", {
    method: "POST",
    headers: authHeadersJson(),
    body: JSON.stringify({
      category: input.category,
      title: input.title,
      body: input.body || null,
      linkUrl: input.linkUrl || null,
      actorName: input.actorName || "FinSight",
      allUsers: input.allUsers ?? true,
      adminsOnly: input.adminsOnly ?? false,
    }),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "알림 등록에 실패했습니다.") }
  }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  const createdCount = typeof data?.createdCount === "number" ? data.createdCount : 0
  return { ok: true, createdCount }
}

export function formatInboxTime(value: string | null): string {
  if (!value) return ""
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return "방금 전"
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay}일 전`
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`
}
