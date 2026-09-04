"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  blockModerationBoard,
  fetchHiddenBoards,
  fetchModerationCandidates,
  fetchModerationRunDetail,
  fetchModerationRuns,
  hideOverReported,
  restoreModerationBoard,
  type ModerationItem,
  type ModerationRun,
} from "@/lib/boardModeration"

const inputClass =
  "rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type TabKey = "candidates" | "hidden" | "runs"

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

function communityPath(boardType: string, id: number): string {
  switch (boardType) {
    case "NOTICE":
      return `/community/notice/${id}`
    case "QNA":
      return `/community/qna/${id}`
    case "FREE":
    case "COMMUNITY":
    default:
      return `/community/free/${id}`
  }
}

function BoardTypeLabel({ type }: { type: string }) {
  const map: Record<string, string> = {
    NOTICE: "공지",
    FREE: "자유",
    QNA: "Q&A",
    COMMUNITY: "커뮤니티",
  }
  return <span>{map[type] ?? type}</span>
}

export default function AdminModerationClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [tab, setTab] = useState<TabKey>("candidates")
  const [threshold, setThreshold] = useState(5)
  const [candidates, setCandidates] = useState<ModerationItem[]>([])
  const [hidden, setHidden] = useState<ModerationItem[]>([])
  const [runs, setRuns] = useState<ModerationRun[]>([])
  const [selectedRun, setSelectedRun] = useState<ModerationRun | null>(null)
  const [loading, setLoading] = useState(false)
  const [acting, setActing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const loadCandidates = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchModerationCandidates(threshold)
    setLoading(false)
    if (!result.ok) {
      setCandidates([])
      setError(result.message)
      return
    }
    setCandidates(result.data)
  }, [threshold])

  const loadHidden = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchHiddenBoards()
    setLoading(false)
    if (!result.ok) {
      setHidden([])
      setError(result.message)
      return
    }
    setHidden(result.data)
  }, [])

  const loadRuns = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchModerationRuns(30)
    setLoading(false)
    if (!result.ok) {
      setRuns([])
      setError(result.message)
      return
    }
    setRuns(result.data)
  }, [])

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
    if (tab === "candidates") void loadCandidates()
    if (tab === "hidden") void loadHidden()
    if (tab === "runs") void loadRuns()
  }, [allowed, tab, loadCandidates, loadHidden, loadRuns])

  async function onHideAll() {
    if (candidates.length === 0) {
      setError("숨길 후보가 없습니다. 임계값을 확인하거나 미리보기를 새로고침하세요.")
      return
    }
    if (
      !window.confirm(
        `신고 ${threshold}회 이상 후보 ${candidates.length}건을 숨김 처리할까요?`,
      )
    ) {
      return
    }
    setActing(true)
    setError(null)
    setMessage(null)
    const result = await hideOverReported(threshold)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(
      `숨김 완료 · ${result.data.hiddenCount}건 (실행 #${result.data.runId})`,
    )
    setSelectedRun(result.data)
    setTab("runs")
    await loadRuns()
  }

  async function onRestore(item: ModerationItem) {
    if (!window.confirm(`「${item.title}」을(를) 복구할까요?`)) return
    setActing(true)
    setError(null)
    setMessage(null)
    const result = await restoreModerationBoard(item.id)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`복구 완료 · #${item.id}`)
    await loadHidden()
  }

  async function onBlock(item: ModerationItem) {
    if (!window.confirm(`「${item.title}」을(를) 영구 차단할까요?`)) return
    setActing(true)
    setError(null)
    setMessage(null)
    const result = await blockModerationBoard(item.id)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`차단 완료 · #${item.id}`)
    await loadHidden()
  }

  async function onOpenRun(runId: number) {
    setActing(true)
    setError(null)
    const result = await fetchModerationRunDetail(runId)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setSelectedRun(result.data)
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
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-gray-900">신고 관리</h1>
        <p className="mt-1 text-sm text-gray-500">
          신고가 쌓인 커뮤니티 글을 미리보고 일괄 숨김·복구·차단합니다. MEDIA 글은 제외됩니다.
          매일 새벽 3시 배치도 동일 임계값으로 동작합니다.
        </p>
      </div>

      {error ? (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      ) : null}
      {message ? (
        <div className="mb-4 rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {message}
        </div>
      ) : null}

      <div className="mb-4 flex flex-wrap gap-2 border-b border-gray-200 pb-3">
        {(
          [
            ["candidates", "숨김 후보"],
            ["hidden", "숨김 목록"],
            ["runs", "실행 이력"],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={
              tab === key
                ? "rounded bg-gray-900 px-3 py-1.5 text-sm text-white"
                : buttonClass
            }
          >
            {label}
          </button>
        ))}
      </div>

      {tab === "candidates" ? (
        <section className="rounded border border-gray-200 bg-white">
          <div className="flex flex-wrap items-end gap-3 border-b border-gray-100 px-4 py-3">
            <label className="text-xs text-gray-600">
              신고 임계값
              <input
                type="number"
                min={1}
                max={1000}
                className={`${inputClass} mt-1 block w-24`}
                value={threshold}
                onChange={(e) => setThreshold(Math.max(1, Number(e.target.value) || 1))}
              />
            </label>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadCandidates()}>
              미리보기
            </button>
            <button
              type="button"
              className={primaryButtonClass}
              disabled={acting || loading || candidates.length === 0}
              onClick={() => void onHideAll()}
            >
              일괄 숨김 실행
            </button>
            <span className="ml-auto text-xs text-gray-500">후보 {candidates.length}건</span>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : candidates.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">
              임계값 이상 신고된 ACTIVE 글이 없습니다.
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-xs text-gray-500">
                <tr>
                  <th className="px-4 py-2">ID</th>
                  <th className="px-4 py-2">유형</th>
                  <th className="px-4 py-2">제목</th>
                  <th className="px-4 py-2">신고</th>
                  <th className="px-4 py-2">작성자</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {candidates.map((item) => (
                  <tr key={item.id}>
                    <td className="px-4 py-2 text-gray-500">{item.id}</td>
                    <td className="px-4 py-2">
                      <BoardTypeLabel type={item.boardType} />
                    </td>
                    <td className="px-4 py-2">
                      <Link
                        href={communityPath(item.boardType, item.id)}
                        className="text-finsight-primary hover:underline"
                        target="_blank"
                      >
                        {item.title}
                      </Link>
                    </td>
                    <td className="px-4 py-2 font-semibold text-red-600">{item.reportCount}</td>
                    <td className="px-4 py-2 text-xs text-gray-500">{item.authorEmail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      ) : null}

      {tab === "hidden" ? (
        <section className="rounded border border-gray-200 bg-white">
          <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-800">숨김 게시글 ({hidden.length})</h2>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadHidden()}>
              새로고침
            </button>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : hidden.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">숨김 글이 없습니다.</div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {hidden.map((item) => (
                <li key={item.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs text-gray-400">#{item.id}</span>
                      <BoardTypeLabel type={item.boardType} />
                      <span className="truncate font-medium text-gray-900">{item.title}</span>
                      <span className="rounded bg-red-50 px-1.5 py-0.5 text-[11px] text-red-700">
                        신고 {item.reportCount}
                      </span>
                    </div>
                    <p className="mt-0.5 text-xs text-gray-500">
                      {item.authorEmail} · {formatDate(item.updatedAt || item.createdAt)}
                    </p>
                  </div>
                  <button
                    type="button"
                    className={buttonClass}
                    disabled={acting}
                    onClick={() => void onRestore(item)}
                  >
                    복구
                  </button>
                  <button
                    type="button"
                    className={buttonClass}
                    disabled={acting}
                    onClick={() => void onBlock(item)}
                  >
                    영구 차단
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}

      {tab === "runs" ? (
        <div className="grid gap-4 lg:grid-cols-[1fr_1.1fr]">
          <section className="rounded border border-gray-200 bg-white">
            <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
              <h2 className="text-sm font-semibold text-gray-800">실행 이력</h2>
              <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadRuns()}>
                새로고침
              </button>
            </div>
            {loading ? (
              <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
            ) : runs.length === 0 ? (
              <div className="px-4 py-12 text-center text-sm text-gray-500">실행 이력이 없습니다.</div>
            ) : (
              <ul className="divide-y divide-gray-100">
                {runs.map((run) => (
                  <li key={run.runId}>
                    <button
                      type="button"
                      className={`w-full px-4 py-3 text-left hover:bg-gray-50 ${
                        selectedRun?.runId === run.runId ? "bg-gray-50" : ""
                      }`}
                      onClick={() => void onOpenRun(run.runId)}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-medium text-gray-900">
                          #{run.runId} · 숨김 {run.hiddenCount}건
                        </span>
                        <span className="text-[11px] text-gray-500">
                          {run.triggeredBy === "BATCH" ? "배치" : "수동"}
                        </span>
                      </div>
                      <p className="mt-0.5 text-xs text-gray-500">
                        임계값 {run.reportThreshold} · {formatDate(run.createdAt)}
                        {run.actorEmail ? ` · ${run.actorEmail}` : ""}
                      </p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="rounded border border-gray-200 bg-white">
            <div className="border-b border-gray-100 px-4 py-3">
              <h2 className="text-sm font-semibold text-gray-800">
                {selectedRun ? `실행 #${selectedRun.runId} 상세` : "실행 상세"}
              </h2>
            </div>
            {!selectedRun ? (
              <div className="px-4 py-12 text-center text-sm text-gray-500">
                왼쪽에서 이력을 선택하세요.
              </div>
            ) : selectedRun.items.length === 0 ? (
              <div className="px-4 py-12 text-center text-sm text-gray-500">
                해당 실행에서 숨긴 글이 없습니다.
              </div>
            ) : (
              <ul className="divide-y divide-gray-100">
                {selectedRun.items.map((item) => (
                  <li key={`${selectedRun.runId}-${item.id}`} className="px-4 py-3 text-sm">
                    <div className="font-medium text-gray-900">
                      #{item.id} {item.title}
                    </div>
                    <p className="mt-0.5 text-xs text-gray-500">
                      <BoardTypeLabel type={item.boardType} /> · 신고 {item.reportCount} ·{" "}
                      {item.authorEmail}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      ) : null}
    </div>
  )
}
