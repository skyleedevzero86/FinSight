import { LIVE_VOD_NAV_ITEMS } from "@/data/liveVodNavData"
import { authHeadersJson, clearAuthSession } from "@/lib/finsightToken"
import { displayYoutubeThumbnail } from "@/lib/liveVod"

export const mediaFieldClass =
  "w-full border border-[#d7dee8] bg-white px-3 py-2 text-sm text-gray-900 outline-none"

export const mediaButtonClass =
  "border border-[#d7dee8] bg-white px-3 py-2 text-sm text-gray-800 disabled:opacity-50"

export const mediaPrimaryClass =
  "border border-[#1a1f2e] bg-[#1a1f2e] px-3 py-2 text-sm text-white disabled:opacity-50"

export type MediaImportStatus = "DRAFT" | "PUBLISHED" | "HIDDEN" | "FAILED"
export type MediaAiStatus = "PENDING" | "DONE" | "FAILED"
export type MediaSourceType = "CHANNEL_HANDLE" | "CHANNEL_ID" | "PLAYLIST_ID" | "MANUAL_URL"
export type MediaReviewStatus = "PENDING" | "ACTIVE" | "STOPPED"

export type AdminMediaVideo = {
  boardId: number
  title: string
  videoId: string
  channelTitle: string | null
  sourceValue: string | null
  category: string | null
  thumbnailUrl: string | null
  importStatus: MediaImportStatus | null
  aiStatus: MediaAiStatus
  publishedAt: string | null
  createdAt: string | null
}

export type AdminMediaVideoDetail = AdminMediaVideo & {
  content: string
  youtubeTitle: string | null
  youtubeDescription: string | null
  summary: string | null
  editorComment: string | null
  keyPoints: string[]
  hashtags: string[]
  aiFailedAt: string | null
}

export type AdminMediaSource = {
  id: number
  sourceType: MediaSourceType
  sourceValue: string
  category: string | null
  active: boolean
  reviewStatus: MediaReviewStatus
  lastSyncedAt: string | null
  totalVideoCount: number
}

export type MediaPage<T> = {
  content: T[]
  page: number
  totalPages: number
  totalElements: number
}

export type MediaSyncSummary = {
  importedCount: number
  updatedCount: number
  failedCount: number
}

export type MediaEnrichSummary = {
  requestedCount: number
  enrichedCount: number
  skippedCount: number
  failedCount: number
}

export type MediaCall<T> =
  | { ok: true; data: T }
  | { ok: false; message: string; forbidden?: boolean; unauthorized?: boolean }

export const MEDIA_STATUS_OPTIONS: { value: "" | MediaImportStatus; label: string }[] = [
  { value: "", label: "전체" },
  { value: "DRAFT", label: "초안" },
  { value: "PUBLISHED", label: "게시" },
  { value: "HIDDEN", label: "숨김" },
]

export const MEDIA_SOURCE_TYPES: { value: MediaSourceType; label: string }[] = [
  { value: "CHANNEL_HANDLE", label: "채널 핸들" },
  { value: "CHANNEL_ID", label: "채널 ID" },
  { value: "PLAYLIST_ID", label: "재생목록" },
  { value: "MANUAL_URL", label: "수동 URL" },
]

export const MEDIA_CATEGORIES = LIVE_VOD_NAV_ITEMS.filter((item) => item.tab !== "ALL")

const IMPORT_LABEL: Record<string, string> = {
  DRAFT: "초안",
  PUBLISHED: "게시",
  HIDDEN: "숨김",
  FAILED: "실패",
}

const AI_LABEL: Record<MediaAiStatus, string> = {
  PENDING: "대기",
  DONE: "완료",
  FAILED: "실패",
}

const REVIEW_LABEL: Record<MediaReviewStatus, string> = {
  PENDING: "검토 대기",
  ACTIVE: "활성",
  STOPPED: "중지",
}

export function categoryLabel(category: string | null): string {
  if (!category) return "-"
  return LIVE_VOD_NAV_ITEMS.find((item) => item.tab === category)?.label ?? category
}

export function importStatusLabel(status: string | null): string {
  if (!status) return "-"
  return IMPORT_LABEL[status] ?? status
}

export function aiStatusLabel(status: MediaAiStatus): string {
  return AI_LABEL[status]
}

export function reviewStatusLabel(status: MediaReviewStatus): string {
  return REVIEW_LABEL[status]
}

export function sourceTypeLabel(sourceType: string): string {
  return MEDIA_SOURCE_TYPES.find((item) => item.value === sourceType)?.label ?? sourceType
}

export function formatMediaDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

export function videoListDate(video: AdminMediaVideo): string {
  return formatMediaDate(video.publishedAt ?? video.createdAt)
}

