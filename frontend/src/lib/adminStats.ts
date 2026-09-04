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

export type AdminStatsChartKey =
  | "signups"
  | "providers"
  | "logins"
  | "cumulative"
  | "status"
  | "content"
  | "news"
  | "health"
  | "metrics"

export type AdminStatsSeriesPoint = {
  date: string
  value: number
}

export type AdminStatsNamedSeries = {
  name: string
  label: string
  points: AdminStatsSeriesPoint[]
}

export type AdminStatsChart = {
  chartKey: string
  title: string
  unit: string
  series: AdminStatsNamedSeries[]
  from: string
  to: string
}

export type HealthStatusSnapshot = {
  status: string
  message: string
}

export type MetricsSnapshot = {
  heapUsedMb: number
  heapMaxMb: number
  heapUsagePercent: number
  threadCount: number
  processors: number
  systemLoadAverage: number
  cpuUsagePercent: number | null
  timestamp: number
}

export type AdminStatsOverview = {
  totalUsers: number
  approvedUsers: number
  pendingUsers: number
  suspendedUsers: number
  withdrawnUsers: number
  totalBoards: number
  totalComments: number
  totalNews: number
  healthSnapshot: {
    overall?: HealthStatusSnapshot
    database?: HealthStatusSnapshot
    redis?: HealthStatusSnapshot
    externalApis?: Record<string, HealthStatusSnapshot>
  }
  metricsSnapshot?: MetricsSnapshot
}

function parsePoint(raw: unknown): AdminStatsSeriesPoint | null {
  const o = asRecord(raw)
  if (!o) return null
  const date = typeof o.date === "string" ? o.date : String(o.date ?? "")
  const value = typeof o.value === "number" ? o.value : Number(o.value)
  if (!date || !Number.isFinite(value)) return null
  return { date, value }
}

function parseSeries(raw: unknown): AdminStatsNamedSeries | null {
  const o = asRecord(raw)
  if (!o) return null
  const pointsRaw = Array.isArray(o.points) ? o.points : []
  const points = pointsRaw.map(parsePoint).filter((p): p is AdminStatsSeriesPoint => p != null)
  return {
    name: typeof o.name === "string" ? o.name : "",
    label: typeof o.label === "string" ? o.label : "",
    points,
  }
}

function parseHealthStatus(raw: unknown): HealthStatusSnapshot | undefined {
  const o = asRecord(raw)
  if (!o) return undefined
  return {
    status: typeof o.status === "string" ? o.status : "UNKNOWN",
    message: typeof o.message === "string" ? o.message : "",
  }
}

function parseMetricsSnapshot(raw: unknown): MetricsSnapshot | undefined {
  const o = asRecord(raw)
  if (!o) return undefined
  const cpuRaw = Number(o.cpuUsagePercent)
  return {
    heapUsedMb: Number(o.heapUsedMb) || 0,
    heapMaxMb: Number(o.heapMaxMb) || 0,
    heapUsagePercent: Number(o.heapUsagePercent) || 0,
    threadCount: Number(o.threadCount) || 0,
    processors: Number(o.processors) || 0,
    systemLoadAverage: Number(o.systemLoadAverage),
    cpuUsagePercent: Number.isFinite(cpuRaw) && cpuRaw >= 0 ? cpuRaw : null,
    timestamp: Number(o.timestamp) || 0,
  }
}

function parseOverview(raw: unknown): AdminStatsOverview | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const healthRaw = asRecord(data.healthSnapshot) ?? {}
  const externalRaw = asRecord(healthRaw.externalApis) ?? {}
  const externalApis: Record<string, HealthStatusSnapshot> = {}
  for (const [key, value] of Object.entries(externalRaw)) {
    const parsed = parseHealthStatus(value)
    if (parsed) externalApis[key] = parsed
  }
  return {
    totalUsers: Number(data.totalUsers) || 0,
    approvedUsers: Number(data.approvedUsers) || 0,
    pendingUsers: Number(data.pendingUsers) || 0,
    suspendedUsers: Number(data.suspendedUsers) || 0,
    withdrawnUsers: Number(data.withdrawnUsers) || 0,
    totalBoards: Number(data.totalBoards) || 0,
    totalComments: Number(data.totalComments) || 0,
    totalNews: Number(data.totalNews) || 0,
    healthSnapshot: {
      overall: parseHealthStatus(healthRaw.overall),
      database: parseHealthStatus(healthRaw.database),
      redis: parseHealthStatus(healthRaw.redis),
      externalApis,
    },
    metricsSnapshot: parseMetricsSnapshot(data.metricsSnapshot),
  }
}

function parseChart(raw: unknown): AdminStatsChart | null {
  const root = asRecord(raw)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  const seriesRaw = Array.isArray(data.series) ? data.series : []
  return {
    chartKey: typeof data.chartKey === "string" ? data.chartKey : "",
    title: typeof data.title === "string" ? data.title : "",
    unit: typeof data.unit === "string" ? data.unit : "명",
    series: seriesRaw.map(parseSeries).filter((s): s is AdminStatsNamedSeries => s != null),
    from: typeof data.from === "string" ? data.from : "",
    to: typeof data.to === "string" ? data.to : "",
  }
}

export async function fetchAdminStatsOverview(): Promise<
  { ok: true; data: AdminStatsOverview } | { ok: false; message: string }
> {
  try {
    const res = await fetch("/api/v1/admin/stats/overview", {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    const payload = await readJson(res)
    if (!res.ok) {
      return { ok: false, message: readMessage(payload, "통계 개요를 불러오지 못했습니다.") }
    }
    const data = parseOverview(payload)
    if (!data) return { ok: false, message: "통계 개요 형식이 올바르지 않습니다." }
    return { ok: true, data }
  } catch {
    return { ok: false, message: "서버에 연결하지 못했습니다. Next/백엔드가 실행 중인지 확인해 주세요." }
  }
}

export async function fetchAdminStatsChart(
  chartKey: AdminStatsChartKey,
  options?: { days?: number; from?: string; to?: string },
): Promise<{ ok: true; data: AdminStatsChart } | { ok: false; message: string }> {
  try {
    const qs = new URLSearchParams()
    if (options?.from && options?.to) {
      qs.set("from", options.from)
      qs.set("to", options.to)
    } else {
      qs.set("days", String(options?.days ?? 7))
    }
    const res = await fetch(`/api/v1/admin/stats/charts/${chartKey}?${qs.toString()}`, {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    const payload = await readJson(res)
    if (!res.ok) {
      return { ok: false, message: readMessage(payload, "통계 차트를 불러오지 못했습니다.") }
    }
    const data = parseChart(payload)
    if (!data) return { ok: false, message: "통계 차트 형식이 올바르지 않습니다." }
    return { ok: true, data }
  } catch {
    return { ok: false, message: "서버에 연결하지 못했습니다. Next/백엔드가 실행 중인지 확인해 주세요." }
  }
}

export async function refreshAdminHealth(): Promise<
  { ok: true } | { ok: false; message: string }
> {
  try {
    const res = await fetch("/api/v1/admin/stats/health/refresh", {
      method: "POST",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    const payload = await readJson(res)
    if (!res.ok) {
      return { ok: false, message: readMessage(payload, "헬스 상태를 새로고침하지 못했습니다.") }
    }
    return { ok: true }
  } catch {
    return { ok: false, message: "서버에 연결하지 못했습니다. Next/백엔드가 실행 중인지 확인해 주세요." }
  }
}
