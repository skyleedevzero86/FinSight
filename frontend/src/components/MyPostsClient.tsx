"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import AdminDateField from "@/components/admin/AdminDateField"
import {
  unwrapApiData,
  formatBoardDate,
  type BoardListItem,
  type BoardPagination,
} from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"

type TabKey = "boards" | "comments" | "boardReactions" | "commentReactions"

type CommentItem = {
  id: number
  content: string
  targetId: number
  commentType: string
  boardType: string | null
  createdAt: string | null
}

type BoardReactionItem = {
  boardId: number
  title: string
  boardType: string | null
  reactionType: string | null
  createdAt: string | null
}

type CommentReactionItem = {
  commentId: number
  content: string
  targetId: number | null
  commentType: string | null
  boardType: string | null
  reactionType: string | null
  createdAt: string | null
}

type ActivityStats = {
  boardCount: number
  commentCount: number
  boardReactionCount: number
  commentReactionCount: number
  rangeFrom: string | null
  rangeTo: string | null
  periodType: string
}

type PeriodMode = "ALL" | "DAILY" | "WEEKLY" | "MONTHLY" | "RANGE"

type PageState<T> = {
  items: T[]
  page: number
  totalPages: number
  totalElements: number
}

const PAGE_SIZE = 10

const TABS: { key: TabKey; label: string }[] = [
  { key: "boards", label: "작성한 글" },
  { key: "comments", label: "작성한 댓글" },
  { key: "boardReactions", label: "글에 남긴 좋아요·싫어요" },
  { key: "commentReactions", label: "댓글에 남긴 좋아요·싫어요" },
]

function boardHref(boardType: string | null | undefined, id: number): string {
  if (boardType === "NOTICE") return `/community/notice/${id}`
  if (boardType === "FREE") return `/community/free/${id}`
  if (boardType === "QNA") return `/community/qna/${id}`
  return `/community/free/${id}`
}

function reactionLabel(type: string | null | undefined): string {
  if (type === "LIKE") return "좋아요"
  if (type === "DISLIKE") return "싫어요"
  return type || "-"
}

