"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchHealthDatabase,
  fetchHealthExternal,
  fetchHealthMetrics,
  fetchSystemHealth,
  refreshHealthStatus,
  type HealthMetricsView,
  type HealthStatusView,
  type SystemHealthView,
} from "@/lib/health"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

function statusTone(status: string | undefined): string {
  const s = (status ?? "").toUpperCase()
  if (s === "UP" || s === "HEALTHY") return "text-emerald-700 bg-emerald-50 border-emerald-200"
  if (s === "DOWN" || s === "UNHEALTHY") return "text-red-700 bg-red-50 border-red-200"
  return "text-amber-800 bg-amber-50 border-amber-200"
}

function StatusCard({
  title,
  status,
}: {
  title: string
  status: HealthStatusView | null | undefined
}) {
  if (!status) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-4">
        <p className="text-sm font-medium text-gray-800">{title}</p>
        <p className="mt-2 text-sm text-gray-400">데이터 없음</p>
      </div>
    )
  }
  return (
    <div className={`rounded-lg border p-4 ${statusTone(status.status)}`}>
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium">{title}</p>
        <span className="text-xs font-semibold tracking-wide">{status.status}</span>
      </div>
      {status.message ? <p className="mt-2 text-sm opacity-90">{status.message}</p> : null}
    </div>
  )
}

function MetricTable({ title, data }: { title: string; data: Record<string, unknown> }) {
  const entries = Object.entries(data)
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <p className="text-sm font-medium text-gray-900">{title}</p>
      {entries.length === 0 ? (
        <p className="mt-3 text-sm text-gray-400">메트릭 없음</p>
      ) : (
        <dl className="mt-3 grid gap-2 sm:grid-cols-2">
          {entries.map(([key, value]) => (
            <div key={key} className="rounded border border-gray-100 bg-gray-50 px-3 py-2">
              <dt className="text-xs text-gray-500">{key}</dt>
              <dd className="mt-0.5 break-all text-sm font-medium text-gray-900">
                {typeof value === "object" ? JSON.stringify(value) : String(value)}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  )
}

export default function AdminHealthClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [health, setHealth] = useState<SystemHealthView | null>(null)
  const [metrics, setMetrics] = useState<HealthMetricsView | null>(null)
  const [database, setDatabase] = useState<HealthStatusView | null>(null)
  const [external, setExternal] = useState<Record<string, HealthStatusView>>({})
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const loadAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    const [h, m, d, e] = await Promise.all([
      fetchSystemHealth(),
      fetchHealthMetrics(),
      fetchHealthDatabase(),
      fetchHealthExternal(),
    ])
    setLoading(false)

    const errors: string[] = []
    if (h.ok) setHealth(h.health)
    else errors.push(h.message)
    if (m.ok) setMetrics(m.metrics)
    else errors.push(m.message)
    if (d.ok) setDatabase(d.status)
    else errors.push(d.message)
    if (e.ok) setExternal(e.services)
    else errors.push(e.message)

    if (errors.length) setError(errors[0] ?? null)
  }, [])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace("/login")
      return
    }
    if (!canManageUsers(user.role)) {
      router.replace("/")
      return
    }
    void loadAll()
  }, [ready, user, router, loadAll])

  async function onRefresh() {
    setRefreshing(true)
    setMessage(null)
    setError(null)
    const result = await refreshHealthStatus()
    setRefreshing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage("상태를 새로고침했습니다.")
    await loadAll()
  }

  if (!ready || !user) {
    return <div className="min-h-[40vh]" />
  }

  if (!allowed) {
    return (
      <section className="mx-auto max-w-5xl px-4 py-12">
        <p className="text-sm text-red-600">관리자 권한이 필요합니다.</p>
      </section>
    )
  }

  const componentEntries = Object.entries(health?.components ?? {})
  const externalEntries = Object.entries(external)

  return (
    <section className="mx-auto max-w-5xl px-4 py-10 md:px-6 md:py-12">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">서버상황</h1>
          <p className="mt-1 text-sm text-gray-500">
            시스템 헬스·메트릭·DB·외부 서비스 상태. 추이 차트는{" "}
            <Link href="/admin/stats" className="text-finsight-primary underline-offset-2 hover:underline">
              통계
            </Link>
            에서 확인할 수 있습니다.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadAll()}>
            {loading ? "불러오는 중…" : "다시 조회"}
          </button>
          <button
            type="button"
            className={primaryButtonClass}
            disabled={refreshing || loading}
            onClick={() => void onRefresh()}
          >
            {refreshing ? "새로고침 중…" : "상태 새로고침"}
          </button>
        </div>
      </div>

      {error ? (
        <p className="mt-4 text-sm text-red-600" role="alert">
          {error}
        </p>
      ) : null}
      {message ? (
        <p className="mt-4 text-sm text-emerald-700" role="status">
          {message}
        </p>
      ) : null}

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatusCard title="전체 상태" status={health?.status} />
        <StatusCard title="데이터베이스" status={database} />
        <div className="rounded-lg border border-gray-200 bg-white p-4">
          <p className="text-sm font-medium text-gray-800">점검 시각</p>
          <p className="mt-2 text-sm text-gray-700">{health?.checkedAt ?? "-"}</p>
        </div>
      </div>

      <div className="mt-8">
        <h2 className="text-lg font-medium text-gray-900">구성 요소</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {componentEntries.length ? (
            componentEntries.map(([name, status]) => (
              <StatusCard key={name} title={name} status={status} />
            ))
          ) : (
            <p className="text-sm text-gray-400">구성 요소 상태 없음</p>
          )}
        </div>
      </div>

      <div className="mt-8">
        <h2 className="text-lg font-medium text-gray-900">외부 서비스</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {externalEntries.length ? (
            externalEntries.map(([name, status]) => (
              <StatusCard key={name} title={name} status={status} />
            ))
          ) : (
            <p className="text-sm text-gray-400">외부 서비스 상태 없음</p>
          )}
        </div>
      </div>

      <div className="mt-8 space-y-4">
        <h2 className="text-lg font-medium text-gray-900">시스템 메트릭</h2>
        <MetricTable title="JVM" data={metrics?.jvm ?? health?.metrics.jvm ?? {}} />
        <MetricTable title="System" data={metrics?.system ?? health?.metrics.system ?? {}} />
      </div>
    </section>
  )
}
