"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers, fetchAdminUsers } from "@/lib/adminUsers"
import { fetchMaintenanceRuns, type ModerationRun } from "@/lib/boardModeration"
import PortfolioShareArticle from "@/components/community/PortfolioShareArticle"
import { PortfolioDialog } from "@/components/portfolio/PortfolioDialog"
import {
  fetchPortfolioDetections,
  fetchPortfolioShares,
  moderatePortfolioShare,
  type PortfolioDetectionChip,
  type PortfolioShareCard,
} from "@/lib/portfolioApi"

type ReportStatus = "대기" | "검토중" | "정상" | "경고" | "블라인드" | "정지 7일" | "삭제"

type ReportRow = {
  id: string
  shareId?: number
  type: string
  typeClass: string
  author: string
  content: string
  count: number
  status: ReportStatus
  actions: string[]
  card?: PortfolioShareCard
}

const PAGE_SIZE = 2
const AUTHOR_LIMIT = 12

const INITIAL_ROWS: ReportRow[] = [
  { id: "r1", type: "투자 사기", typeClass: "text-rose-500", author: "user_1938", content: "“이 코인 사면 무조건 10배...”", count: 12, status: "대기", actions: ["정상", "블라인드", "삭제"] },
  { id: "r2", type: "개인정보", typeClass: "text-orange-500", author: "invest_moon", content: "계좌 캡처 이미지 포함", count: 8, status: "대기", actions: ["경고", "블라인드", "삭제"] },
  { id: "r3", type: "광고", typeClass: "text-slate-500", author: "stockking", content: "“오픈채팅방 링크...”", count: 5, status: "검토중", actions: ["정상", "정지 7일", "삭제"] },
]

