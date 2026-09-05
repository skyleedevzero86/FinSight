"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { AdminStatsBarChart, AdminStatsLineChart } from "@/components/admin/AdminStatsCharts"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchAdminStatsChart,
  fetchAdminStatsOverview,
  type AdminStatsChart,
  type AdminStatsChartKey,
  type AdminStatsOverview,
} from "@/lib/adminStats"

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
]

const PERIOD_OPTIONS = [
  { days: 7, label: "일간", color: "#ef4444" },
  { days: 30, label: "주간", color: "#f97316" },
  { days: 90, label: "월간", color: "#14b8a6" },
] as const

export default function AdminStatsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [tab, setTab] = useState<AdminStatsChartKey>("signups")
  const [days, setDays] = useState<number>(7)
  const [overview, setOverview] = useState<AdminStatsOverview | null>(null)
  const [chart, setChart] = useState<AdminStatsChart | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<string | null>(null)

  const activeTab = TABS.find((t) => t.key === tab) ?? TABS[0]

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

  if (!ready || !allowed) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-16 text-center text-gray-500">
        권한을 확인하는 중…
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="overflow-hidden border border-gray-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 bg-[#3d4654] px-5 py-3 text-white">
          <h1 className="text-base font-medium tracking-tight">
            관리자 통계 <span className="text-[#7CFC00]">FinSight</span>
          </h1>
          <div className="flex items-center gap-2 text-xs text-gray-200">
            <span className="inline-flex items-center gap-1.5 bg-white/10 px-2.5 py-1">
              <span className="h-1.5 w-1.5 animate-pulse bg-[#7CFC00]" />
              실시간 30초
            </span>
            {updatedAt ? <span>갱신 {updatedAt}</span> : null}
            <button
              type="button"
              onClick={() => void load()}
              disabled={loading}
              className="border border-white/30 px-2.5 py-1 hover:bg-white/10 disabled:opacity-50"
            >
              새로고침
            </button>
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
                  active ? "font-semibold text-[#03c75a]" : "text-gray-600 hover:text-gray-900"
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
                  className="inline-block h-3.5 w-3.5"
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
              <div key={String(label)} className="bg-gray-50 px-3 py-2">
                <div className="text-[11px] text-gray-500">{label}</div>
                <div className="text-lg font-semibold text-gray-900">{value}</div>
              </div>
            ))}
          </div>
        ) : null}

        <div className="px-3 py-4 sm:px-5">
          {error ? (
            <div className="mb-3 border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}
          {loading && !chart ? (
            <div className="py-24 text-center text-sm text-gray-500">차트를 불러오는 중…</div>
          ) : chart ? (
            <>
              <div className="mb-2 px-2 text-sm font-medium text-gray-800">{chart.title}</div>
              {activeTab.mode === "bar" ? (
                <AdminStatsBarChart series={chart.series} unit={chart.unit} />
              ) : (
                <AdminStatsLineChart series={chart.series} unit={chart.unit} />
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
