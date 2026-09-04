"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchAdminStatsChart,
  fetchAdminStatsOverview,
  refreshAdminHealth,
  type AdminStatsChart,
  type AdminStatsChartKey,
  type AdminStatsNamedSeries,
  type AdminStatsOverview,
} from "@/lib/adminStats"

const SERIES_COLORS = ["#3b82f6", "#03c75a", "#14b8a6", "#f97316", "#a855f7", "#ef4444"]

type TabDef = {
  key: AdminStatsChartKey
  label: string
  mode: "line" | "bar"
}

const TABS: TabDef[] = [
  { key: "signups", label: "신규 가입 / 탈퇴", mode: "line" },
  { key: "providers", label: "가입 경로별", mode: "bar" },
  { key: "logins", label: "활동 사용자", mode: "line" },
  { key: "cumulative", label: "누적 사용자", mode: "line" },
  { key: "status", label: "상태별 사용자", mode: "bar" },
  { key: "content", label: "게시글 / 댓글", mode: "line" },
  { key: "news", label: "뉴스 수집", mode: "line" },
  { key: "health", label: "시스템 헬스", mode: "line" },
  { key: "metrics", label: "JVM 메트릭", mode: "line" },
]

const PERIOD_OPTIONS = [
  { days: 7, label: "일간", color: "#ef4444" },
  { days: 30, label: "주간", color: "#f97316" },
  { days: 90, label: "월간", color: "#14b8a6" },
] as const

function formatDayLabel(iso: string): string {
  if (iso.length >= 10 && iso.includes("-")) {
    return iso.slice(0, 10).replace(/-/g, ".")
  }
  return iso
}

function statusTone(status: string | undefined): string {
  const s = (status ?? "").toUpperCase()
  if (s === "UP" || s === "HEALTHY") return "text-emerald-600"
  if (s === "DOWN" || s === "UNHEALTHY") return "text-red-600"
  return "text-amber-600"
}

