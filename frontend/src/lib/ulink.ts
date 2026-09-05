import { authHeadersJson } from "@/lib/finsightToken"
import { prepareImageForUpload, uploadEditorAsset } from "@/lib/editorUpload"

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

export const ULINK_SECTIONS = [
  { value: "FOOTER_TEXT", label: "텍스트" },
  { value: "FOOTER_IMAGE", label: "이미지" },
] as const

export type UlinkSectionCode = (typeof ULINK_SECTIONS)[number]["value"]

export type UlinkItem = {
  id: string
  domainId: string | null
  sectionCode: string | null
  linkGroup: string | null
  linkName: string
  linkUrl: string
  linkTarget: string | null
  description: string | null
  imgPath: string | null
  sortOrder: number
  openYn: string
  createdAt: string | null
  updatedAt: string | null
}

export type UlinkItemInput = {
  domainId?: string
  sectionCode?: string
  linkGroup?: string
  linkName: string
  linkUrl: string
  linkTarget?: string
  description?: string
  imgPath?: string
  sortOrder?: number
  openYn?: string
}

export type UlinkPage = {
  content: UlinkItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function parseItem(raw: unknown): UlinkItem | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "string" ? o.id : ""
  const linkName = typeof o.linkName === "string" ? o.linkName : ""
  const linkUrl = typeof o.linkUrl === "string" ? o.linkUrl : ""
  if (!id || !linkName || !linkUrl) return null
  const sortOrderRaw = o.sortOrder
  const sortOrder =
    typeof sortOrderRaw === "number" && Number.isFinite(sortOrderRaw)
      ? sortOrderRaw
      : typeof sortOrderRaw === "string" && sortOrderRaw.trim()
        ? Number(sortOrderRaw)
        : 0
  return {
    id,
    domainId: typeof o.domainId === "string" ? o.domainId : null,
    sectionCode: typeof o.sectionCode === "string" ? o.sectionCode : null,
    linkGroup: typeof o.linkGroup === "string" ? o.linkGroup : null,
    linkName,
    linkUrl,
    linkTarget: typeof o.linkTarget === "string" ? o.linkTarget : null,
    description: typeof o.description === "string" ? o.description : null,
    imgPath: typeof o.imgPath === "string" ? o.imgPath : null,
    sortOrder: Number.isFinite(sortOrder) ? sortOrder : 0,
    openYn: typeof o.openYn === "string" ? o.openYn : "Y",
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    updatedAt: typeof o.updatedAt === "string" ? o.updatedAt : null,
  }
}

function parsePage(raw: unknown): UlinkPage | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const contentRaw = Array.isArray(data.content) ? data.content : []
  const content = contentRaw.map(parseItem).filter((x): x is UlinkItem => x != null)
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

function parseOne(raw: unknown): UlinkItem | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  return parseItem(data)
}

export function isUlinkImageType(sectionCode: string | null | undefined): boolean {
  return sectionCode === "FOOTER_IMAGE"
}

export function isUlinkTextType(sectionCode: string | null | undefined): boolean {
  return (
    sectionCode === "FOOTER_TEXT" ||
    sectionCode === "FOOTER_SERVICE" ||
    sectionCode === "FOOTER_POLICY" ||
    !sectionCode
  )
}

export function isUlinkPolicyItem(item: UlinkItem): boolean {
  if (item.sectionCode === "FOOTER_POLICY") return true
  return (item.linkGroup || "").trim().toUpperCase() === "POLICY"
}

export async function uploadUlinkImage(
  file: File,
): Promise<{ ok: true; url: string } | { ok: false; message: string }> {
  try {
    const prepared = await prepareImageForUpload(file)
    const uploaded = await uploadEditorAsset(prepared)
    const url = uploaded.url?.trim()
    if (!url) return { ok: false, message: "업로드 응답에 이미지 URL이 없습니다." }
    return { ok: true, url }
  } catch (err) {
    const message = err instanceof Error ? err.message : "이미지 업로드에 실패했습니다."
    return { ok: false, message }
  }
}

export async function fetchPublicUlinkItems(options?: {
  sectionCode?: string
  domainId?: string
  size?: number
}): Promise<{ ok: true; data: UlinkItem[] } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", "0")
  params.set("size", String(options?.size ?? 50))
  params.set("openOnly", "true")
  if (options?.sectionCode) params.set("sectionCode", options.sectionCode)
  if (options?.domainId) params.set("domainId", options.domainId)
  try {
    const res = await fetch(`/api/v1/ulink/items?${params.toString()}`, { cache: "no-store" })
    const payload = await readJson(res)
    if (!res.ok) return { ok: false, message: readMessage(payload, "통합링크를 불러오지 못했습니다.") }
    const page = parsePage(payload)
    if (!page) return { ok: false, message: "통합링크 형식이 올바르지 않습니다." }
    return { ok: true, data: page.content }
  } catch {
    return { ok: false, message: "통합링크를 불러오지 못했습니다." }
  }
}

export async function fetchAdminUlinkItems(options?: {
  page?: number
  size?: number
  sectionCode?: string
  domainId?: string
}): Promise<{ ok: true; data: UlinkPage } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", String(options?.page ?? 0))
  params.set("size", String(options?.size ?? 20))
  params.set("openOnly", "false")
  if (options?.sectionCode) params.set("sectionCode", options.sectionCode)
  if (options?.domainId) params.set("domainId", options.domainId)
  const res = await fetch(`/api/v1/ulink/items?${params.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "통합링크 목록을 불러오지 못했습니다.") }
  const page = parsePage(payload)
  if (!page) return { ok: false, message: "통합링크 목록 형식이 올바르지 않습니다." }
  return { ok: true, data: page }
}

export async function createUlinkItem(
  input: UlinkItemInput,
): Promise<{ ok: true; data: UlinkItem } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/ulink/items", {
    method: "POST",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "통합링크를 등록하지 못했습니다.") }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "등록 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function updateUlinkItem(
  id: string,
  input: UlinkItemInput,
): Promise<{ ok: true; data: UlinkItem } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/ulink/items/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "통합링크를 수정하지 못했습니다.") }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "수정 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function deleteUlinkItem(
  id: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/ulink/items/${encodeURIComponent(id)}`, {
    method: "DELETE",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "통합링크를 삭제하지 못했습니다.") }
  return { ok: true }
}
