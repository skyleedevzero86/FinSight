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

export type MainimgItem = {
  id: string
  domainId: string | null
  imageName: string
  image: string | null
  imageFile: string | null
  description: string | null
  linkUrl: string | null
  noticeBegin: string | null
  noticeEnd: string | null
  reflectYn: string
  sortOrder: number
  createdAt: string | null
  updatedAt: string | null
}

export type MainimgItemInput = {
  domainId?: string
  imageName: string
  image?: string
  imageFile?: string
  description?: string
  linkUrl?: string
  noticeBegin?: string
  noticeEnd?: string
  reflectYn?: string
  sortOrder?: number
}

export type MainimgPage = {
  content: MainimgItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

function parseItem(raw: unknown): MainimgItem | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "string" ? o.id : ""
  const imageName = typeof o.imageName === "string" ? o.imageName : ""
  if (!id || !imageName) return null
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
    imageName,
    image: typeof o.image === "string" ? o.image : null,
    imageFile: typeof o.imageFile === "string" ? o.imageFile : null,
    description: typeof o.description === "string" ? o.description : null,
    linkUrl: typeof o.linkUrl === "string" ? o.linkUrl : null,
    noticeBegin: typeof o.noticeBegin === "string" ? o.noticeBegin : null,
    noticeEnd: typeof o.noticeEnd === "string" ? o.noticeEnd : null,
    reflectYn: typeof o.reflectYn === "string" ? o.reflectYn : "Y",
    sortOrder: Number.isFinite(sortOrder) ? sortOrder : 0,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    updatedAt: typeof o.updatedAt === "string" ? o.updatedAt : null,
  }
}

function parsePage(raw: unknown): MainimgPage | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const contentRaw = Array.isArray(data.content) ? data.content : []
  const content = contentRaw.map(parseItem).filter((x): x is MainimgItem => x != null)
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

function parseOne(raw: unknown): MainimgItem | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  return parseItem(data)
}

export function resolveMainimgUrl(item: Pick<MainimgItem, "image" | "imageFile">): string {
  return (item.image || item.imageFile || "").trim()
}

export async function uploadMainimgFile(
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

export async function fetchPublicMainimgItems(options?: {
  domainId?: string
  size?: number
}): Promise<{ ok: true; data: MainimgItem[] } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("reflectOnly", "true")
  params.set("page", "0")
  params.set("size", String(options?.size ?? 20))
  if (options?.domainId) params.set("domainId", options.domainId)
  const res = await fetch(`/api/v1/mainimg/items?${params.toString()}`, {
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메인이미지를 불러오지 못했습니다.") }
  }
  const page = parsePage(payload)
  if (!page) return { ok: false, message: "메인이미지 형식이 올바르지 않습니다." }
  return { ok: true, data: page.content }
}

export async function fetchAdminMainimgItems(options?: {
  page?: number
  size?: number
  domainId?: string
  reflectOnly?: boolean
}): Promise<{ ok: true; data: MainimgPage } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", String(options?.page ?? 0))
  params.set("size", String(options?.size ?? 20))
  params.set("reflectOnly", String(options?.reflectOnly ?? false))
  if (options?.domainId) params.set("domainId", options.domainId)
  const res = await fetch(`/api/v1/mainimg/items?${params.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메인이미지 목록을 불러오지 못했습니다.") }
  }
  const page = parsePage(payload)
  if (!page) return { ok: false, message: "메인이미지 목록 형식이 올바르지 않습니다." }
  return { ok: true, data: page }
}

export async function createMainimgItem(
  input: MainimgItemInput,
): Promise<{ ok: true; data: MainimgItem } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/mainimg/items", {
    method: "POST",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메인이미지를 등록하지 못했습니다.") }
  }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "등록 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function updateMainimgItem(
  id: string,
  input: MainimgItemInput,
): Promise<{ ok: true; data: MainimgItem } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/mainimg/items/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메인이미지를 수정하지 못했습니다.") }
  }
  const data = parseOne(payload)
  if (!data) return { ok: false, message: "수정 응답 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function deleteMainimgItem(
  id: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/mainimg/items/${encodeURIComponent(id)}`, {
    method: "DELETE",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메인이미지를 삭제하지 못했습니다.") }
  }
  return { ok: true }
}
