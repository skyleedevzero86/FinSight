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

function unwrapData(payload: unknown): Record<string, unknown> | null {
  const root = asRecord(payload)
  if (!root) return null
  return asRecord(root.data) ?? root
}

export type HealthStatusView = {
  status: string
  message: string
  details?: Record<string, unknown> | null
}

export type SystemHealthView = {
  id: string | null
  status: HealthStatusView | null
  checkedAt: string | null
  components: Record<string, HealthStatusView>
  metrics: {
    jvm: Record<string, unknown>
    system: Record<string, unknown>
  }
}

export type HealthMetricsView = {
  jvm: Record<string, unknown>
  system: Record<string, unknown>
  timestamp: number | null
}

function parseStatus(raw: unknown): HealthStatusView | null {
  const o = asRecord(raw)
  if (!o) return null
  return {
    status: typeof o.status === "string" ? o.status : "UNKNOWN",
    message: typeof o.message === "string" ? o.message : "",
    details: asRecord(o.details),
  }
}

function parseComponents(raw: unknown): Record<string, HealthStatusView> {
  const o = asRecord(raw)
  if (!o) return {}
  const out: Record<string, HealthStatusView> = {}
  for (const [key, value] of Object.entries(o)) {
    const parsed = parseStatus(value)
    if (parsed) out[key] = parsed
  }
  return out
}

export async function fetchSystemHealth(): Promise<
  { ok: true; health: SystemHealthView } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/health", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "시스템 상태를 불러오지 못했습니다.") }
  }
  const data = unwrapData(payload)
  if (!data) return { ok: false, message: "시스템 상태 형식이 올바르지 않습니다." }
  const metrics = asRecord(data.metrics)
  return {
    ok: true,
    health: {
      id: typeof data.id === "string" ? data.id : null,
      status: parseStatus(data.status),
      checkedAt: typeof data.checkedAt === "string" ? data.checkedAt : null,
      components: parseComponents(data.componentStatuses),
      metrics: {
        jvm: asRecord(metrics?.jvmMetrics) ?? asRecord(metrics?.jvm) ?? {},
        system: asRecord(metrics?.systemMetrics) ?? asRecord(metrics?.system) ?? {},
      },
    },
  }
}

export async function fetchHealthMetrics(): Promise<
  { ok: true; metrics: HealthMetricsView } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/health/metrics", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메트릭을 불러오지 못했습니다.") }
  }
  const data = unwrapData(payload)
  if (!data) return { ok: false, message: "메트릭 형식이 올바르지 않습니다." }
  return {
    ok: true,
    metrics: {
      jvm: asRecord(data.jvm) ?? {},
      system: asRecord(data.system) ?? {},
      timestamp: typeof data.timestamp === "number" ? data.timestamp : null,
    },
  }
}

export async function fetchHealthDatabase(): Promise<
  { ok: true; status: HealthStatusView } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/health/database", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "DB 상태를 불러오지 못했습니다.") }
  }
  const data = unwrapData(payload)
  const parsed = parseStatus(data)
  if (!parsed) return { ok: false, message: "DB 상태 형식이 올바르지 않습니다." }
  return { ok: true, status: parsed }
}

export async function fetchHealthExternal(): Promise<
  | { ok: true; services: Record<string, HealthStatusView>; timestamp: number | null }
  | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/health/external-services", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "외부 서비스 상태를 불러오지 못했습니다.") }
  }
  const data = unwrapData(payload)
  if (!data) return { ok: false, message: "외부 서비스 상태 형식이 올바르지 않습니다." }
  const timestamp = typeof data.timestamp === "number" ? data.timestamp : null
  const services: Record<string, HealthStatusView> = {}
  for (const [key, value] of Object.entries(data)) {
    if (key === "timestamp") continue
    const parsed = parseStatus(value)
    if (parsed) services[key] = parsed
  }
  return {
    ok: true,
    services,
    timestamp,
  }
}

export async function refreshHealthStatus(): Promise<
  { ok: true } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/health/refresh", {
    method: "POST",
    headers: authHeadersJson(),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "상태 새로고침에 실패했습니다.") }
  }
  return { ok: true }
}
