"use client"

import Link from "next/link"
import { useCallback, useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchAdminStatsChart,
  fetchAdminStatsOverview,
  refreshAdminHealth,
  type AdminStatsChart,
  type AdminStatsNamedSeries,
  type AdminStatsOverview,
  type HealthStatusSnapshot,
  type MetricsSnapshot,
} from "@/lib/adminStats"

type PeriodMode = "daily" | "weekly" | "monthly" | "custom"

const SERIES_COLORS = ["#22c55e", "#f59e0b", "#3b82f6", "#ef4444", "#a855f7"]

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoIso(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - (days - 1))
  return d.toISOString().slice(0, 10)
}

function formatDayLabel(iso: string): string {
  if (iso.length >= 10 && iso.includes("-")) {
    return iso.slice(5, 10).replace("-", "/")
  }
  return iso
}

function statusRank(status: string | undefined): "critical" | "warning" | "ok" | "info" {
  const s = (status ?? "").toUpperCase()
  if (s === "DOWN" || s === "UNHEALTHY") return "critical"
  if (s === "UNKNOWN" || s === "DEGRADED") return "warning"
  if (s === "UP" || s === "HEALTHY") return "ok"
  return "info"
}

function statusColor(status: string | undefined): string {
  const rank = statusRank(status)
  if (rank === "critical") return "border-black bg-red-50 text-red-800"
  if (rank === "warning") return "border-black bg-amber-50 text-amber-900"
  if (rank === "ok") return "border-black bg-emerald-50 text-emerald-800"
  return "border-black bg-sky-50 text-sky-900"
}

function statusLabelKo(status: string | undefined): string {
  const s = (status ?? "").toUpperCase()
  if (s === "UP" || s === "HEALTHY") return "정상"
  if (s === "DOWN" || s === "UNHEALTHY") return "장애"
  if (s === "DEGRADED") return "저하"
  if (s === "UNKNOWN") return "확인불가"
  return status || "-"
}

function healthMessageKo(message: string | undefined): string {
  if (!message) return ""
  const known: Record<string, string> = {
    "Some components are down": "일부 구성 요소에 장애가 있습니다",
    "System is healthy": "시스템이 정상입니다",
    "Database is healthy": "DB가 정상입니다",
    "Database connection validation failed": "DB 연결 검증에 실패했습니다",
    "Redis is healthy": "Redis가 정상입니다",
    "Redis connection check failed": "Redis 연결 확인에 실패했습니다",
    "Redis health check not implemented": "Redis 헬스체크가 구성되지 않았습니다",
  }
  if (known[message]) return known[message]

  let out = message
  out = out.replace(/^Database health check failed:\s*/i, "DB 헬스체크 실패: ")
  out = out.replace(/^Redis health check failed:\s*/i, "Redis 헬스체크 실패: ")
  out = out.replace(/^System health check failed:\s*/i, "시스템 헬스체크 실패: ")
  out = out.replace(/^(.+?) API is healthy$/i, "$1 API가 정상입니다")
  out = out.replace(/^(.+?) API returned status:\s*/i, "$1 API 응답 상태: ")
  out = out.replace(/^(.+?) API health check failed:\s*/i, "$1 API 헬스체크 실패: ")
  out = out.replace(/base URL이 설정되지 않았습니다/i, "기본 URL이 설정되지 않았습니다")
  out = out.replace(/api\.example\.com/gi, "미설정 URL")
  return out
}

function Gauge({
  label,
  percent,
  sub,
}: {
  label: string
  percent: number | null
  sub?: string
}) {
  const value = percent == null || !Number.isFinite(percent) ? null : Math.max(0, Math.min(100, percent))
  const tone =
    value == null ? "#9ca3af" : value >= 85 ? "#ef4444" : value >= 60 ? "#f59e0b" : "#10b981"
  const r = 42
  const semi = Math.PI * r
  const dash = value == null ? 0 : (semi * value) / 100

  return (
    <div className="flex flex-col items-center border border-black bg-white p-4">
      <svg viewBox="0 0 120 80" className="h-20 w-28">
        <path
          d="M18 70 A42 42 0 0 1 102 70"
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="10"
          strokeLinecap="round"
        />
        <path
          d="M18 70 A42 42 0 0 1 102 70"
          fill="none"
          stroke={tone}
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={`${dash} ${semi}`}
        />
        <text x="60" y="62" textAnchor="middle" fill="#111827" fontSize="18" fontWeight="700">
          {value == null ? "-" : `${Math.round(value)}%`}
        </text>
      </svg>
      <p className="mt-1 text-sm font-medium text-gray-800">{label}</p>
      {sub ? <p className="mt-0.5 text-xs text-gray-500">{sub}</p> : null}
    </div>
  )
}