export function formatSyncMessage(summary: MediaSyncSummary): string {
  return `가져옴 ${summary.importedCount}건, 갱신 ${summary.updatedCount}건, 실패 ${summary.failedCount}건`
}

export function formatEnrichMessage(summary: MediaEnrichSummary): string {
  if (summary.requestedCount === 0) return "보강할 초안이 없습니다."
  return `대기 초안 ${summary.requestedCount}건 중 보강 ${summary.enrichedCount}건, 건너뜀 ${summary.skippedCount}건, 실패 ${summary.failedCount}건`
}

export function buildPublishBody(detail: AdminMediaVideoDetail): {
  title: string
  content: string
  hashtags: string[]
} {
  const title = (detail.title || detail.youtubeTitle || "제목 없음").slice(0, 200)
  const content = (detail.content || detail.summary || detail.youtubeDescription || title).slice(0, 10000)
  return { title, content, hashtags: detail.hashtags }
}

export type VideoQuery = {
  page: number
  importStatus: string
  category: string
  sourceId: string
  keyword: string
}

export function fetchAdminMediaVideos(query: VideoQuery): Promise<MediaCall<MediaPage<AdminMediaVideo>>> {
  const params = new URLSearchParams()
  params.set("page", String(query.page))
  params.set("size", "20")
  if (query.importStatus) params.set("importStatus", query.importStatus)
  if (query.category) params.set("category", query.category)
  if (query.sourceId) params.set("sourceId", query.sourceId)
  if (query.keyword) params.set("keyword", query.keyword)
  return requestJson(`/api/v1/admin/media/videos?${params.toString()}`, { method: "GET" }, "영상 목록을 불러오지 못했습니다.").then(
    (result) => (result.ok ? { ok: true, data: parseVideoPage(result.data) } : result),
  )
}

export function fetchAdminMediaVideo(boardId: number): Promise<MediaCall<AdminMediaVideoDetail>> {
  return requestJson(`/api/v1/admin/media/videos/${boardId}`, { method: "GET" }, "영상 상세를 불러오지 못했습니다.").then(
    (result) => {
      if (!result.ok) return result
      const detail = parseVideoDetail(unwrapData(result.data))
      if (!detail) return { ok: false, message: "영상 상세 형식이 올바르지 않습니다." }
      return { ok: true, data: detail }
    },
  )
}

export function publishAdminMediaVideo(
  boardId: number,
  body: { title: string; content: string; hashtags: string[] },
): Promise<MediaCall<AdminMediaVideoDetail>> {
  return requestJson(
    `/api/v1/admin/media/videos/${boardId}/publish`,
    { method: "POST", body: JSON.stringify(body) },
    "영상을 게시하지 못했습니다.",
  ).then((result) => mapDetail(result, "영상은 게시됐지만 응답을 해석하지 못했습니다."))
}

export function hideAdminMediaVideo(boardId: number): Promise<MediaCall<AdminMediaVideoDetail>> {
  return requestJson(
    `/api/v1/admin/media/videos/${boardId}/hide`,
    { method: "POST", body: "{}" },
    "영상을 숨기지 못했습니다.",
  ).then((result) => mapDetail(result, "영상은 숨겼지만 응답을 해석하지 못했습니다."))
}

export function enrichAdminMediaVideo(boardId: number): Promise<MediaCall<AdminMediaVideoDetail>> {
  return requestJson(
    `/api/v1/admin/media/videos/${boardId}/enrich`,
    { method: "POST", body: "{}" },
    "관리자에게 문의주세요.",
  ).then((result) => mapDetail(result, "보강은 끝났지만 응답을 해석하지 못했습니다."))
}

export function enrichAdminMediaVideos(): Promise<MediaCall<MediaEnrichSummary>> {
  return requestJson("/api/v1/admin/media/import/enrich", { method: "POST", body: "{}" }, "AI 보강에 실패했습니다.").then(
    (result) => (result.ok ? { ok: true, data: parseEnrich(unwrapData(result.data)) } : result),
  )
}

export function importAdminMediaUrls(body: {
  urls: string[]
  category: string
}): Promise<MediaCall<MediaSyncSummary>> {
  return requestJson(
    "/api/v1/admin/media/import/manual",
    { method: "POST", body: JSON.stringify({ urls: body.urls, category: body.category, hashtags: [], autoPublish: false }) },
    "URL을 가져오지 못했습니다.",
  ).then((result) => (result.ok ? { ok: true, data: parseSync(unwrapData(result.data)) } : result))
}

