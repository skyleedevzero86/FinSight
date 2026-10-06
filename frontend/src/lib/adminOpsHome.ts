import { fetchAdminEmailLogs, type AdminEmailLog, type EmailStatus } from "@/lib/adminEmailLogs"
import {
  fetchAdminOpsHome,
  type AdminStatsChart,
  type AdminStatsOverview,
} from "@/lib/adminStats"
import { authHeadersJson } from "@/lib/finsightToken"
import { fetchAdminMainimgItems } from "@/lib/mainimg"
import { loadNoticeCards, type MyInfoNotice } from "@/lib/myInfoHome"
import { fetchAdminPopupItems } from "@/lib/popup"
import { fetchAdminUlinkItems } from "@/lib/ulink"

export type OpsServerRow = {
  name: string
  ok: boolean
  statusLabel: string
  detail: string
  cpu: number | null
  memory: number | null
}

export type OpsLogLine = {
  level: "INFO" | "WARN" | "ERROR"
  message: string
  timeLabel: string
}

export type OpsSlice = {
  label: string
  value: number
  color: string
}

export type OpsTrendSeries = {
  name: string
  color: string
  points: number[]
}

export type AdminOpsHomeData = {
  serviceOk: boolean
  serviceLabel: string
  servers: OpsServerRow[]
  logs: OpsLogLine[]
  totalUsers: number
  todaySignups: number
  todayLogins: number
  loginDeltaPercent: number | null
  openReports: number
  urgentReports: number
  mailFailures: number
  mailSuccessPercent: number | null
  mainimgLive: number
  popupLive: number
  ulinkCount: number
  mailTodayLabel: string
  notices: MyInfoNotice[]
  trendLabels: string[]
  trend: OpsTrendSeries[]
  memberSlices: OpsSlice[]
}

type ReportHit = {
  title: string
  reportCount: number
  createdAt: string | null
}

const URGENT_REPORTS = 5

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readText(value: unknown): string {
  return typeof value === "string" ? value.trim() : ""
}

function isUp(status: string | undefined): boolean {
  const normalized = (status ?? "").toUpperCase()
  return normalized === "UP" || normalized === "HEALTHY"
}

function statusLabel(status: string | undefined): string {
  if (!status) return "확인불가"
  return isUp(status) ? "정상" : "점검"
}

function clockLabel(iso: string | null | undefined): string {
  if (!iso) return ""
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ""
  const hours = String(date.getHours()).padStart(2, "0")
  const minutes = String(date.getMinutes()).padStart(2, "0")
  return `${hours}:${minutes}`
}

function clockNow(timestamp: number | undefined): string {
  const date = timestamp && timestamp > 0 ? new Date(timestamp) : new Date()
  return clockLabel(date.toISOString())
}

function seriesPoints(chart: AdminStatsChart | null, name: string): number[] {
  return chart?.series.find((item) => item.name === name)?.points.map((point) => point.value) ?? []
}

function pointAt(values: number[], indexFromEnd: number): number {
  const index = values.length - 1 - indexFromEnd
  return index >= 0 ? values[index] : 0
}

function deltaPercent(current: number, previous: number): number | null {
  if (previous <= 0) return null
  return Math.round(((current - previous) / previous) * 100)
}

function dayLabel(iso: string): string {
  return iso.length >= 10 ? `${iso.slice(5, 7)}/${iso.slice(8, 10)}` : iso
}

