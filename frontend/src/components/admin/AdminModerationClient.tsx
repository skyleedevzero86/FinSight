"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import FcbBoardFrame from "@/components/community/FcbBoardFrame"
import { canManageUsers } from "@/lib/adminUsers"
import {
  blockModerationBoard,
  fetchHiddenBoards,
  fetchModerationCandidates,
  hideOverReported,
  restoreModerationBoard,
  type ModerationItem,
} from "@/lib/boardModeration"

const PAGE_SIZE = 20

const buttonClass =
  "rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type TabKey = "candidates" | "hidden"

function matchesQuery(item: ModerationItem, type: string, raw: string): boolean {
  const query = raw.trim().toLowerCase()
  if (!query) return true
  const title = (item.title || "").toLowerCase()
  const author = item.authorEmail.toLowerCase()
  if (type === "author") return author.includes(query)
  if (type === "subject") return title.includes(query)
  return title.includes(query) || author.includes(query) || String(item.id).includes(query)
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
  const [page, setPage] = useState(0)
  const [searchType, setSearchType] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [searchValue, setSearchValue] = useState("")

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

  const source = tab === "candidates" ? candidates : hidden
  const filtered = useMemo(
    () => source.filter((item) => matchesQuery(item, searchType, searchValue)),
    [source, searchType, searchValue],
  )
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const pageItems = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  useEffect(() => {
    setPage((current) => Math.min(current, totalPages - 1))
  }, [totalPages])

  function selectTab(next: TabKey) {
    setTab(next)
    setPage(0)
  }

  function submitSearch() {
    setSearchValue(searchInput)
    setPage(0)
  }

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
      <div className="w-full px-6 py-16 text-sm text-gray-500">권한을 확인하는 중…</div>
    )
  }

  const emptyText =
    tab === "candidates"
      ? "임계값 이상 신고된 댓글이 없습니다."
      : "숨김 댓글이 없습니다."

  return (
    <FcbBoardFrame
      boardId="bbs_moderation"
      heading="신고 관리"
      description="댓글 신고만 접수됩니다. 임계값 이상 신고된 댓글을 숨김·복구·차단할 수 있습니다."
      caption={tab === "candidates" ? "숨김 후보" : "숨김 목록"}
      totalCount={filtered.length}
      currentPage={page + 1}
      totalPages={totalPages}
      onPage={(next) => setPage(Math.max(0, next - 1))}
      search={{
        type: searchType,
        value: searchInput,
        typeOptions: [
          { value: "subject", label: "내용" },
          { value: "author", label: "작성자" },
        ],
        onTypeChange: setSearchType,
        onValueChange: setSearchInput,
        onSubmit: submitSearch,
      }}
      beforeList={
        <>
          {error ? (
            <p className="mb-3 border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          ) : null}
          {message ? (
            <p className="mb-3 border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {message}
            </p>
          ) : null}
          <div className="bbs_cate tablist fcb-tablist">
            <ul className="tablist_3d fcb-tablist-3d">
              {(
                [
                  ["candidates", "숨김 후보"],
                  ["hidden", "숨김 목록"],
                ] as const
              ).map(([key, label]) => (
                <li key={key} className={tab === key ? "on fcb-on" : undefined}>
                  <button type="button" onClick={() => selectTab(key)}>
                    {label}
                  </button>
                </li>
              ))}
            </ul>
          </div>
          {tab === "candidates" ? (
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <label className="flex items-center gap-2 text-sm text-gray-700">
                신고 임계값
                <input
                  type="number"
                  min={1}
                  max={1000}
                  className="w-24 border border-gray-300 bg-white px-3 py-2 text-sm"
                  value={threshold}
                  onChange={(e) => setThreshold(Math.max(1, Number(e.target.value) || 1))}
                />
              </label>
              <button
                type="button"
                className={buttonClass}
                disabled={loading}
                onClick={() => void loadCandidates()}
              >
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
          ) : (
            <div className="mb-4">
              <button
                type="button"
                className={buttonClass}
                disabled={loading}
                onClick={() => void loadHidden()}
              >
                새로고침
              </button>
            </div>
          )}
        </>
      }
    >
      <thead>
        <tr>
          <th className="td_num">번호</th>
          <th className="td_subject">내용</th>
          <th className="td_name">작성자</th>
          <th className="td_hit">신고</th>
          <th className="td_date">게시글</th>
          <th className="td_action">작업</th>
        </tr>
      </thead>
      <tbody>
        {loading ? (
          <tr>
            <td colSpan={6} className="td_subject">
              불러오는 중…
            </td>
          </tr>
        ) : pageItems.length === 0 ? (
          <tr>
            <td colSpan={6} className="td_subject">
              {searchValue.trim() ? "검색 결과가 없습니다." : emptyText}
            </td>
          </tr>
        ) : (
          pageItems.map((item, index) => (
            <tr key={item.id}>
              <td className="td_num">{page * PAGE_SIZE + index + 1}</td>
              <td className="td_subject">{item.title || "(내용 없음)"}</td>
              <td className="td_name">{item.authorEmail || "-"}</td>
              <td className="td_hit">{item.reportCount}</td>
              <td className="td_date">#{item.targetId ?? "-"}</td>
              <td className="td_action">
                <div className="flex flex-wrap justify-center gap-1">
                  {tab === "hidden" ? (
                    <button
                      type="button"
                      className={buttonClass}
                      disabled={acting}
                      onClick={() => void onRestore(item)}
                    >
                      복구
                    </button>
                  ) : null}
                  <button
                    type="button"
                    className={buttonClass}
                    disabled={acting}
                    onClick={() => void onBlock(item)}
                  >
                    차단
                  </button>
                </div>
              </td>
            </tr>
          ))
        )}
      </tbody>
    </FcbBoardFrame>
  )
}