export function fetchAdminMediaSources(): Promise<MediaCall<AdminMediaSource[]>> {
  return requestJson("/api/v1/admin/media/sources", { method: "GET" }, "수집 소스를 불러오지 못했습니다.").then((result) => {
    if (!result.ok) return result
    const data = unwrapData(result.data)
    const list = Array.isArray(data) ? data : []
    return { ok: true, data: list.map(parseSource).filter((item): item is AdminMediaSource => item !== null) }
  })
}

export function createAdminMediaSource(body: {
  sourceType: MediaSourceType
  sourceValue: string
  category: string
  active: boolean
}): Promise<MediaCall<AdminMediaSource>> {
  return requestJson(
    "/api/v1/admin/media/sources",
    {
      method: "POST",
      body: JSON.stringify({ ...body, autoPublish: false }),
    },
    "수집 소스를 등록하지 못했습니다.",
  ).then((result) => mapSource(result, "소스는 등록됐지만 응답을 해석하지 못했습니다."))
}

export function updateAdminMediaSourceState(
  sourceId: number,
  active: boolean,
  rejected: boolean,
): Promise<MediaCall<AdminMediaSource>> {
  return requestJson(
    `/api/v1/admin/media/sources/${sourceId}/active`,
    { method: "POST", body: JSON.stringify({ active, rejected }) },
    "수집 소스 상태를 바꾸지 못했습니다.",
  ).then((result) => mapSource(result, "상태는 바뀌었지만 응답을 해석하지 못했습니다."))
}

export function syncAdminMediaSource(sourceId: number): Promise<MediaCall<MediaSyncSummary>> {
  return requestJson(
    `/api/v1/admin/media/sources/${sourceId}/sync`,
    { method: "POST", body: "{}" },
    "소스를 동기화하지 못했습니다.",
  ).then((result) => (result.ok ? { ok: true, data: parseSync(unwrapData(result.data)) } : result))
}

export function syncAllAdminMediaSources(): Promise<MediaCall<MediaSyncSummary>> {
  return requestJson("/api/v1/admin/media/import/sync", { method: "POST", body: "{}" }, "전체 동기화에 실패했습니다.").then(
    (result) => (result.ok ? { ok: true, data: parseSync(unwrapData(result.data)) } : result),
  )
}

function mapDetail(result: MediaCall<unknown>, fallback: string): MediaCall<AdminMediaVideoDetail> {
  if (!result.ok) return result
  const detail = parseVideoDetail(unwrapData(result.data))
  if (!detail) return { ok: false, message: fallback }
  return { ok: true, data: detail }
}

function mapSource(result: MediaCall<unknown>, fallback: string): MediaCall<AdminMediaSource> {
  if (!result.ok) return result
  const source = parseSource(unwrapData(result.data))
  if (!source) return { ok: false, message: fallback }
  return { ok: true, data: source }
}

async function requestJson(path: string, init: RequestInit, fallback: string): Promise<MediaCall<unknown>> {
  try {
    const res = await fetch(path, {
      ...init,
      headers: { ...authHeadersJson(), ...(init.headers ?? {}) },
      credentials: "include",
      cache: "no-store",
    })
    const payload = await readJson(res)
    if (res.status === 401) {
      clearAuthSession({ emit: true })
      return {
        ok: false,
        message: "로그인이 만료되었거나 유효하지 않습니다. 다시 로그인해 주세요.",
        unauthorized: true,
      }
    }
    if (res.status === 403) {
      return { ok: false, message: "이 메뉴는 관리자만 사용할 수 있습니다.", forbidden: true }
    }
    if (!res.ok) return { ok: false, message: readMessage(payload, fallback) }
    return { ok: true, data: payload }
  } catch {
    return { ok: false, message: "서버에 연결하지 못했습니다. Next와 백엔드가 실행 중인지 확인해 주세요." }
  }
}

function parseVideoPage(payload: unknown): MediaPage<AdminMediaVideo> {
  const data = asRecord(unwrapData(payload)) ?? {}
  const rawList = Array.isArray(data.content) ? data.content : []
  const totalElements = readNumber(data.totalElements) ?? rawList.length
  const size = readNumber(data.size) ?? 20
  const totalPages = readNumber(data.totalPages) ?? Math.max(1, Math.ceil(totalElements / Math.max(size, 1)))
  return {
    content: rawList.map(parseVideo).filter((item): item is AdminMediaVideo => item !== null),
    page: readNumber(data.page) ?? 0,
    totalPages: Math.max(1, totalPages),
    totalElements,
  }
}

