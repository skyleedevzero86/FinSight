"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import {
  Eye,
  Flag,
  ImageIcon,
  Link2,
  Mail,
  PanelTop,
  ShieldAlert,
  Users,
  type LucideIcon,
} from "lucide-react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  loadAdminOpsCore,
  loadAdminOpsExtras,
  type AdminOpsHomeData,
  type OpsLogLine,
  type OpsServerRow,
  type OpsSlice,
  type OpsTask,
  type OpsTrendSeries,
} from "@/lib/adminOpsHome"
import { formatKoreanDate, type MyInfoNotice } from "@/lib/myInfoHome"

const EMPTY: AdminOpsHomeData = {
  serviceOk: false,
  serviceLabel: "상태를 불러오는 중입니다.",
  servers: [],
  logs: [],
  totalUsers: 0,
  todaySignups: 0,
  todayLogins: 0,
  loginDeltaPercent: null,
  openReports: 0,
  urgentReports: 0,
  mailFailures: 0,
  mailSuccessPercent: null,
  mainimgLive: 0,
  popupLive: 0,
  ulinkCount: 0,
  mailTodayLabel: "오늘 0건",
  notices: [],
  trendLabels: [],
  trend: [],
  memberSlices: [],
  tasks: [],
}

function countText(value: number): string {
  return value.toLocaleString("ko-KR")
}

function Panel({
  title,
  href,
  action,
  children,
}: {
  title: string
  href?: string
  action?: string
  children: React.ReactNode
}) {
  return (
    <section className="flex flex-col border border-[#e7edf5] bg-white px-4 py-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-[15px] font-bold text-slate-800">{title}</h2>
        {href && action ? (
          <Link href={href} className="shrink-0 text-xs text-slate-400 hover:text-finsight-primary">
            {action} <span aria-hidden>›</span>
          </Link>
        ) : null}
      </div>
      {children}
    </section>
  )
}

function Meter({ label, value }: { label: string; value: number | null }) {
  if (value == null) {
    return <p className="text-[11px] text-slate-400">{label} 측정값 없음</p>
  }
  const width = Math.max(0, Math.min(100, Math.round(value)))
  const tone = width >= 85 ? "bg-red-500" : width >= 60 ? "bg-amber-400" : "bg-emerald-500"
  return (
    <p className="flex items-center gap-2 text-[11px] text-slate-500">
      <span className="w-10 shrink-0">{label}</span>
      <span className="h-1.5 min-w-0 flex-1 bg-slate-100">
        <span className={`block h-full ${tone}`} style={{ width: `${width}%` }} />
      </span>
      <span className="w-8 shrink-0 text-right">{width}%</span>
    </p>
  )
}