function LineChart({
  series,
  unit,
}: {
  series: AdminStatsNamedSeries[]
  unit: string
}) {
  const width = 920
  const height = 360
  const padL = 56
  const padR = 140
  const padT = 24
  const padB = 48
  const plotW = width - padL - padR
  const plotH = height - padT - padB

  const labels = useMemo(() => {
    const first = series[0]?.points ?? []
    return first.map((p) => p.date)
  }, [series])

  const maxValue = useMemo(() => {
    let max = 0
    for (const s of series) {
      for (const p of s.points) max = Math.max(max, p.value)
    }
    return Math.max(10, Math.ceil(max / 10) * 10)
  }, [series])

  const yTicks = useMemo(() => {
    const steps = 10
    const step = maxValue / steps
    return Array.from({ length: steps + 1 }, (_, i) => Math.round(step * i))
  }, [maxValue])

  const xCount = Math.max(labels.length, 1)

  function xAt(index: number): number {
    if (xCount <= 1) return padL + plotW / 2
    return padL + (plotW * index) / (xCount - 1)
  }

  function yAt(value: number): number {
    return padT + plotH - (plotH * value) / maxValue
  }

  return (
    <div className="w-full overflow-x-auto">
      <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[720px] w-full h-auto">
        <text x={12} y={18} className="fill-gray-500" fontSize="12">
          {`단위 : ${unit}`}
        </text>
        {yTicks.map((tick) => {
          const y = yAt(tick)
          return (
            <g key={tick}>
              <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="#e5e7eb" strokeWidth="1" />
              <text x={padL - 10} y={y + 4} textAnchor="end" className="fill-gray-400" fontSize="11">
                {tick}
              </text>
            </g>
          )
        })}
        {labels.map((label, i) => (
          <text
            key={`${label}-${i}`}
            x={xAt(i)}
            y={height - 16}
            textAnchor="middle"
            className="fill-gray-500"
            fontSize="11"
          >
            {formatDayLabel(label)}
          </text>
        ))}
        {series.map((s, si) => {
          const color = SERIES_COLORS[si % SERIES_COLORS.length]
          const pts = s.points
            .map((p, i) => `${xAt(i)},${yAt(p.value)}`)
            .join(" ")
          return (
            <g key={s.name || s.label}>
              <polyline
                fill="none"
                stroke={color}
                strokeWidth="2.5"
                points={pts}
              />
              {s.points.map((p, i) => (
                <circle key={`${s.name}-${i}`} cx={xAt(i)} cy={yAt(p.value)} r="3.5" fill={color} />
              ))}
            </g>
          )
        })}
        {series.map((s, si) => {
          const color = SERIES_COLORS[si % SERIES_COLORS.length]
          const y = padT + 8 + si * 28
          return (
            <g key={`legend-${s.name}`}>
              <rect x={width - padR + 16} y={y} width="14" height="14" fill={color} rx="2" />
              <text x={width - padR + 36} y={y + 12} className="fill-gray-700" fontSize="13">
                {s.label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

function BarChart({
  series,
  unit,
}: {
  series: AdminStatsNamedSeries[]
  unit: string
}) {
  const width = 920
  const height = 360
  const padL = 56
  const padR = 40
  const padT = 24
  const padB = 56
  const plotW = width - padL - padR
  const plotH = height - padT - padB

  const bars = useMemo(
    () =>
      series.map((s) => ({
        label: s.label,
        value: s.points[0]?.value ?? 0,
      })),
    [series],
  )

  const maxValue = useMemo(() => {
    const max = bars.reduce((acc, b) => Math.max(acc, b.value), 0)
    return Math.max(10, Math.ceil(max / 10) * 10)
  }, [bars])

  const barGap = 24
  const barW = bars.length ? Math.min(72, (plotW - barGap * (bars.length + 1)) / bars.length) : 40

  return (
    <div className="w-full overflow-x-auto">
      <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[720px] w-full h-auto">
        <text x={12} y={18} className="fill-gray-500" fontSize="12">
          {`단위 : ${unit}`}
        </text>
        {Array.from({ length: 11 }, (_, i) => {
          const tick = Math.round((maxValue / 10) * i)
          const y = padT + plotH - (plotH * tick) / maxValue
          return (
            <g key={tick}>
              <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="#e5e7eb" strokeWidth="1" />
              <text x={padL - 10} y={y + 4} textAnchor="end" className="fill-gray-400" fontSize="11">
                {tick}
              </text>
            </g>
          )
        })}
        {bars.map((bar, i) => {
          const color = SERIES_COLORS[i % SERIES_COLORS.length]
          const x = padL + barGap + i * (barW + barGap)
          const h = (plotH * bar.value) / maxValue
          const y = padT + plotH - h
          return (
            <g key={bar.label}>
              <rect x={x} y={y} width={barW} height={Math.max(h, 1)} fill={color} rx="2" />
              <text
                x={x + barW / 2}
                y={height - 20}
                textAnchor="middle"
                className="fill-gray-600"
                fontSize="12"
              >
                {bar.label}
              </text>
              <text
                x={x + barW / 2}
                y={y - 8}
                textAnchor="middle"
                className="fill-gray-700"
                fontSize="12"
              >
                {bar.value}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

export default function AdminStatsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [tab, setTab] = useState<AdminStatsChartKey>("health")
  const [days, setDays] = useState<number>(7)
  const [overview, setOverview] = useState<AdminStatsOverview | null>(null)
  const [chart, setChart] = useState<AdminStatsChart | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [updatedAt, setUpdatedAt] = useState<string | null>(null)

  const activeTab = TABS.find((t) => t.key === tab) ?? TABS[0]
  const showOpsPanels = tab === "health" || tab === "metrics"

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const [overviewResult, chartResult] = await Promise.all([
      fetchAdminStatsOverview(),
      fetchAdminStatsChart(tab, { days }),
    ])
    setLoading(false)
    if (!overviewResult.ok) {
      setOverview(null)
      setError(overviewResult.message)
      return
    }
    if (!chartResult.ok) {
      setChart(null)
      setError(chartResult.message)
      return
    }
    setOverview(overviewResult.data)
    setChart(chartResult.data)
    setUpdatedAt(new Date().toLocaleString("ko-KR"))
  }, [tab, days])

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
      void load()
    }, 30_000)
    return () => window.clearInterval(id)
  }, [allowed, load])

  async function onRefreshHealth() {
    setRefreshing(true)
    setError(null)
    const result = await refreshAdminHealth()
    setRefreshing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    await load()
  }

  if (!ready || !allowed) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-16 text-center text-gray-500">
        권한을 확인하는 중…
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="overflow-hidden rounded border border-gray-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 bg-[#3d4654] px-5 py-3 text-white">
          <h1 className="text-base font-medium tracking-tight">
            시스템 헬스 · 관리자 통계{" "}
            <span className="text-[#7CFC00]">FinSight</span>
          </h1>
          <div className="flex items-center gap-2 text-xs text-gray-200">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-[#7CFC00]" />
              실시간 30초
            </span>
            {updatedAt ? <span>갱신 {updatedAt}</span> : null}
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="rounded border border-white/30 px-2.5 py-1 hover:bg-white/10 disabled:opacity-50"
            >
              새로고침
            </button>
            {showOpsPanels ? (
              <button
                type="button"
                onClick={() => void onRefreshHealth()}
                disabled={refreshing}
                className="rounded border border-white/30 px-2.5 py-1 hover:bg-white/10 disabled:opacity-50"
              >
                헬스 재수집
              </button>
            ) : null}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-b border-gray-200 px-5 py-3 text-sm">
          {TABS.map((item) => {
            const active = item.key === tab
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setTab(item.key)}
                className={
                  active
                    ? "font-semibold text-[#03c75a]"
                    : "text-gray-600 hover:text-gray-900"
                }
              >
                {item.label}
              </button>
            )
          })}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-4 border-b border-gray-100 px-5 py-3">
          {PERIOD_OPTIONS.map((option) => {
            const active = days === option.days
            return (
              <button
                key={option.days}
                type="button"
                onClick={() => setDays(option.days)}
                className={`inline-flex items-center gap-2 text-sm ${
                  active ? "font-semibold text-gray-900" : "text-gray-500 hover:text-gray-800"
                }`}
              >
                <span
                  className="inline-block h-3.5 w-3.5 rounded-sm"
                  style={{ backgroundColor: option.color, opacity: active ? 1 : 0.45 }}
                />
                {option.label}
              </button>
            )
          })}
        </div>

        {overview ? (
          <div className="grid grid-cols-2 gap-3 border-b border-gray-100 px-5 py-4 sm:grid-cols-4 lg:grid-cols-8">
            {[
              ["전체 회원", overview.totalUsers],
              ["정상", overview.approvedUsers],
              ["대기", overview.pendingUsers],
              ["정지", overview.suspendedUsers],
              ["탈퇴", overview.withdrawnUsers],
              ["게시글", overview.totalBoards],
              ["댓글", overview.totalComments],
              ["뉴스", overview.totalNews],
            ].map(([label, value]) => (
              <div key={String(label)} className="rounded bg-gray-50 px-3 py-2">
                <div className="text-[11px] text-gray-500">{label}</div>
                <div className="text-lg font-semibold text-gray-900">{value}</div>
              </div>
            ))}
          </div>
        ) : null}

        {tab === "health" && overview?.healthSnapshot ? (
          <div className="grid gap-3 border-b border-gray-100 px-5 py-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ["전체", overview.healthSnapshot.overall],
              ["DB", overview.healthSnapshot.database],
              ["Redis", overview.healthSnapshot.redis],
            ].map(([label, snap]) => {
              const status = snap?.status ?? "UNKNOWN"
              return (
                <div key={String(label)} className="rounded border border-gray-200 px-3 py-2">
                  <div className="text-xs text-gray-500">{label}</div>
                  <div className={`text-sm font-semibold ${statusTone(status)}`}>{status}</div>
                  <div className="mt-1 truncate text-xs text-gray-500">{snap?.message || "-"}</div>
                </div>
              )
            })}
            {overview.healthSnapshot.externalApis
              ? Object.entries(overview.healthSnapshot.externalApis).map(([name, snap]) => (
                  <div key={name} className="rounded border border-gray-200 px-3 py-2">
                    <div className="text-xs text-gray-500">{name}</div>
                    <div className={`text-sm font-semibold ${statusTone(snap.status)}`}>
                      {snap.status}
                    </div>
                    <div className="mt-1 truncate text-xs text-gray-500">{snap.message || "-"}</div>
                  </div>
                ))
              : null}
          </div>
        ) : null}

        {tab === "metrics" && overview?.metricsSnapshot ? (
          <div className="grid gap-3 border-b border-gray-100 px-5 py-4 sm:grid-cols-2 lg:grid-cols-5">
            {[
              ["힙 사용", `${overview.metricsSnapshot.heapUsedMb} / ${overview.metricsSnapshot.heapMaxMb} MB`],
              ["힙 사용률", `${overview.metricsSnapshot.heapUsagePercent}%`],
              ["스레드", String(overview.metricsSnapshot.threadCount)],
              ["프로세서", String(overview.metricsSnapshot.processors)],
              [
                "시스템 로드",
                Number.isFinite(overview.metricsSnapshot.systemLoadAverage)
                  ? overview.metricsSnapshot.systemLoadAverage.toFixed(2)
                  : "-",
              ],
            ].map(([label, value]) => (
              <div key={String(label)} className="rounded border border-gray-200 px-3 py-2">
                <div className="text-xs text-gray-500">{label}</div>
                <div className="text-sm font-semibold text-gray-900">{value}</div>
              </div>
            ))}
          </div>
        ) : null}

        <div className="px-3 py-4 sm:px-5">
          {error ? (
            <div className="mb-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}
          {loading && !chart ? (
            <div className="py-24 text-center text-sm text-gray-500">차트를 불러오는 중…</div>
          ) : chart ? (
            <>
              <div className="mb-2 px-2 text-sm font-medium text-gray-800">{chart.title}</div>
              {activeTab.mode === "bar" ? (
                <BarChart series={chart.series} unit={chart.unit} />
              ) : (
                <LineChart series={chart.series} unit={chart.unit} />
              )}
            </>
          ) : (
            <div className="py-24 text-center text-sm text-gray-500">표시할 데이터가 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  )
}