function parseVideo(raw: unknown): AdminMediaVideo | null {
  const row = asRecord(raw)
  const boardId = readNumber(row?.boardId)
  if (!row || boardId === null) return null
  return {
    boardId,
    title: readString(row.title) || "제목 없음",
    videoId: readString(row.videoId),
    channelTitle: readString(row.channelTitle) || null,
    sourceValue: readString(row.sourceValue) || null,
    category: readString(row.category) || null,
    thumbnailUrl: displayYoutubeThumbnail(readString(row.videoId), readString(row.thumbnailUrl)) || null,
    importStatus: readImportStatus(row.importStatus),
    aiStatus: readAiStatus(row),
    publishedAt: readString(row.publishedAt) || null,
    createdAt: readString(row.createdAt) || null,
  }
}

function parseVideoDetail(raw: unknown): AdminMediaVideoDetail | null {
  const video = parseVideo(raw)
  const row = asRecord(raw)
  if (!video || !row) return null
  return {
    ...video,
    content: readString(row.content),
    youtubeTitle: readString(row.youtubeTitle) || null,
    youtubeDescription: readString(row.youtubeDescription) || null,
    summary: readString(row.summary) || null,
    editorComment: readString(row.editorComment) || null,
    keyPoints: Array.isArray(row.keyPoints) ? row.keyPoints.filter((item): item is string => typeof item === "string") : [],
    hashtags: Array.isArray(row.hashtags) ? row.hashtags.filter((item): item is string => typeof item === "string") : [],
    aiFailedAt: readString(row.aiFailedAt) || null,
  }
}

function parseSource(raw: unknown): AdminMediaSource | null {
  const row = asRecord(raw)
  const id = readNumber(row?.id)
  if (!row || id === null) return null
  const sourceType = readSourceType(row.sourceType)
  if (!sourceType) return null
  return {
    id,
    sourceType,
    sourceValue: readString(row.sourceValue),
    category: readString(row.category) || null,
    active: row.active === true,
    reviewStatus: readReviewStatus(row),
    lastSyncedAt: readString(row.lastSyncedAt) || null,
    totalVideoCount: readNumber(row.totalVideoCount) ?? 0,
  }
}

function parseSync(raw: unknown): MediaSyncSummary {
  const row = asRecord(raw) ?? {}
  return {
    importedCount: readNumber(row.importedCount) ?? 0,
    updatedCount: readNumber(row.updatedCount) ?? 0,
    failedCount: readNumber(row.failedCount) ?? 0,
  }
}

function parseEnrich(raw: unknown): MediaEnrichSummary {
  const row = asRecord(raw) ?? {}
  return {
    requestedCount: readNumber(row.requestedCount) ?? 0,
    enrichedCount: readNumber(row.enrichedCount) ?? 0,
    skippedCount: readNumber(row.skippedCount) ?? 0,
    failedCount: readNumber(row.failedCount) ?? 0,
  }
}

function readAiStatus(row: Record<string, unknown>): MediaAiStatus {
  if (row.aiStatus === "DONE" || row.aiStatus === "FAILED" || row.aiStatus === "PENDING") return row.aiStatus
  if (readString(row.aiGeneratedAt)) return "DONE"
  if (readString(row.aiFailedAt)) return "FAILED"
  return "PENDING"
}

function readReviewStatus(row: Record<string, unknown>): MediaReviewStatus {
  if (row.reviewStatus === "PENDING" || row.reviewStatus === "ACTIVE" || row.reviewStatus === "STOPPED") {
    return row.reviewStatus
  }
  if (row.active === true) return "ACTIVE"
  if (row.rejected === true || readString(row.lastSyncedAt)) return "STOPPED"
  return "PENDING"
}

function readImportStatus(value: unknown): MediaImportStatus | null {
  if (value === "DRAFT" || value === "PUBLISHED" || value === "HIDDEN" || value === "FAILED") return value
  return null
}

function readSourceType(value: unknown): MediaSourceType | null {
  if (value === "CHANNEL_HANDLE" || value === "CHANNEL_ID" || value === "PLAYLIST_ID" || value === "MANUAL_URL") {
    return value
  }
  return null
}

function unwrapData(payload: unknown): unknown {
  const root = asRecord(payload)
  if (!root || !("data" in root)) return payload
  return root.data
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null
  return value as Record<string, unknown>
}

function readMessage(payload: unknown, fallback: string): string {
  const root = asRecord(payload)
  if (!root) return fallback
  if (typeof root.message === "string" && root.message) return root.message
  const data = asRecord(root.data)
  if (data && typeof data.message === "string" && data.message) return data.message
  return fallback
}

function readString(value: unknown): string {
  return typeof value === "string" ? value : ""
}

function readNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value
  if (typeof value === "string" && value.trim() && Number.isFinite(Number(value))) return Number(value)
  return null
}

async function readJson(res: Response): Promise<unknown> {
  try {
    return await res.json()
  } catch {
    return null
  }
}