function MiniLineChart({
  title,
  unit,
  series,
  rangeLabel,
}: {
  title: string
  unit: string
  series: AdminStatsNamedSeries[]
  rangeLabel: string
}) {
  const width = 640
  const height = 220
  const padL = 40
  const padR = 120
  const padT = 20
  const padB = 36
  const plotW = width - padL - padR
  const plotH = height - padT - padB

  const labels = useMemo(() => series[0]?.points.map((p) => p.date) ?? [], [series])
  const maxValue = useMemo(() => {
    let max = 0
    for (const s of series) {
      for (const p of s.points) max = Math.max(max, p.value)
    }
    return Math.max(1, Math.ceil(max / 5) * 5)
  }, [series])

  function xAt(index: number): number {
    const n = Math.max(labels.length, 1)
    if (n <= 1) return padL + plotW / 2
    return padL + (plotW * index) / (n - 1)
  }

  function yAt(value: number): number {
    return padT + plotH - (plotH * value) / maxValue
  }

  return (
    <div className="border border-black bg-white p-4">
      <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
          <p className="text-xs text-gray-500">{rangeLabel}</p>
        </div>
        <span className="text-xs text-gray-400">단위: {unit}</span>
      </div>
      <div className="overflow-x-auto">
        <svg viewBox={`0 0 ${width} ${height}`} className="h-auto min-w-[480px] w-full">
          {[0, 0.25, 0.5, 0.75, 1].map((t) => {
            const y = padT + plotH * (1 - t)
            const tick = Math.round(maxValue * t)
            return (
              <g key={t}>
                <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="#e5e7eb" strokeWidth="1" />
                <text x={padL - 8} y={y + 4} textAnchor="end" fill="#9ca3af" fontSize="10">
                  {tick}
                </text>
              </g>
            )
          })}
          {labels.map((label, i) =>
            i % Math.max(1, Math.floor(labels.length / 6)) === 0 || i === labels.length - 1 ? (
              <text
                key={`${label}-${i}`}
                x={xAt(i)}
                y={height - 10}
                textAnchor="middle"
                fill="#6b7280"
                fontSize="10"
              >
                {formatDayLabel(label)}
              </text>
            ) : null,
          )}
          {series.map((s, si) => {
            const color = SERIES_COLORS[si % SERIES_COLORS.length]
            const pts = s.points.map((p, i) => `${xAt(i)},${yAt(p.value)}`).join(" ")
            return (
              <g key={s.name || s.label}>
                <polyline fill="none" stroke={color} strokeWidth="2" points={pts} />
              </g>
            )
          })}
          {series.map((s, si) => {
            const color = SERIES_COLORS[si % SERIES_COLORS.length]
            const y = padT + 4 + si * 18
            return (
              <g key={`lg-${s.name}`}>
                <rect x={width - padR + 10} y={y} width="10" height="10" fill={color} />
                <text x={width - padR + 26} y={y + 9} fill="#374151" fontSize="11">
                  {s.label}
                </text>
              </g>
            )
          })}
        </svg>
      </div>
    </div>
  )
}

function collectStatuses(overview: AdminStatsOverview | null): { name: string; snap: HealthStatusSnapshot }[] {
  if (!overview) return []
  const hs = overview.healthSnapshot
  const list: { name: string; snap: HealthStatusSnapshot }[] = []
  if (hs.overall) list.push({ name: "전체", snap: hs.overall })
  if (hs.database) list.push({ name: "DB", snap: hs.database })
  if (hs.redis) list.push({ name: "Redis", snap: hs.redis })
  if (hs.externalApis) {
    for (const [k, v] of Object.entries(hs.externalApis)) {
      list.push({ name: k, snap: v })
    }
  }
  return list
}