function ServerList({ rows }: { rows: OpsServerRow[] }) {
  if (!rows.length) return <p className="py-8 text-center text-sm text-slate-400">서버 상태를 불러오지 못했습니다.</p>
  return (
    <ul className="flex flex-col gap-3">
      {rows.map((row) => (
        <li key={row.name} className="flex items-start gap-2">
          <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${row.ok ? "bg-emerald-500" : "bg-red-500"}`} />
          <span className="min-w-0 flex-1">
            <span className="flex items-center justify-between gap-2 text-[13px] font-semibold text-slate-800">
              {row.name}
              <span className={row.ok ? "text-[11px] font-medium text-emerald-600" : "text-[11px] font-medium text-red-600"}>
                {row.statusLabel}
              </span>
            </span>
            {row.cpu != null || row.memory != null ? (
              <span className="mt-1 flex flex-col gap-1">
                {row.cpu != null ? <Meter label="CPU" value={row.cpu} /> : null}
                {row.memory != null ? <Meter label="메모리" value={row.memory} /> : null}
              </span>
            ) : (
              <span className="mt-1 block text-[11px] text-slate-500">{row.detail || "상태만 확인됩니다."}</span>
            )}
          </span>
        </li>
      ))}
    </ul>
  )
}

function logTone(level: OpsLogLine["level"]): string {
  if (level === "ERROR") return "text-red-600"
  if (level === "WARN") return "text-amber-600"
  return "text-sky-600"
}

function LogList({ items }: { items: OpsLogLine[] }) {
  if (!items.length) return <p className="py-8 text-center text-sm text-slate-400">최근 기록이 없습니다.</p>
  return (
    <ul className="flex flex-col gap-2.5">
      {items.map((item, index) => (
        <li key={`${item.level}-${index}`} className="flex items-start gap-2 text-[12px]">
          <span className={`shrink-0 font-semibold ${logTone(item.level)}`}>[{item.level}]</span>
          <span className="min-w-0 flex-1 leading-snug text-slate-700">{item.message}</span>
          <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
        </li>
      ))}
    </ul>
  )
}

function Greeting({ serviceOk, serviceLabel }: { serviceOk: boolean; serviceLabel: string }) {
  const { user } = useAuthSession()
  const name = user?.nickname || "관리자"
  return (
    <div className="flex flex-1 items-center justify-between gap-3 border border-[#e7edf5] bg-white px-4 py-4">
      <div className="min-w-0">
        <p className="truncate text-base font-bold text-slate-900">안녕하세요, {name}님</p>
        <p className="mt-1 text-[13px] text-slate-500">FinSight 서비스 운영 현황을 확인하세요.</p>
      </div>
      <div className="shrink-0 text-right">
        <p className="text-[11px] text-slate-400">{formatKoreanDate(new Date())}</p>
        <p className={`mt-2 inline-block px-2 py-1 text-[11px] font-semibold ${serviceOk ? "bg-emerald-50 text-emerald-600" : "bg-amber-50 text-amber-700"}`}>
          {serviceLabel}
        </p>
      </div>
    </div>
  )
}

function StatCard({
  href,
  icon: Icon,
  iconClass,
  label,
  value,
  sub,
}: {
  href: string
  icon: LucideIcon
  iconClass: string
  label: string
  value: string
  sub: string
}) {
  return (
    <Link href={href} className="flex h-full items-center gap-3 border border-[#e7edf5] bg-white px-3 py-3">
      <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${iconClass}`}>
        <Icon className="h-4 w-4" aria-hidden />
      </span>
      <span className="min-w-0">
        <span className="block truncate text-[11px] text-slate-500">{label}</span>
        <span className="block truncate text-sm font-bold text-slate-900">{value}</span>
        <span className="block truncate text-[11px] text-rose-500">{sub}</span>
      </span>
    </Link>
  )
}

function Shortcut({
  href,
  icon: Icon,
  iconClass,
  label,
  detail,
}: {
  href: string
  icon: LucideIcon
  iconClass: string
  label: string
  detail: string
}) {
  return (
    <Link href={href} className="flex items-center gap-3 rounded-xl px-1 py-2 hover:bg-slate-50">
      <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-white ${iconClass}`}>
        <Icon className="h-4 w-4" aria-hidden />
      </span>
      <span className="min-w-0">
        <span className="block truncate text-[13px] font-semibold text-slate-800">{label}</span>
        <span className="block truncate text-[11px] text-slate-400">{detail}</span>
      </span>
    </Link>
  )
}

function NoticeList({ items }: { items: MyInfoNotice[] }) {
  if (!items.length) return <p className="py-8 text-center text-sm text-slate-400">공지가 없습니다.</p>
  return (
    <ul className="flex flex-col gap-2.5">
      {items.map((item) => (
        <li key={item.id}>
          <Link href={item.href} className="flex items-start gap-2 text-[13px] text-slate-700 hover:text-finsight-primary">
            <Flag className="mt-0.5 h-3.5 w-3.5 shrink-0 text-rose-500" aria-hidden />
            <span className="min-w-0 flex-1 truncate">{item.title}</span>
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function TrendChart({ labels, series }: { labels: string[]; series: OpsTrendSeries[] }) {
  const width = 320
  const height = 150
  const padX = 16
  const padY = 12
  const max = Math.max(1, ...series.flatMap((item) => item.points))
  const plotW = width - padX * 2
  const plotH = height - padY * 2
  return (
    <div>
      <div className="mb-2 flex flex-wrap gap-3 text-[11px] text-slate-500">
        {series.map((item) => (
          <span key={item.name} className="inline-flex items-center gap-1">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: item.color }} />
            {item.name}
          </span>
        ))}
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} className="h-36 w-full" role="img" aria-label="최근 7일 사용자 추이">
        {series.map((item) => {
          if (item.points.length < 2) return null
          const step = plotW / (item.points.length - 1)
          const d = item.points
            .map((value, index) => {
              const x = padX + step * index
              const y = padY + plotH - (value / max) * plotH
              return `${index === 0 ? "M" : "L"}${x.toFixed(1)} ${y.toFixed(1)}`
            })
            .join(" ")
          return <path key={item.name} d={d} fill="none" stroke={item.color} strokeWidth="2" />
        })}
      </svg>
      <div className="mt-1 flex justify-between text-[10px] text-slate-400">
        {labels.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
    </div>
  )
}

function MemberDonut({ total, slices }: { total: number; slices: OpsSlice[] }) {
  const radius = 38
  const circumference = 2 * Math.PI * radius
  let cursor = 0
  const sum = slices.reduce((acc, slice) => acc + slice.value, 0)
  return (
    <div className="flex flex-col items-center gap-3 sm:flex-row">
      <svg viewBox="0 0 120 120" className="h-36 w-36 shrink-0" role="img" aria-label="회원 현황">
        <g transform="rotate(-90 60 60)">
          <circle cx="60" cy="60" r={radius} fill="none" stroke="#e2e8f0" strokeWidth="14" />
          {sum > 0
            ? slices.map((slice) => {
                const length = (slice.value / sum) * circumference
                const node = (
                  <circle
                    key={slice.label}
                    cx="60"
                    cy="60"
                    r={radius}
                    fill="none"
                    stroke={slice.color}
                    strokeWidth="14"
                    strokeDasharray={`${length} ${circumference - length}`}
                    strokeDashoffset={-cursor}
                  />
                )
                cursor += length
                return node
              })
            : null}
        </g>
        <text x="60" y="56" textAnchor="middle" fontSize="9" fill="#94a3b8">
          전체회원
        </text>
        <text x="60" y="74" textAnchor="middle" fontSize="12" fontWeight="700" fill="#0f172a">
          {countText(total)}명
        </text>
      </svg>
      <ul className="flex flex-col gap-1 text-[12px] text-slate-600">
        {slices.length ? slices.map((slice) => (
          <li key={slice.label} className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full" style={{ backgroundColor: slice.color }} />
            <span>{slice.label}</span>
            <span className="text-slate-400">
              {countText(slice.value)}명 ({sum ? Math.round((slice.value / sum) * 1000) / 10 : 0}%)
            </span>
          </li>
        )) : <li>회원 상태가 없습니다.</li>}
      </ul>
    </div>
  )
}

const TASK_LIST_MIN_HEIGHT = "min-h-[8.75rem]"

function TaskList({ items }: { items: OpsTask[] }) {
  if (!items.length) {
    return (
      <p className={`flex ${TASK_LIST_MIN_HEIGHT} items-center justify-center text-sm text-slate-400`}>
        지금 처리할 일이 없습니다.
      </p>
    )
  }
  return (
    <ul className={`grid ${TASK_LIST_MIN_HEIGHT} content-start gap-2 md:grid-cols-2`}>
      {items.map((item) => (
        <li key={item.key}>
          <Link href={item.href} className="flex items-center gap-3 border border-[#e7edf5] px-3 py-2.5 hover:bg-slate-50">
            <span className={`shrink-0 px-1.5 py-0.5 text-[11px] font-semibold ${item.urgent ? "bg-red-50 text-red-600" : "bg-amber-50 text-amber-700"}`}>
              {item.label}
            </span>
            <span className="min-w-0 flex-1 truncate text-[13px] font-medium text-slate-800">{item.title}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function loginSub(delta: number | null): string {
  if (delta == null) return "전일 로그인 없음"
  const shown = Math.max(0, delta)
  const sign = shown > 0 ? "+" : ""
  return `전일 대비 ${sign}${shown}%`
}

function signupSub(count: number): string {
  if (count <= 0) return "오늘 신규 0건"
  return `오늘 신규 +${countText(count)}건`
}

export default function AdminOpsHomeClient() {
  const [data, setData] = useState<AdminOpsHomeData>(EMPTY)
  const [coreReady, setCoreReady] = useState(false)
  const [detailReady, setDetailReady] = useState(false)

  useEffect(() => {
    let alive = true
    void loadAdminOpsCore()
      .then((core) => {
        if (!alive) return
        setData(core)
        setCoreReady(true)
        if (!core.servers.length) {
          setDetailReady(true)
          return
        }
        return loadAdminOpsExtras(core)
      })
      .then((full) => {
        if (!alive) return
        if (full) setData(full)
        setDetailReady(true)
      })
      .catch((error: unknown) => {
        console.error("운영 현황을 불러오지 못했습니다.", error)
        if (!alive) return
        setCoreReady(true)
        setDetailReady(true)
      })
    return () => {
      alive = false
    }
  }, [])

  const mailSub = data.mailSuccessPercent == null
    ? "성공률 계산 전"
    : `성공률 ${data.mailSuccessPercent}%`

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:px-5 md:py-5">
      <div className="grid items-stretch gap-4 xl:grid-cols-2">
        <div className="grid gap-4 md:grid-cols-2">
          <Panel title="서버 현황" href="/admin/health" action="상세보기">
            {!coreReady ? (
              <p className="py-8 text-center text-sm text-slate-400">서버 상태를 불러오는 중입니다.</p>
            ) : (
              <ServerList rows={data.servers} />
            )}
          </Panel>
          <Panel title="최근 시스템 로그" href="/admin/stats" action="상세보기">
            {!coreReady ? (
              <p className="py-8 text-center text-sm text-slate-400">로그를 불러오는 중입니다.</p>
            ) : data.logs.length ? (
              <LogList items={data.logs} />
            ) : (
              <p className="py-8 text-center text-sm text-slate-400">{data.serviceLabel}</p>
            )}
          </Panel>
        </div>
        <div className="flex h-full flex-col gap-3">
          <Greeting serviceOk={data.serviceOk} serviceLabel={data.serviceLabel} />
          <div className="grid min-h-0 flex-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard href="/admin/users" icon={Users} iconClass="bg-sky-100 text-sky-600" label="전체 회원수" value={`${countText(data.totalUsers)}명`} sub={signupSub(data.todaySignups)} />
            <StatCard href="/admin/stats" icon={Eye} iconClass="bg-violet-100 text-violet-600" label="오늘 접속자" value={`${countText(data.todayLogins)}명`} sub={loginSub(data.loginDeltaPercent)} />
            <StatCard href="/admin/moderation" icon={ShieldAlert} iconClass="bg-rose-100 text-rose-600" label="미처리 신고" value={`${countText(data.openReports)}건`} sub={`긴급 ${countText(data.urgentReports)}건`} />
            <StatCard href="/admin/email-logs" icon={Mail} iconClass="bg-amber-100 text-amber-600" label="메일 발송 실패" value={`${countText(data.mailFailures)}건`} sub={mailSub} />
          </div>
        </div>
      </div>

      <Panel title="오늘 처리할 일">
        {detailReady ? <TaskList items={data.tasks} /> : <p className={`flex ${TASK_LIST_MIN_HEIGHT} items-center justify-center text-sm text-slate-400`}>처리할 일을 불러오는 중입니다.</p>}
      </Panel>

      <div className="grid items-stretch gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,0.9fr)_minmax(0,1.2fr)]">
        <Panel title="주요 관리 바로가기">
          <div className="grid grid-cols-1 gap-1 sm:grid-cols-2">
            <Shortcut href="/admin/mainimg" icon={ImageIcon} iconClass="bg-violet-500" label="메인이미지 관리" detail={`현재 ${countText(data.mainimgLive)}개 노출중`} />
            <Shortcut href="/admin/popup" icon={PanelTop} iconClass="bg-orange-500" label="팝업 관리" detail={`현재 ${countText(data.popupLive)}개 노출중`} />
            <Shortcut href="/admin/ulink" icon={Link2} iconClass="bg-sky-500" label="유용한링크 관리" detail={`등록 ${countText(data.ulinkCount)}개`} />
            <Shortcut href="/admin/users" icon={Users} iconClass="bg-emerald-500" label="사용자 관리" detail={`전체 ${countText(data.totalUsers)}명`} />
            <Shortcut href="/admin/moderation" icon={Flag} iconClass="bg-rose-500" label="신고 관리" detail={`미처리 ${countText(data.openReports)}건`} />
            <Shortcut href="/admin/email-logs" icon={Mail} iconClass="bg-fuchsia-500" label="메일 이력" detail={data.mailTodayLabel} />
          </div>
        </Panel>
        <Panel title="공지사항 / 운영 알림" href="/community/notice" action="전체보기">
          {detailReady ? <NoticeList items={data.notices} /> : <p className="py-8 text-center text-sm text-slate-400">공지를 불러오는 중입니다.</p>}
        </Panel>
        <div className="grid gap-4 md:grid-cols-2">
          <Panel title="최근 7일 사용자 추이" href="/admin/stats" action="전체보기">
            {!coreReady ? (
              <p className="py-8 text-center text-sm text-slate-400">추이를 불러오는 중입니다.</p>
            ) : data.trendLabels.length < 2 ? (
              <p className="py-8 text-center text-sm text-slate-400">{data.serviceLabel}</p>
            ) : (
              <TrendChart labels={data.trendLabels} series={data.trend} />
            )}
          </Panel>
          <Panel title="회원 현황" href="/admin/users" action="전체보기">
            {!coreReady ? (
              <p className="py-8 text-center text-sm text-slate-400">회원 현황을 불러오는 중입니다.</p>
            ) : data.servers.length || data.totalUsers > 0 || data.memberSlices.length ? (
              <MemberDonut total={data.totalUsers} slices={data.memberSlices} />
            ) : (
              <p className="py-8 text-center text-sm text-slate-400">{data.serviceLabel}</p>
            )}
          </Panel>
        </div>
      </div>
    </div>
  )
}