export default function PortfolioReportAdmin({ onBack }: { onBack?: () => void }) {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [rows, setRows] = useState(INITIAL_ROWS)
  const [blindToday, setBlindToday] = useState(3)
  const [chips, setChips] = useState<PortfolioDetectionChip[]>([])
  const [signalNote, setSignalNote] = useState("공개 공유 글과 신고 사유를 집계하는 중입니다.")
  const [names, setNames] = useState<Record<string, { username: string; id: string }>>({})
  const [page, setPage] = useState(0)
  const [detail, setDetail] = useState<ReportRow | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [runs, setRuns] = useState<ModerationRun[]>([])
  const [runNote, setRunNote] = useState("")
  const [localHistory, setLocalHistory] = useState<string[]>([])
  const [actionError, setActionError] = useState("")
  const waiting = rows.filter((row) => row.status === "대기" || row.status === "검토중").length
  const allowed = Boolean(onBack) || Boolean(user && canManageUsers(user.role))

  useEffect(() => {
    if (onBack || !ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/admin/moderation")}`)
      return
    }
    if (!canManageUsers(user.role)) router.replace("/")
  }, [onBack, ready, user, router])

  useEffect(() => {
    if (!allowed) return
    let alive = true
    fetchPortfolioDetections()
      .then((detection) => {
        if (!alive) return
        setChips(detection.chips)
        setSignalNote(detection.note)
      })
      .catch((reason: unknown) => {
        if (!alive) return
        setChips([])
        setSignalNote(reason instanceof Error ? reason.message : "공유 자동 탐지를 불러오지 못했습니다.")
      })
    return () => {
      alive = false
    }
  }, [allowed])

  useEffect(() => {
    if (!allowed) return
    let alive = true
    fetchAdminUsers({ page: 0, size: 100, status: "", keyword: "", reveal: ["username"] }).then((result) => {
      if (!alive || !result.ok) return
      const loaded = result.data.content.filter((member) => member.username && !member.usernameMasked)
      const next: Record<string, { username: string; id: string }> = {}
      INITIAL_ROWS.forEach((row, index) => {
        const matched = loaded.find((member) => member.username === row.author) ?? loaded[index]
        if (!matched) return
        next[row.id] = { username: matched.username, id: String(matched.id) }
      })
      setNames(next)
    })
    return () => {
      alive = false
    }
  }, [allowed])

  useEffect(() => {
    if (!allowed) return
    let alive = true
    fetchPortfolioShares(0, 12)
      .then((feed) => {
        if (!alive || feed.cards.length === 0) return
        const live = feed.cards.map(shareRow).filter((row): row is ReportRow => row !== null)
        setRows([...live, ...INITIAL_ROWS])
      })
      .catch(() => {
        if (alive) setActionError("공개 공유 카드를 불러오지 못했습니다.")
      })
    return () => {
      alive = false
    }
  }, [allowed])

  useEffect(() => {
    if (!historyOpen) return
    let alive = true
    fetchMaintenanceRuns(20).then((result) => {
      if (!alive) return
      if (!result.ok) {
        setRuns([])
        setRunNote(result.message)
        return
      }
      setRuns(result.data)
      setRunNote(result.data.length === 0 ? "저장된 처리 이력이 없습니다." : "")
    })
    return () => {
      alive = false
    }
  }, [historyOpen])

  if (!allowed) {
    return <p className="px-6 py-10 text-sm text-slate-500">권한을 확인하는 중입니다.</p>
  }

  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const pageRows = rows.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE)

  async function apply(id: string, action: string) {
    const row = rows.find((item) => item.id === id)
    const who = row ? authorText(row, names) : id
    const stamp = new Intl.DateTimeFormat("ko-KR", {
      timeZone: "Asia/Seoul",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date())
    setLocalHistory((current) => [`${stamp} · ${who} · ${action}`, ...current])
    setActionError("")
    if (row?.shareId && (action === "경고" || action === "블라인드" || action === "삭제")) {
      const status = action === "경고" ? "WARN" : action === "블라인드" ? "BLIND" : "REMOVED"
      try {
        const updated = await moderatePortfolioShare(row.shareId, status)
        const next = shareRow(updated)
        setRows((current) => current.flatMap((item) => {
          if (item.id !== id) return [item]
          return next ? [next] : []
        }))
        setDetail((current) => (current?.id === id ? next : current))
        if (action === "블라인드") setBlindToday((count) => count + 1)
      } catch (reason) {
        setActionError(reason instanceof Error ? reason.message : "공유 게시물을 처리하지 못했습니다.")
      }
      return
    }
    if (action === "삭제") {
      setRows((current) => current.filter((item) => item.id !== id))
      return
    }
    if (action === "블라인드") setBlindToday((count) => count + 1)
    const status: ReportStatus = action === "정상" || action === "경고" || action === "블라인드" || action === "정지 7일" ? action : "검토중"
    setRows((current) => current.map((item) => (item.id === id ? { ...item, status } : item)))
  }

  return (
    <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-4 px-4 py-6 md:px-6">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Kpi label="오늘 공유" value="53" />
        <Kpi label="공개 Portfolio" value="824" />
        <Kpi label="신고 대기" value={String(waiting)} accent />
        <Kpi label="오늘 블라인드" value={String(blindToday)} />
      </div>
      <section className="border border-neutral-300 bg-white p-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h1 className="text-lg font-bold text-slate-900">포트폴리오 신고 관리</h1>
            <p className="mt-1 text-xs text-slate-500">PRIVATE 자산 원본은 관리자 화면에 노출하지 않음</p>
          </div>
          <button type="button" onClick={() => setHistoryOpen(true)} className="border border-neutral-800 bg-white px-3 py-2 text-sm font-semibold text-slate-800">
            처리이력
          </button>
        </div>
        {actionError ? <p className="mt-3 text-sm text-red-600">{actionError}</p> : null}
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="text-xs text-slate-400">
              <tr>
                <th className="pb-2 font-medium">신고유형</th>
                <th className="pb-2 font-medium">작성자</th>
                <th className="pb-2 font-medium">공개 콘텐츠</th>
                <th className="pb-2 font-medium">신고수</th>
                <th className="pb-2 font-medium">상태</th>
                <th className="pb-2 font-medium">관리</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="border-t border-neutral-200 py-6 text-slate-500">처리할 신고가 없습니다.</td>
                </tr>
              ) : null}
              {pageRows.map((row) => (
                <tr key={row.id} className={`border-t border-neutral-200 ${row.status === "경고" ? "bg-rose-50" : ""}`}>
                  <td className={`py-3 font-semibold ${row.typeClass}`}>{row.type}</td>
                  <td className="py-3 text-slate-700">{authorText(row, names)}</td>
                  <td className="py-3">
                    <button type="button" onClick={() => setDetail(row)} className={`text-left underline ${row.status === "삭제" ? "text-slate-400 line-through" : "text-slate-700"}`}>
                      {row.content}
                    </button>
                  </td>
                  <td className="py-3">{row.count}</td>
                  <td className="py-3"><span className="border border-neutral-300 bg-slate-100 px-2 py-1 text-xs">{row.status}</span></td>
                  <td className="py-3">
                    <div className="flex gap-2">
                      {row.actions.map((action) => (
                        <button key={action} type="button" onClick={() => void apply(row.id, action)} className="text-xs font-medium text-slate-500 hover:text-slate-900">{action}</button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center justify-end gap-2 text-sm">
          <button type="button" disabled={safePage <= 0} onClick={() => setPage(0)} className="border border-neutral-300 px-2 py-1 disabled:opacity-40">처음</button>
          <button type="button" disabled={safePage <= 0} onClick={() => setPage(safePage - 1)} className="border border-neutral-300 px-2 py-1 disabled:opacity-40">이전</button>
          <span className="px-2 text-slate-600">{safePage + 1} / {totalPages}</span>
          <button type="button" disabled={safePage >= totalPages - 1} onClick={() => setPage(safePage + 1)} className="border border-neutral-300 px-2 py-1 disabled:opacity-40">다음</button>
          <button type="button" disabled={safePage >= totalPages - 1} onClick={() => setPage(totalPages - 1)} className="border border-neutral-300 px-2 py-1 disabled:opacity-40">맨끝</button>
        </div>
      </section>
      <div className="grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <section className="border border-neutral-300 bg-white p-5">
          <h2 className="text-base font-bold text-slate-900">운영 원칙</h2>
          <ul className="mt-3 space-y-2 text-sm">
            <li className="border border-emerald-200 bg-emerald-50 px-3 py-2 text-emerald-800">PUBLIC으로 공유된 Snapshot만 moderation 대상</li>
            <li className="border border-emerald-200 bg-emerald-50 px-3 py-2 text-emerald-800">PRIVATE Portfolio 상세값은 기본적으로 관리자에게 숨김</li>
            <li className="border border-amber-200 bg-amber-50 px-3 py-2 text-amber-900">신고 처리에 필요한 최소 정보만 노출</li>
          </ul>
        </section>
        <section className="border border-neutral-300 bg-white p-5">
          <h2 className="text-base font-bold text-slate-900">자동 탐지</h2>
          <p className="mt-1 text-xs text-slate-500">{signalNote}</p>
          <div className="mt-3 flex flex-nowrap gap-2 overflow-x-auto">
            {chips.map((chip) => (
              <span key={chip.label} className="shrink-0 whitespace-nowrap border border-neutral-300 bg-slate-50 px-2 py-1 text-xs text-slate-600">
                {chip.label} {chip.count}
              </span>
            ))}
          </div>
        </section>
      </div>
      {detail ? (
        <PortfolioDialog title="공개 콘텐츠" subtitle={authorText(detail, names)} onClose={() => setDetail(null)}>
          <p className="text-xs text-slate-500">{detail.type} · 신고 {detail.count} · {detail.status}</p>
          {detail.card ? (
            <div className="mt-3">
              <PortfolioShareArticle card={detail.card} />
            </div>
          ) : (
            <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-900">{detail.content}</p>
          )}
          <div className="mt-5 flex justify-end">
            <button type="button" onClick={() => setDetail(null)} className="border border-neutral-300 px-4 py-2 text-sm">닫기</button>
          </div>
        </PortfolioDialog>
      ) : null}
      {historyOpen ? (
        <PortfolioDialog title="처리 이력" subtitle="신고 관리에서 처리한 기록과 저장된 실행 이력입니다." onClose={() => setHistoryOpen(false)} wide>
          <h3 className="text-sm font-bold text-slate-900">이번 화면에서 처리</h3>
          <ul className="mt-2 space-y-2 text-sm text-slate-700">
            {localHistory.length === 0 ? <li className="text-slate-500">아직 처리한 신고가 없습니다.</li> : null}
            {localHistory.map((line, index) => (
              <li key={`${line}-${index}`} className="border border-neutral-200 px-3 py-2">{line}</li>
            ))}
          </ul>
          <h3 className="mt-5 text-sm font-bold text-slate-900">저장된 실행 이력</h3>
          {runNote ? <p className="mt-2 text-sm text-slate-500">{runNote}</p> : null}
          <ul className="mt-2 space-y-2 text-sm text-slate-700">
            {runs.map((run) => (
              <li key={run.runId} className="border border-neutral-200 px-3 py-2">
                #{run.runId} · {run.triggeredBy === "BATCH" ? "자동" : "수동"} · 숨김 {run.hiddenCount}건 · 임계값 {run.reportThreshold}
                {run.actorEmail ? ` · ${run.actorEmail}` : ""}
                {run.createdAt ? ` · ${run.createdAt.replace("T", " ").slice(0, 16)}` : ""}
              </li>
            ))}
          </ul>
          <div className="mt-5 flex justify-end">
            <button type="button" onClick={() => setHistoryOpen(false)} className="border border-neutral-300 px-4 py-2 text-sm">닫기</button>
          </div>
        </PortfolioDialog>
      ) : null}
      {onBack ? (
        <div className="flex justify-end">
          <button type="button" onClick={onBack} className="border border-neutral-800 bg-white px-3 py-2 text-sm font-semibold text-slate-800">포트폴리오</button>
        </div>
      ) : null}
    </div>
  )
}

function shareRow(card: PortfolioShareCard): ReportRow | null {
  const likes = card.likeCount ?? 0
  const dislikes = card.dislikeCount ?? 0
  const reports = card.reportCount ?? 0
  const removed = card.moderationStatus === "REMOVED"
  const blind = card.moderationStatus === "BLIND" || reports > 0
  const warn = !removed && !blind && (card.moderationStatus === "WARN" || dislikes > 0)
  const normal = !removed && !blind && !warn && likes >= 10
  if (!removed && !blind && !warn && !normal) return null
  const status: ReportStatus = removed ? "삭제" : blind ? "블라인드" : warn ? "경고" : "정상"
  const type = reports > 0 ? "신고" : dislikes > 0 ? "싫어요" : likes >= 10 ? "좋아요" : "공유"
  const hidden = status === "블라인드" || status === "삭제"
  return {
    id: `share-${card.id}`,
    shareId: card.id,
    type,
    typeClass: type === "신고" || status === "삭제" ? "text-rose-500" : type === "싫어요" ? "text-orange-500" : type === "좋아요" ? "text-emerald-600" : "text-slate-500",
    author: card.authorName,
    content: hidden ? (status === "블라인드" ? "블라인드 처리된 게시물입니다." : "삭제된 게시물입니다.") : card.message,
    count: reports,
    status,
    actions: status === "삭제" ? [] : ["경고", "블라인드", "삭제"],
    card,
  }
}

function authorText(row: ReportRow, names: Record<string, { username: string; id: string }>): string {
  const member = names[row.id]
  if (!member) return row.author
  return authorLabel(member.username, member.id)
}

function authorLabel(username: string, id: string): string {
  const name = username.trim()
  const key = id.trim()
  if (!name) return key || "-"
  if (!key || name === key) return name
  const combined = `${name} (${key})`
  return combined.length > AUTHOR_LIMIT ? name : combined
}

function Kpi({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="border border-neutral-300 bg-white px-4 py-4">
      <p className="text-xs text-slate-400">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${accent ? "text-rose-500" : "text-slate-900"}`}>{value}</p>
    </div>
  )
}