function SkeletonBlock({ className }: { className?: string }) {
  return <div className={`animate-pulse bg-gray-200 ${className ?? ""}`} />
}

export default function AdminHealthClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [period, setPeriod] = useState<PeriodMode>("daily")
  const [customFrom, setCustomFrom] = useState(daysAgoIso(14))
  const [customTo, setCustomTo] = useState(todayIso())
  const [overview, setOverview] = useState<AdminStatsOverview | null>(null)
  const [healthChart, setHealthChart] = useState<AdminStatsChart | null>(null)
  const [metricsChart, setMetricsChart] = useState<AdminStatsChart | null>(null)
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<string | null>(null)

  const rangeQuery = useMemo(() => {
    if (period === "daily") return { days: 1 as number }
    if (period === "weekly") return { days: 7 as number }
    if (period === "monthly") return { days: 30 as number }
    return { from: customFrom, to: customTo }
  }, [period, customFrom, customTo])

  const rangeLabel = useMemo(() => {
    if (period === "daily") return `${todayIso()} (일별)`
    if (period === "weekly") return `${daysAgoIso(7)} ~ ${todayIso()} (주별)`
    if (period === "monthly") return `${daysAgoIso(30)} ~ ${todayIso()} (월별)`
    return `${customFrom} ~ ${customTo} (기간별)`
  }, [period, customFrom, customTo])

  const load = useCallback(async (opts?: { soft?: boolean }) => {
    const soft = Boolean(opts?.soft)
    if (!soft) setLoading(true)
    setError(null)
    try {
      // 카드(개요)를 먼저 그려 체감 지연을 줄이고, 차트는 이어서 로드
      const ov = await fetchAdminStatsOverview()
      if (!ov.ok) {
        setError(ov.message)
        return
      }
      setOverview(ov.data)
      setUpdatedAt(new Date().toLocaleString("ko-KR"))

      const [health, metrics] = await Promise.all([
        fetchAdminStatsChart("health", rangeQuery),
        fetchAdminStatsChart("metrics", rangeQuery),
      ])
      if (health.ok) setHealthChart(health.data)
      else setError(health.message)
      if (metrics.ok) setMetricsChart(metrics.data)
      else if (health.ok) setError(metrics.message)
    } catch {
      setError("서버상황을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.")
    } finally {
      if (!soft) setLoading(false)
    }
  }, [rangeQuery])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace("/login")
      return
    }
    if (!canManageUsers(user.role)) {
      router.replace("/")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!allowed) return
    void load()
  }, [allowed, load])

  useEffect(() => {
    if (!allowed) return
    const id = window.setInterval(() => {
      void load({ soft: true })
    }, 30_000)
    return () => window.clearInterval(id)
  }, [allowed, load])

  async function onRefresh() {
    setRefreshing(true)
    setError(null)
    try {
      const result = await refreshAdminHealth()
      if (!result.ok) {
        setError(result.message)
        return
      }
      await load({ soft: true })
    } catch {
      setError("상태 새로고침에 실패했습니다.")
    } finally {
      setRefreshing(false)
    }
  }

  if (!ready) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
        <div className="border border-black bg-white p-8 text-center text-sm text-gray-500">
          권한을 확인하는 중…
        </div>
      </div>
    )
  }

  if (!allowed) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
        <div className="border border-black bg-white p-8 text-center text-sm text-gray-500">
          관리자 권한이 필요합니다.
        </div>
      </div>
    )
  }

  const metrics: MetricsSnapshot | undefined = overview?.metricsSnapshot
  const statuses = collectStatuses(overview)
  const critical = statuses.filter((s) => statusRank(s.snap.status) === "critical").length
  const warning = statuses.filter((s) => statusRank(s.snap.status) === "warning").length
  const okCount = statuses.filter((s) => statusRank(s.snap.status) === "ok").length
  const showSkeleton = loading && !overview

  const heapPct = metrics?.heapUsagePercent ?? null
  const loadAvg = metrics?.systemLoadAverage
  const cpuFromLoadAvg =
    loadAvg == null || !Number.isFinite(loadAvg) || loadAvg < 0 || !metrics?.processors
      ? null
      : Math.min(100, (loadAvg / Math.max(metrics.processors, 1)) * 100)
  const loadPct =
    metrics?.cpuUsagePercent != null && Number.isFinite(metrics.cpuUsagePercent)
      ? Math.max(0, Math.min(100, metrics.cpuUsagePercent))
      : cpuFromLoadAvg
  const threadPct =
    metrics?.threadCount == null ? null : Math.min(100, (metrics.threadCount / 200) * 100)

  const infoCards = [
    { label: "OS", value: "Windows / JVM" },
    { label: "CPU Core", value: metrics?.processors != null ? String(metrics.processors) : "-" },
    {
      label: "Heap",
      value:
        metrics != null
          ? `${Math.round(metrics.heapUsedMb)} / ${Math.round(metrics.heapMaxMb)} MB`
          : "-",
    },
    { label: "Threads", value: metrics?.threadCount != null ? String(metrics.threadCount) : "-" },
    {
      label: "CPU 사용률",
      value: loadPct != null ? `${Math.round(loadPct)}%` : "N/A",
    },
    {
      label: "DB",
      value: statusLabelKo(overview?.healthSnapshot.database?.status),
    },
    {
      label: "Redis",
      value: statusLabelKo(overview?.healthSnapshot.redis?.status),
    },
    {
      label: "회원",
      value: overview != null ? String(overview.totalUsers) : "-",
    },
  ]

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <div className="overflow-hidden border border-black bg-white">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-black bg-[#3d4654] px-5 py-3 text-white">
          <div>
            <h1 className="text-base font-medium tracking-tight">
              서버상황 <span className="text-[#7CFC00]">FinSight</span>
            </h1>
            <p className="mt-0.5 text-xs text-gray-300">
              헬스·메트릭 모니터링 ·{" "}
              <Link href="/admin/stats" className="text-[#7CFC00] underline-offset-2 hover:underline">
                전체 통계
              </Link>
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-xs text-gray-200">
            <span className="inline-flex items-center gap-1.5 bg-white/10 px-2.5 py-1">
              <span className="h-1.5 w-1.5 animate-pulse bg-[#7CFC00]" />
              30초 폴링
            </span>
            {updatedAt ? <span>갱신 {updatedAt}</span> : null}
            {loading && overview ? <span className="text-gray-300">갱신 중…</span> : null}
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="border border-white/30 px-2.5 py-1 hover:bg-white/10 disabled:opacity-50"
            >
              {loading ? "조회 중…" : "다시 조회"}
            </button>
            <button
              type="button"
              onClick={() => void onRefresh()}
              disabled={refreshing}
              className="border border-white/30 px-2.5 py-1 hover:bg-white/10 disabled:opacity-50"
            >
              {refreshing ? "재수집 중…" : "상태 새로고침"}
            </button>
          </div>
        </div>

        <div className="flex flex-wrap items-end gap-3 border-b border-black px-5 py-4">
          {(
            [
              ["daily", "일별"],
              ["weekly", "주별"],
              ["monthly", "월별"],
              ["custom", "기간별"],
            ] as const
          ).map(([key, label]) => (
            <button
              key={key}
              type="button"
              onClick={() => setPeriod(key)}
              className={
                period === key
                  ? "bg-finsight-primary px-3 py-1.5 text-sm font-medium text-white"
                  : "border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
              }
            >
              {label}
            </button>
          ))}
          {period === "custom" ? (
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <input
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                className="border border-gray-300 bg-white px-2 py-1.5 text-gray-800"
              />
              <span className="text-gray-400">~</span>
              <input
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                className="border border-gray-300 bg-white px-2 py-1.5 text-gray-800"
              />
              <button
                type="button"
                onClick={() => void load()}
                className="border border-finsight-primary px-3 py-1.5 text-finsight-primary hover:bg-finsight-primary/5"
              >
                검색
              </button>
            </div>
          ) : null}
        </div>

        <div className="relative space-y-6 px-5 py-6">
          {error ? (
            <div className="flex flex-wrap items-center justify-between gap-3 border border-black bg-red-50 px-4 py-3 text-sm text-red-700">
              <p role="alert">{error}</p>
              <button
                type="button"
                onClick={() => void load()}
                className="border border-black bg-white px-3 py-1.5 text-gray-800 hover:bg-gray-50"
              >
                다시 시도
              </button>
            </div>
          ) : null}

          {showSkeleton ? (
            <>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {Array.from({ length: 8 }).map((_, i) => (
                  <SkeletonBlock key={i} className="h-20 border border-black" />
                ))}
              </div>
              <div className="grid gap-3 sm:grid-cols-3">
                {Array.from({ length: 3 }).map((_, i) => (
                  <SkeletonBlock key={i} className="h-24 border border-black" />
                ))}
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {Array.from({ length: 4 }).map((_, i) => (
                  <SkeletonBlock key={i} className="h-36 border border-black" />
                ))}
              </div>
              <div className="grid gap-4 lg:grid-cols-2">
                <SkeletonBlock className="h-64 border border-black" />
                <SkeletonBlock className="h-64 border border-black" />
              </div>
              <p className="text-center text-sm text-gray-500">서버상황 데이터를 불러오는 중…</p>
            </>
          ) : (
            <>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {infoCards.map((card) => (
                  <div key={card.label} className="border border-black bg-gray-50 px-4 py-3">
                    <p className="text-xs text-gray-500">{card.label}</p>
                    <p className="mt-1 truncate text-lg font-semibold text-gray-900">{card.value}</p>
                  </div>
                ))}
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <div className="border border-black bg-red-600 px-4 py-5 text-center text-white">
                  <p className="text-sm font-medium opacity-90">위험</p>
                  <p className="mt-1 text-3xl font-bold">{critical}</p>
                </div>
                <div className="border border-black bg-amber-500 px-4 py-5 text-center text-white">
                  <p className="text-sm font-medium opacity-90">주의</p>
                  <p className="mt-1 text-3xl font-bold">{warning}</p>
                </div>
                <div className="border border-black bg-sky-600 px-4 py-5 text-center text-white">
                  <p className="text-sm font-medium opacity-90">정상</p>
                  <p className="mt-1 text-3xl font-bold">{okCount}</p>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <Gauge label="힙 사용률" percent={heapPct} sub="JVM Heap" />
                <Gauge
                  label="CPU 부하"
                  percent={loadPct}
                  sub={
                    loadPct != null
                      ? metrics?.cpuUsagePercent != null
                        ? `사용률 ${Math.round(loadPct)}%`
                        : loadAvg != null && loadAvg >= 0
                          ? `부하 ${loadAvg.toFixed(2)}`
                          : undefined
                      : "측정 불가"
                  }
                />
                <Gauge
                  label="스레드"
                  percent={threadPct}
                  sub={metrics?.threadCount != null ? `${metrics.threadCount}개` : undefined}
                />
                <Gauge
                  label="프로세서"
                  percent={
                    metrics?.processors != null ? Math.min(100, metrics.processors * 12.5) : null
                  }
                  sub={metrics?.processors != null ? `${metrics.processors}코어` : undefined}
                />
              </div>

              <div>
                <h2 className="mb-3 text-sm font-semibold text-gray-800">구성 요소 상태</h2>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  {statuses.map(({ name, snap }) => (
                    <div key={name} className={`border p-3 ${statusColor(snap.status)}`}>
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-medium">{name}</span>
                        <span className="text-xs font-bold tracking-wide">
                          {statusLabelKo(snap.status)}
                        </span>
                      </div>
                      {snap.message ? (
                        <p className="mt-2 text-xs opacity-90">{healthMessageKo(snap.message)}</p>
                      ) : null}
                    </div>
                  ))}
                </div>
              </div>

              <div className="grid gap-4 lg:grid-cols-2">
                <MiniLineChart
                  title="시스템 헬스 추이"
                  unit={healthChart?.unit ?? "건"}
                  series={healthChart?.series ?? []}
                  rangeLabel={rangeLabel}
                />
                <MiniLineChart
                  title="JVM 메트릭 추이"
                  unit={metricsChart?.unit ?? "%"}
                  series={metricsChart?.series ?? []}
                  rangeLabel={rangeLabel}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
