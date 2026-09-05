"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  blockModerationBoard,
  fetchHiddenBoards,
  fetchModerationCandidates,
  hideOverReported,
  restoreModerationBoard,
  type ModerationItem,
} from "@/lib/boardModeration"

const inputClass =
  "rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type TabKey = "candidates" | "hidden"

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

export default function AdminModerationClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [tab, setTab] = useState<TabKey>("candidates")
  const [threshold, setThreshold] = useState(5)
  const [candidates, setCandidates] = useState<ModerationItem[]>([])
  const [hidden, setHidden] = useState<ModerationItem[]>([])
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
  }, [allowed, tab, loadCandidates, loadHidden])

  async function onHideAll() {
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
    setMessage(`숨김 완료 · ${result.data.hiddenCount}건`)
    await loadCandidates()
  }

  async function onRestore(item: ModerationItem) {
    setActing(true)
    setError(null)
    setMessage(null)
    const result = await restoreModerationBoard(item.id)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`댓글 #${item.id} 을(를) 복구했습니다.`)
    await loadHidden()
  }

  async function onBlock(item: ModerationItem) {
    if (!window.confirm(`댓글 #${item.id} 을(를) 영구 차단할까요?`)) return
    setActing(true)
    setError(null)
    setMessage(null)
    const result = await blockModerationBoard(item.id)
    setActing(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`댓글 #${item.id} 을(를) 차단했습니다.`)
    if (tab === "candidates") await loadCandidates()
    else await loadHidden()
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
          댓글 신고만 접수됩니다. 임계값 이상 신고된 댓글을 숨김·복구·차단할 수 있습니다.
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

      <div className="mb-4 flex flex-wrap gap-2">
        {(
          [
            ["candidates", "숨김 후보"],
            ["hidden", "숨김 목록"],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            type="button"
            className={tab === key ? primaryButtonClass : buttonClass}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === "candidates" ? (
        <section className="rounded border border-gray-200 bg-white">
          <div className="flex flex-wrap items-center gap-3 border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-800">신고 과다 댓글 후보</h2>
            <label className="ml-auto flex items-center gap-2 text-xs text-gray-600">
              신고 임계값
              <input
                type="number"
                min={1}
                max={1000}
                className={`${inputClass} w-20`}
                value={threshold}
                onChange={(e) => setThreshold(Math.max(1, Number(e.target.value) || 1))}
              />
            </label>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadCandidates()}>
              새로고침
            </button>
            <button
              type="button"
              className={primaryButtonClass}
              disabled={acting || candidates.length === 0}
              onClick={() => void onHideAll()}
            >
              일괄 숨김 실행
            </button>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : candidates.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">
              임계값 이상 신고된 ACTIVE 댓글이 없습니다.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50 text-xs text-gray-500">
                  <tr>
                    <th className="px-4 py-2">ID</th>
                    <th className="px-4 py-2">내용</th>
                    <th className="px-4 py-2">작성자</th>
                    <th className="px-4 py-2">신고</th>
                    <th className="px-4 py-2">게시글</th>
                    <th className="px-4 py-2">작업</th>
                  </tr>
                </thead>
                <tbody>
                  {candidates.map((item) => (
                    <tr key={item.id} className="border-t border-gray-100">
                      <td className="px-4 py-2">{item.id}</td>
                      <td className="max-w-xs truncate px-4 py-2">{item.title || "-"}</td>
                      <td className="px-4 py-2 text-xs text-gray-600">{item.authorEmail}</td>
                      <td className="px-4 py-2 font-semibold text-red-600">{item.reportCount}</td>
                      <td className="px-4 py-2 text-xs text-gray-500">#{item.targetId ?? "-"}</td>
                      <td className="px-4 py-2">
                        <button
                          type="button"
                          className={buttonClass}
                          disabled={acting}
                          onClick={() => void onBlock(item)}
                        >
                          차단
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : null}

      {tab === "hidden" ? (
        <section className="rounded border border-gray-200 bg-white">
          <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-800">숨김 댓글 ({hidden.length})</h2>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void loadHidden()}>
              새로고침
            </button>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : hidden.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">숨김 댓글이 없습니다.</div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {hidden.map((item) => (
                <li key={item.id} className="px-4 py-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium text-gray-900">#{item.id}</span>
                    <span className="text-xs text-amber-700">신고 {item.reportCount}</span>
                    <span className="text-[11px] text-gray-400">{formatDate(item.updatedAt)}</span>
                  </div>
                  <p className="mt-1 text-sm text-gray-700">{item.title || "(내용 없음)"}</p>
                  <p className="mt-0.5 text-xs text-gray-500">
                    {item.authorEmail} · 게시글 #{item.targetId ?? "-"}
                  </p>
                  <div className="mt-2 flex gap-2">
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
                      차단
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
    </div>
  )
}
