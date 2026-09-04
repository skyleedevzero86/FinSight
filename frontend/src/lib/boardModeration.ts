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

export type ModerationItem = {
  id: number
  title: string
  authorEmail: string
  boardType: string
  status: string
  reportCount: number
  createdAt: string | null
  updatedAt: string | null
}

export type ModerationRun = {
  runId: number
  hiddenCount: number
  reportThreshold: number
  triggeredBy: string
  actorEmail: string | null
  createdAt: string | null
  items: ModerationItem[]
}

function parseItem(raw: unknown): ModerationItem | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = Number(o.id)
  if (!Number.isFinite(id)) return null
  return {
    id,
    title: typeof o.title === "string" ? o.title : "",
    authorEmail: typeof o.authorEmail === "string" ? o.authorEmail : "",
    boardType: typeof o.boardType === "string" ? o.boardType : "",
    status: typeof o.status === "string" ? o.status : "",
    reportCount: Number(o.reportCount) || 0,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    updatedAt: typeof o.updatedAt === "string" ? o.updatedAt : null,
  }
}

function parseRun(raw: unknown): ModerationRun | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const itemsRaw = Array.isArray(data.items) ? data.items : []
  const runId = Number(data.runId)
  if (!Number.isFinite(runId)) return null
  return {
    runId,
    hiddenCount: Number(data.hiddenCount) || 0,
    reportThreshold: Number(data.reportThreshold) || 0,
    triggeredBy: typeof data.triggeredBy === "string" ? data.triggeredBy : "",
    actorEmail: typeof data.actorEmail === "string" ? data.actorEmail : null,
    createdAt: typeof data.createdAt === "string" ? data.createdAt : null,
    items: itemsRaw.map(parseItem).filter((x): x is ModerationItem => x != null),
  }
}

function parseItemList(raw: unknown): ModerationItem[] {
  const root = asRecord(raw)
  const data = root?.data
  const list = Array.isArray(data) ? data : Array.isArray(root) ? root : []
  return list.map(parseItem).filter((x): x is ModerationItem => x != null)
}

function parseRunList(raw: unknown): ModerationRun[] {
  const root = asRecord(raw)
  const data = root?.data
  const list = Array.isArray(data) ? data : []
  return list.map(parseRun).filter((x): x is ModerationRun => x != null)
}

export async function fetchModerationCandidates(
  reportThreshold: number,
): Promise<{ ok: true; data: ModerationItem[] } | { ok: false; message: string }> {
  const res = await fetch(
    `/api/v1/admin/boards/maintenance/candidates?reportThreshold=${reportThreshold}`,
    { headers: authHeadersJson(), cache: "no-store" },
  )
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "후보 목록을 불러오지 못했습니다.") }
  return { ok: true, data: parseItemList(payload) }
}

export async function fetchHiddenBoards(): Promise<
  { ok: true; data: ModerationItem[] } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/admin/boards/maintenance/hidden", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "숨김 목록을 불러오지 못했습니다.") }
  return { ok: true, data: parseItemList(payload) }
}

export async function hideOverReported(
  reportThreshold: number,
): Promise<{ ok: true; data: ModerationRun } | { ok: false; message: string }> {
  const res = await fetch(
    `/api/v1/admin/boards/maintenance/hide-over-reported?reportThreshold=${reportThreshold}`,
    { method: "POST", headers: authHeadersJson(), cache: "no-store" },
  )
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "일괄 숨김에 실패했습니다.") }
  const data = parseRun(payload)
  if (!data) return { ok: false, message: "숨김 결과 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function restoreModerationBoard(
  boardId: number,
): Promise<{ ok: true; data: ModerationItem } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/boards/maintenance/boards/${boardId}/restore`, {
    method: "POST",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "복구에 실패했습니다.") }
  const root = asRecord(payload)
  const item = parseItem(asRecord(root?.data) ?? root)
  if (!item) return { ok: false, message: "복구 응답 형식이 올바르지 않습니다." }
  return { ok: true, data: item }
}

export async function blockModerationBoard(
  boardId: number,
): Promise<{ ok: true; data: ModerationItem } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/boards/maintenance/boards/${boardId}/block`, {
    method: "POST",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "차단에 실패했습니다.") }
  const root = asRecord(payload)
  const item = parseItem(asRecord(root?.data) ?? root)
  if (!item) return { ok: false, message: "차단 응답 형식이 올바르지 않습니다." }
  return { ok: true, data: item }
}

export async function fetchModerationRuns(
  limit = 20,
): Promise<{ ok: true; data: ModerationRun[] } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/boards/maintenance/runs?limit=${limit}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "실행 이력을 불러오지 못했습니다.") }
  return { ok: true, data: parseRunList(payload) }
}

export async function fetchModerationRunDetail(
  runId: number,
): Promise<{ ok: true; data: ModerationRun } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/boards/maintenance/runs/${runId}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "실행 상세를 불러오지 못했습니다.") }
  const data = parseRun(payload)
  if (!data) return { ok: false, message: "실행 상세 형식이 올바르지 않습니다." }
  return { ok: true, data }
}

export async function reportBoard(
  boardId: number,
  reason: string,
  description?: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/boards/${boardId}/report`, {
    method: "POST",
    headers: authHeadersJson(),
    body: JSON.stringify({ reason, description: description ?? "" }),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "신고에 실패했습니다.") }
  return { ok: true }
}

export const REPORT_REASONS = [
  { value: "SPAM", label: "스팸·광고" },
  { value: "ABUSE", label: "욕설·비방" },
  { value: "ILLEGAL", label: "불법·유해 정보" },
  { value: "PRIVACY", label: "개인정보 노출" },
  { value: "OTHER", label: "기타" },
] as const