function todayKey(): string {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

async function emailTotal(status: EmailStatus | ""): Promise<number> {
  const result = await fetchAdminEmailLogs({
    page: 0,
    size: 1,
    keyword: "",
    status,
    purpose: "",
    actorType: "",
    requestIp: "",
  })
  return result.ok ? result.data.totalElements : 0
}

async function recentFailedMails(): Promise<AdminEmailLog[]> {
  const result = await fetchAdminEmailLogs({
    page: 0,
    size: 5,
    keyword: "",
    status: "FAILED",
    purpose: "",
    actorType: "",
    requestIp: "",
  })
  return result.ok ? result.data.content : []
}

async function mailTodayLabel(): Promise<string> {
  const result = await fetchAdminEmailLogs({
    page: 0,
    size: 40,
    keyword: "",
    status: "",
    purpose: "",
    actorType: "",
    requestIp: "",
  })
  if (!result.ok) return "오늘 0건"
  const today = todayKey()
  const count = result.data.content.filter((item) => (item.createdAt ?? "").startsWith(today)).length
  const more = result.data.hasNext && count === result.data.content.length
  return more ? `오늘 ${count}건 이상` : `오늘 ${count}건`
}

async function reportHits(path: string): Promise<ReportHit[]> {
  try {
    const res = await fetch(path, { headers: authHeadersJson(), cache: "no-store" })
    if (!res.ok) return []
    const root = asRecord(await res.json())
    const rows = Array.isArray(root?.data) ? root.data : []
    return rows.flatMap((raw) => {
      const row = asRecord(raw)
      const title = readText(row?.title)
      if (!title) return []
      return [{
        title,
        reportCount: Number(row?.reportCount) || 0,
        createdAt: readText(row?.createdAt) || null,
      }]
    })
  } catch (error) {
    console.error("신고 후보를 불러오지 못했습니다.", path, error)
    return []
  }
}

function serverRows(overview: AdminStatsOverview | null): OpsServerRow[] {
  if (!overview) return []
  const health = overview.healthSnapshot
  const metrics = overview.metricsSnapshot
  const cpu = metrics?.cpuUsagePercent ?? null
  const memory = metrics ? Math.round(metrics.heapUsagePercent) : null
  return [
    {
      name: "Spring API",
      ok: isUp(health?.overall?.status),
      statusLabel: statusLabel(health?.overall?.status),
      detail: health?.overall?.message || "",
      cpu,
      memory,
    },
    {
      name: "데이터베이스",
      ok: isUp(health?.database?.status),
      statusLabel: statusLabel(health?.database?.status),
      detail: health?.database?.message || "",
      cpu: null,
      memory: null,
    },
    {
      name: "Redis",
      ok: isUp(health?.redis?.status),
      statusLabel: statusLabel(health?.redis?.status),
      detail: health?.redis?.message || "",
      cpu: null,
      memory: null,
    },
  ]
}

function healthLog(status: string | undefined, message: string | undefined, time: string): OpsLogLine {
  return {
    level: isUp(status) ? "INFO" : "ERROR",
    message: message || "상태를 확인하지 못했습니다.",
    timeLabel: time,
  }
}

function buildLogs(
  overview: AdminStatsOverview | null,
  mails: AdminEmailLog[],
  reports: ReportHit[],
): OpsLogLine[] {
  const time = clockNow(overview?.metricsSnapshot?.timestamp)
  const health = overview?.healthSnapshot
  const healthLines: OpsLogLine[] = [
    healthLog(health?.overall?.status, health?.overall?.message, time),
    healthLog(health?.database?.status, health?.database?.message ? `데이터베이스: ${health.database.message}` : undefined, time),
    healthLog(health?.redis?.status, health?.redis?.message ? `Redis: ${health.redis.message}` : undefined, time),
  ]
  const mailLines: OpsLogLine[] = mails.slice(0, 3).map((item) => ({
    level: "ERROR",
    message: item.errorMessage || `메일 발송 실패: ${item.subject}`,
    timeLabel: clockLabel(item.createdAt),
  }))
  const reportLines: OpsLogLine[] = reports.slice(0, 3).map((item) => ({
    level: item.reportCount >= URGENT_REPORTS ? "ERROR" : "WARN",
    message: `신고 ${item.reportCount}회 · ${item.title}`,
    timeLabel: clockLabel(item.createdAt),
  }))
  return [...healthLines, ...mailLines, ...reportLines].slice(0, 5)
}

function memberSlices(overview: AdminStatsOverview | null): OpsSlice[] {
  if (!overview) return []
  return [
    { label: "승인", value: overview.approvedUsers, color: "#3b82f6" },
    { label: "승인대기", value: overview.pendingUsers, color: "#94a3b8" },
    { label: "정지", value: overview.suspendedUsers, color: "#f97316" },
    { label: "거부", value: overview.totalUsers - overview.approvedUsers - overview.pendingUsers - overview.suspendedUsers - overview.withdrawnUsers, color: "#a855f7" },
    { label: "탈퇴", value: overview.withdrawnUsers, color: "#ef4444" },
  ].filter((slice) => slice.value > 0)
}

function trendOf(
  signups: AdminStatsChart | null,
  logins: AdminStatsChart | null,
  content: AdminStatsChart | null,
): { labels: string[]; series: OpsTrendSeries[] } {
  const dates = signups?.series.find((item) => item.name === "signups")?.points.map((point) => point.date)
    ?? logins?.series[0]?.points.map((point) => point.date)
    ?? []
  return {
    labels: dates.map(dayLabel),
    series: [
      { name: "신규 회원", color: "#3b82f6", points: seriesPoints(signups, "signups") },
      { name: "접속자", color: "#8b5cf6", points: seriesPoints(logins, "logins") },
      { name: "게시글", color: "#14b8a6", points: seriesPoints(content, "boards") },
    ],
  }
}

function successPercent(sent: number, failed: number): number | null {
  const total = sent + failed
  if (total <= 0) return null
  return Math.round((sent / total) * 1000) / 10
}

function serviceLabel(overview: AdminStatsOverview | null): string {
  if (!overview) return "상태를 불러오지 못했습니다."
  return isUp(overview.healthSnapshot.overall?.status)
    ? "전체 서비스 정상 운영"
    : "일부 서비스 점검 필요"
}

async function countOf(
  loader: () => Promise<{ ok: true; data: { totalElements: number } } | { ok: false; message: string }>,
): Promise<number> {
  const result = await loader()
  return result.ok ? result.data.totalElements : 0
}

type OpsBundle = {
  overview: AdminStatsOverview | null
  signups: AdminStatsChart | null
  logins: AdminStatsChart | null
  content: AdminStatsChart | null
  notices: MyInfoNotice[]
  mainimgLive: number
  popupLive: number
  ulinkCount: number
  failedMails: AdminEmailLog[]
  sentTotal: number
  failedTotal: number
  mailToday: string
  reports: ReportHit[]
}

function toOpsHome(bundle: OpsBundle): AdminOpsHomeData {
  const signupValues = seriesPoints(bundle.signups, "signups")
  const loginValues = seriesPoints(bundle.logins, "logins")
  const trend = trendOf(bundle.signups, bundle.logins, bundle.content)
  return {
    serviceOk: Boolean(bundle.overview) && isUp(bundle.overview?.healthSnapshot.overall?.status),
    serviceLabel: serviceLabel(bundle.overview),
    servers: serverRows(bundle.overview),
    logs: buildLogs(bundle.overview, bundle.failedMails, bundle.reports),
    totalUsers: bundle.overview?.totalUsers ?? 0,
    todaySignups: pointAt(signupValues, 0),
    todayLogins: pointAt(loginValues, 0),
    loginDeltaPercent: deltaPercent(pointAt(loginValues, 0), pointAt(loginValues, 1)),
    openReports: bundle.reports.length,
    urgentReports: bundle.reports.filter((item) => item.reportCount >= URGENT_REPORTS).length,
    mailFailures: bundle.failedTotal,
    mailSuccessPercent: successPercent(bundle.sentTotal, bundle.failedTotal),
    mainimgLive: bundle.mainimgLive,
    popupLive: bundle.popupLive,
    ulinkCount: bundle.ulinkCount,
    mailTodayLabel: bundle.mailToday,
    notices: bundle.notices,
    trendLabels: trend.labels,
    trend: trend.series,
    memberSlices: memberSlices(bundle.overview),
  }
}

const EMPTY_EXTRAS = {
  notices: [] as MyInfoNotice[],
  mainimgLive: 0,
  popupLive: 0,
  ulinkCount: 0,
  failedMails: [] as AdminEmailLog[],
  sentTotal: 0,
  failedTotal: 0,
  mailToday: "오늘 0건",
  reports: [] as ReportHit[],
}

let coreFlight: Promise<AdminOpsHomeData> | null = null

function emptyOpsHome(message: string): AdminOpsHomeData {
  return {
    ...toOpsHome({
      overview: null,
      signups: null,
      logins: null,
      content: null,
      ...EMPTY_EXTRAS,
    }),
    serviceLabel: message,
  }
}

async function fetchOpsCore(): Promise<AdminOpsHomeData> {
  const packed = await fetchAdminOpsHome()
  if (packed.ok) {
    return toOpsHome({
      overview: packed.data.overview,
      signups: packed.data.signups,
      logins: packed.data.logins,
      content: packed.data.content,
      ...EMPTY_EXTRAS,
    })
  }
  console.error("운영 현황을 불러오지 못했습니다.", packed.message)
  return emptyOpsHome(packed.message)
}

export function loadAdminOpsCore(): Promise<AdminOpsHomeData> {
  if (!coreFlight) {
    coreFlight = fetchOpsCore().finally(() => {
      coreFlight = null
    })
  }
  return coreFlight
}

export async function loadAdminOpsExtras(base: AdminOpsHomeData): Promise<AdminOpsHomeData> {
  const [notices, mainimgLive, popupLive, ulinkCount, failedMails, sentTotal, failedTotal, mailToday, boardReports, commentReports] =
    await Promise.all([
      loadNoticeCards(5),
      countOf(() => fetchAdminMainimgItems({ page: 0, size: 1, reflectOnly: true })),
      countOf(() => fetchAdminPopupItems({ page: 0, size: 1, activeOnly: true })),
      countOf(() => fetchAdminUlinkItems({ page: 0, size: 1 })),
      recentFailedMails(),
      emailTotal("SENT"),
      emailTotal("FAILED"),
      mailTodayLabel(),
      reportHits("/api/v1/admin/boards/maintenance/candidates?reportThreshold=1"),
      reportHits("/api/v1/admin/comments/maintenance/candidates?reportThreshold=1"),
    ])
  const reports = [...boardReports, ...commentReports]
  const extraLogs = [
    ...failedMails.slice(0, 3).map((item) => ({
      level: "ERROR" as const,
      message: item.errorMessage || `메일 발송 실패: ${item.subject}`,
      timeLabel: clockLabel(item.createdAt),
    })),
    ...reports.slice(0, 3).map((item) => ({
      level: (item.reportCount >= URGENT_REPORTS ? "ERROR" : "WARN") as OpsLogLine["level"],
      message: `신고 ${item.reportCount}회 · ${item.title}`,
      timeLabel: clockLabel(item.createdAt),
    })),
  ]
  return {
    ...base,
    logs: [...base.logs, ...extraLogs].slice(0, 5),
    openReports: reports.length,
    urgentReports: reports.filter((item) => item.reportCount >= URGENT_REPORTS).length,
    mailFailures: failedTotal,
    mailSuccessPercent: successPercent(sentTotal, failedTotal),
    mainimgLive,
    popupLive,
    ulinkCount,
    mailTodayLabel: mailToday,
    notices,
  }
}