function todayYmd(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

function monthYmd(): string {
  return todayYmd().slice(0, 7) + "-01"
}

function parsePaged<T>(payload: unknown): PageState<T> {
  const data = unwrapApiData<BoardPagination | T[]>(payload)
  if (!data) {
    return { items: [], page: 0, totalPages: 1, totalElements: 0 }
  }
  if (Array.isArray(data)) {
    return {
      items: data as T[],
      page: 0,
      totalPages: 1,
      totalElements: data.length,
    }
  }
  const content = Array.isArray(data.content) ? (data.content as T[]) : []
  return {
    items: content,
    page: Number(data.page) || 0,
    totalPages: Math.max(1, Number(data.totalPages) || 1),
    totalElements: Number(data.totalElements) || content.length,
  }
}

const squareBox = "border border-slate-300 bg-white"
const squareBtn =
  "border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 hover:bg-slate-50 disabled:opacity-50"
const squareBtnActive = "border border-slate-900 bg-slate-900 px-3 py-2 text-sm text-white"
const squareInput =
  "w-full border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-finsight-secondary"

export default function MyPostsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [tab, setTab] = useState<TabKey>("boards")
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [statsLoading, setStatsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState<ActivityStats | null>(null)
  const [periodMode, setPeriodMode] = useState<PeriodMode>("ALL")
  const [fromDate, setFromDate] = useState(todayYmd())
  const [toDate, setToDate] = useState(todayYmd())

  const [boards, setBoards] = useState<PageState<BoardListItem>>({
    items: [],
    page: 0,
    totalPages: 1,
    totalElements: 0,
  })
  const [comments, setComments] = useState<PageState<CommentItem>>({
    items: [],
    page: 0,
    totalPages: 1,
    totalElements: 0,
  })
  const [boardReactions, setBoardReactions] = useState<PageState<BoardReactionItem>>({
    items: [],
    page: 0,
    totalPages: 1,
    totalElements: 0,
  })
  const [commentReactions, setCommentReactions] = useState<PageState<CommentReactionItem>>({
    items: [],
    page: 0,
    totalPages: 1,
    totalElements: 0,
  })

  const loadStats = useCallback(async () => {
    setStatsLoading(true)
    try {
      const params = new URLSearchParams()
      params.set("periodType", periodMode)
      if (periodMode === "DAILY" || periodMode === "WEEKLY") {
        params.set("fromDate", fromDate)
      } else if (periodMode === "MONTHLY") {
        params.set("fromDate", fromDate.slice(0, 7) + "-01")
      } else if (periodMode === "RANGE") {
        params.set("fromDate", fromDate)
        params.set("toDate", toDate)
      }
      const res = await fetch(`/api/v1/boards/my-activity-stats?${params.toString()}`, {
        headers: { Accept: "application/json", ...authHeadersJson() },
        cache: "no-store",
      })
      const json = await res.json()
      if (!res.ok) {
        setStats(null)
        return
      }
      const data = unwrapApiData<ActivityStats>(json)
      setStats(data)
    } catch {
      setStats(null)
    } finally {
      setStatsLoading(false)
    }
  }, [periodMode, fromDate, toDate])

  const loadTab = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const headers = { Accept: "application/json", ...authHeadersJson() }
      const q = `page=${page}&size=${PAGE_SIZE}`
      if (tab === "boards") {
        const res = await fetch(`/api/v1/boards/my-boards?${q}`, { headers, cache: "no-store" })
        const json = await res.json()
        if (!res.ok) throw new Error("fail")
        setBoards(parsePaged<BoardListItem>(json))
      } else if (tab === "comments") {
        const res = await fetch(`/api/v1/comments/my-comments?${q}`, { headers, cache: "no-store" })
        const json = await res.json()
        if (!res.ok) throw new Error("fail")
        setComments(parsePaged<CommentItem>(json))
      } else if (tab === "boardReactions") {
        const res = await fetch(`/api/v1/boards/my-reactions?${q}`, { headers, cache: "no-store" })
        const json = await res.json()
        if (!res.ok) throw new Error("fail")
        setBoardReactions(parsePaged<BoardReactionItem>(json))
      } else {
        const res = await fetch(`/api/v1/comments/my-reactions?${q}`, { headers, cache: "no-store" })
        const json = await res.json()
        if (!res.ok) throw new Error("fail")
        setCommentReactions(parsePaged<CommentReactionItem>(json))
      }
    } catch {
      setError("활동 기록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [tab, page])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/myinfo/posts")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!ready || !user) return
    void loadStats()
  }, [ready, user, loadStats])

  useEffect(() => {
    if (!ready || !user) return
    void loadTab()
  }, [ready, user, loadTab])

  function selectTab(next: TabKey) {
    setTab(next)
    setPage(0)
  }

  function selectPeriod(mode: PeriodMode) {
    setPeriodMode(mode)
    if (mode === "DAILY" || mode === "WEEKLY") {
      setFromDate(todayYmd())
    } else if (mode === "MONTHLY") {
      setFromDate(monthYmd())
    } else if (mode === "RANGE") {
      setFromDate(todayYmd())
      setToDate(todayYmd())
    }
  }

  if (!ready || !user) {
    return <p className="text-sm text-slate-500">불러오는 중…</p>
  }

  const currentTotalPages =
    tab === "boards"
      ? boards.totalPages
      : tab === "comments"
        ? comments.totalPages
        : tab === "boardReactions"
          ? boardReactions.totalPages
          : commentReactions.totalPages

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-8">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">나의 게시글</h1>
        <p className="mt-1 text-sm text-slate-600">내가 쓴 글·댓글·반응 기록을 확인합니다.</p>
      </header>

      <section className={`${squareBox} p-4`}>
        <div className="mb-3 flex flex-wrap gap-2">
          {(
            [
              ["ALL", "전체"],
              ["DAILY", "일별"],
              ["WEEKLY", "주별"],
              ["MONTHLY", "월별"],
              ["RANGE", "기간별"],
            ] as const
          ).map(([mode, label]) => (
            <button
              key={mode}
              type="button"
              className={periodMode === mode ? squareBtnActive : squareBtn}
              onClick={() => selectPeriod(mode)}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="mb-3 overflow-hidden [&_button]:rounded-none [&_input]:rounded-none [&_div]:rounded-none">
          {periodMode === "DAILY" || periodMode === "WEEKLY" ? (
            <div className="max-w-xs">
              <AdminDateField
                label={periodMode === "DAILY" ? "기준일" : "주 시작 기준일"}
                value={fromDate}
                onChange={setFromDate}
              />
            </div>
          ) : null}
          {periodMode === "MONTHLY" ? (
            <label className="block max-w-xs text-xs text-slate-600">
              기준 월
              <input
                type="month"
                className={`${squareInput} mt-1`}
                value={fromDate.slice(0, 7)}
                onChange={(e) => setFromDate(`${e.target.value}-01`)}
              />
            </label>
          ) : null}
          {periodMode === "RANGE" ? (
            <div className="grid max-w-xl grid-cols-2 gap-3">
              <AdminDateField label="시작일" value={fromDate} onChange={setFromDate} />
              <AdminDateField label="종료일" value={toDate} onChange={setToDate} />
            </div>
          ) : null}
        </div>

        {statsLoading ? (
          <p className="text-sm text-slate-500">통계 불러오는 중…</p>
        ) : stats ? (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-slate-100 text-left text-slate-700">
                <th className="border border-slate-300 px-3 py-2 font-semibold">구분</th>
                <th className="border border-slate-300 px-3 py-2 font-semibold">건수</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="border border-slate-300 px-3 py-2">작성한 글</td>
                <td className="border border-slate-300 px-3 py-2">{stats.boardCount}</td>
              </tr>
              <tr>
                <td className="border border-slate-300 px-3 py-2">작성한 댓글</td>
                <td className="border border-slate-300 px-3 py-2">{stats.commentCount}</td>
              </tr>
              <tr>
                <td className="border border-slate-300 px-3 py-2">글에 남긴 좋아요·싫어요</td>
                <td className="border border-slate-300 px-3 py-2">{stats.boardReactionCount}</td>
              </tr>
              <tr>
                <td className="border border-slate-300 px-3 py-2">댓글에 남긴 좋아요·싫어요</td>
                <td className="border border-slate-300 px-3 py-2">{stats.commentReactionCount}</td>
              </tr>
            </tbody>
          </table>
        ) : (
          <p className="text-sm text-slate-500">통계를 불러오지 못했습니다.</p>
        )}
        {stats?.rangeFrom && stats?.rangeTo ? (
          <p className="mt-2 text-xs text-slate-500">
            집계 기간: {stats.rangeFrom} ~ {stats.rangeTo}
          </p>
        ) : null}
      </section>

      <div className="flex flex-wrap gap-0 border border-slate-300">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            className={
              tab === t.key
                ? "border-r border-slate-300 bg-slate-900 px-4 py-2 text-sm text-white last:border-r-0"
                : "border-r border-slate-300 bg-white px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 last:border-r-0"
            }
            onClick={() => selectTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error ? (
        <p className="border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      ) : null}
      {loading ? <p className="text-sm text-slate-500">불러오는 중…</p> : null}

      {!loading && tab === "boards" ? (
        boards.items.length === 0 ? (
          <p className="text-sm text-slate-500">작성한 글이 없습니다.</p>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-slate-100 text-left">
                <th className="border border-slate-300 px-3 py-2">제목</th>
                <th className="border border-slate-300 px-3 py-2">유형</th>
                <th className="border border-slate-300 px-3 py-2">작성일</th>
              </tr>
            </thead>
            <tbody>
              {boards.items.map((b) => (
                <tr key={b.id} className="hover:bg-slate-50">
                  <td className="border border-slate-300 px-3 py-2">
                    <Link
                      href={boardHref(b.boardType, b.id)}
                      className="font-medium text-finsight-primary hover:underline"
                    >
                      {b.title}
                    </Link>
                  </td>
                  <td className="border border-slate-300 px-3 py-2">{b.boardType}</td>
                  <td className="border border-slate-300 px-3 py-2">
                    {formatBoardDate(b.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      {!loading && tab === "comments" ? (
        comments.items.length === 0 ? (
          <p className="text-sm text-slate-500">작성한 댓글이 없습니다.</p>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-slate-100 text-left">
                <th className="border border-slate-300 px-3 py-2">내용</th>
                <th className="border border-slate-300 px-3 py-2">작성일</th>
                <th className="border border-slate-300 px-3 py-2">이동</th>
              </tr>
            </thead>
            <tbody>
              {comments.items.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50">
                  <td className="border border-slate-300 px-3 py-2 whitespace-pre-wrap">
                    {c.content}
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {c.createdAt ? formatBoardDate(c.createdAt) : "-"}
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {c.commentType === "BOARD" && c.targetId ? (
                      <Link
                        href={boardHref(c.boardType, c.targetId)}
                        className="text-finsight-primary hover:underline"
                      >
                        글 보기
                      </Link>
                    ) : (
                      "-"
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      {!loading && tab === "boardReactions" ? (
        boardReactions.items.length === 0 ? (
          <p className="text-sm text-slate-500">글에 남긴 반응 기록이 없습니다.</p>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-slate-100 text-left">
                <th className="border border-slate-300 px-3 py-2">글 제목</th>
                <th className="border border-slate-300 px-3 py-2">반응</th>
                <th className="border border-slate-300 px-3 py-2">일시</th>
              </tr>
            </thead>
            <tbody>
              {boardReactions.items.map((r, idx) => (
                <tr key={`br-${r.boardId}-${idx}`} className="hover:bg-slate-50">
                  <td className="border border-slate-300 px-3 py-2">
                    <Link
                      href={boardHref(r.boardType, r.boardId)}
                      className="font-medium text-finsight-primary hover:underline"
                    >
                      {r.title}
                    </Link>
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {reactionLabel(r.reactionType)}
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {r.createdAt ? formatBoardDate(r.createdAt) : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      {!loading && tab === "commentReactions" ? (
        commentReactions.items.length === 0 ? (
          <p className="text-sm text-slate-500">댓글에 남긴 반응 기록이 없습니다.</p>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="bg-slate-100 text-left">
                <th className="border border-slate-300 px-3 py-2">댓글 내용</th>
                <th className="border border-slate-300 px-3 py-2">반응</th>
                <th className="border border-slate-300 px-3 py-2">일시</th>
                <th className="border border-slate-300 px-3 py-2">이동</th>
              </tr>
            </thead>
            <tbody>
              {commentReactions.items.map((r, idx) => (
                <tr key={`cr-${r.commentId}-${idx}`} className="hover:bg-slate-50">
                  <td className="border border-slate-300 px-3 py-2">{r.content}</td>
                  <td className="border border-slate-300 px-3 py-2">
                    {reactionLabel(r.reactionType)}
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {r.createdAt ? formatBoardDate(r.createdAt) : "-"}
                  </td>
                  <td className="border border-slate-300 px-3 py-2">
                    {r.targetId ? (
                      <Link
                        href={boardHref(r.boardType, r.targetId)}
                        className="text-finsight-primary hover:underline"
                      >
                        글 보기
                      </Link>
                    ) : (
                      "-"
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      ) : null}

      <div className="flex items-center justify-between gap-3 text-sm">
        <button
          type="button"
          className={squareBtn}
          disabled={page <= 0 || loading}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          이전
        </button>
        <span className="text-slate-600">
          {page + 1} / {currentTotalPages}
        </span>
        <button
          type="button"
          className={squareBtn}
          disabled={page + 1 >= currentTotalPages || loading}
          onClick={() => setPage((p) => p + 1)}
        >
          다음
        </button>
      </div>
    </div>
  )
}
