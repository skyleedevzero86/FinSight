import { authHeadersJson } from "@/lib/finsightToken"

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

export type PopupItem = {
  id: string
  domainId: string | null
  title: string
  fileUrl: string | null
  linkTarget: string | null
  imgPath: string | null
  fileName: string | null
  verticalPos: number | null
  widthPos: number | null
  verticalSize: number | null
  widthSize: number | null
  noticeBegin: string | null
  noticeEnd: string | null
  stopTodayHide: string
  noticeActive: string
  createdAt: string | null
  updatedAt: string | null
}

export type PopupItemInput = {
  domainId?: string
  title: string
  fileUrl?: string
  linkTarget?: string
  imgPath?: string
  fileName?: string
  verticalPos?: number | null
  widthPos?: number | null
  verticalSize?: number | null
  widthSize?: number | null
  noticeBegin?: string
  noticeEnd?: string
  stopTodayHide?: string
  noticeActive?: string
}

export type PopupPage = {
  content: PopupItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function parseItem(raw: unknown): PopupItem | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "string" ? o.id : ""
  const title = typeof o.title === "string" ? o.title : ""
  if (!id || !title) return null
  return {
    id,
    domainId: typeof o.domainId === "string" ? o.domainId : null,
    title,
    fileUrl: typeof o.fileUrl === "string" ? o.fileUrl : null,
    linkTarget: typeof o.linkTarget === "string" ? o.linkTarget : null,
    imgPath: typeof o.imgPath === "string" ? o.imgPath : null,
    fileName: typeof o.fileName === "string" ? o.fileName : null,
    verticalPos: typeof o.verticalPos === "number" ? o.verticalPos : null,
    widthPos: typeof o.widthPos === "number" ? o.widthPos : null,
    verticalSize: typeof o.verticalSize === "number" ? o.verticalSize : null,
    widthSize: typeof o.widthSize === "number" ? o.widthSize : null,
    noticeBegin: typeof o.noticeBegin === "string" ? o.noticeBegin : null,
    noticeEnd: typeof o.noticeEnd === "string" ? o.noticeEnd : null,
    stopTodayHide: typeof o.stopTodayHide === "string" ? o.stopTodayHide : "N",
    noticeActive: typeof o.noticeActive === "string" ? o.noticeActive : "Y",
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    updatedAt: typeof o.updatedAt === "string" ? o.updatedAt : null,
  }
}

function parsePage(raw: unknown): PopupPage | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const contentRaw = Array.isArray(data.content) ? data.content : []
  const content = contentRaw.map(parseItem).filter((x): x is PopupItem => x != null)
  const size = Number(data.size) || 20
  const totalElements = Number(data.totalElements) || content.length
  const totalPages =
    Number(data.totalPages) || Math.max(1, Math.ceil(totalElements / Math.max(1, size)))
  return {
    content,
    page: Number(data.page) || 0,
    size,
    totalElements,
    totalPages,
  }
}

function parseOne(raw: unknown): PopupItem | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  return parseItem(data)
}

function normalizeDateKey(value: string | null | undefined): string | null {
  if (!value) return null
  const digits = value.replace(/\D/g, "")
  if (digits.length >= 8) return digits.slice(0, 8)
  return null
}

export function isPopupInSchedule(item: PopupItem, now = new Date()): boolean {
  const today = [
    String(now.getFullYear()),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("")
  const begin = normalizeDateKey(item.noticeBegin)
  const end = normalizeDateKey(item.noticeEnd)
  if (begin && today < begin) return false
  if (end && today > end) return false
  return true
}

const HIDE_PREFIX = "finsight_popup_hide_"

export function isPopupHiddenToday(id: string): boolean {
  if (typeof window === "undefined") return false
  try {
    const raw = localStorage.getItem(`${HIDE_PREFIX}${id}`)
    if (!raw) return false
    const today = new Date().toISOString().slice(0, 10)
    return raw === today
  } catch {
    return false
  }
}

export function hidePopupToday(id: string): void {
  if (typeof window === "undefined") return
  try {
    const today = new Date().toISOString().slice(0, 10)
    localStorage.setItem(`${HIDE_PREFIX}${id}`, today)
  } catch {
  }
}

export async function fetchPublicPopupItems(options?: {
  domainId?: string
  size?: number
}): Promise<{ ok: true; data: PopupItem[] } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("activeOnly", "true")
  params.set("page", "0")
  params.set("size", String(options?.size ?? 20))
  if (options?.domainId) params.set("domainId", options.domainId)
  try {
    const res = await fetch(`/api/v1/popup/items?${params.toString()}`, { cache: "no-store" })
    const payload = await readJson(res)
    if (!res.ok) return { ok: false, message: readMessage(payload, "팝업을 불러오지 못했습니다.") }
    const page = parsePage(payload)
    if (!page) return { ok: false, message: "팝업 형식이 올바르지 않습니다." }
    return { ok: true, data: page.content }
  } catch {
    return { ok: false, message: "팝업을 불러오지 못했습니다." }
  }
}

export async function fetchAdminPopupItems(options?: {
  page?: number
  size?: number
  domainId?: string
  activeOnly?: boolean
}): Promise<{ ok: true; data: PopupPage } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", String(options?.page ?? 0))
  params.set("size", String(options?.size ?? 20))
  params.set("activeOnly", String(options?.activeOnly ?? false))
  if (options?.domainId) params.set("domainId", options.domainId)
  const res = await fetch(`/api/v1/popup/items?${params.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "팝업 목록을 불러오지 못했습니다.") }
  const page = parsePage(payload)
  if (!page) return { ok: false, message: "팝업 목록 형식이 올바르지 않습니다." }
  return { ok: true, data: page }
}

export async function createPopupItem(
  input: PopupItemInput,
): Promise<{ ok: true; data: PopupItem } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/popup/items", {
    method: "POST",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "팝업을 등록하지 못했습니다.") }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "등록 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function updatePopupItem(
  id: string,
  input: PopupItemInput,
): Promise<{ ok: true; data: PopupItem } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/popup/items/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "팝업을 수정하지 못했습니다.") }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "수정 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function deletePopupItem(
  id: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/popup/items/${encodeURIComponent(id)}`, {
    method: "DELETE",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "팝업을 삭제하지 못했습니다.") }
  return { ok: true }
}
