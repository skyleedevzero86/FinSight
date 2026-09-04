"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { unwrapApiData, formatBoardDate, type BoardListItem } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"

type CommentItem = {
  id: number
  content: string
  targetId: number
  commentType: string
  createdAt: string | null
}

type ReactionItem = {
  boardId: number
  title: string
  boardType: string | null
  reactionType: string | null
  createdAt: string | null
}

function boardHref(boardType: string | null | undefined, id: number): string {
  if (boardType === "NOTICE") return `/community/notice/${id}`
  if (boardType === "QNA") return `/community/qna/${id}`
  return `/community/qna/${id}`
}

export default function MyPostsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [boards, setBoards] = useState<BoardListItem[]>([])
  const [comments, setComments] = useState<CommentItem[]>([])
  const [reactions, setReactions] = useState<ReactionItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/my/posts")}`)
      return
    }
    let cancelled = false
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const headers = { Accept: "application/json", ...authHeadersJson() }
        const [bRes, cRes, rRes] = await Promise.all([
          fetch("/api/v1/boards/my-boards?page=0&size=30", { headers, cache: "no-store" }),
          fetch("/api/v1/comments/my-comments?page=0&size=30", { headers, cache: "no-store" }),
          fetch("/api/v1/boards/my-reactions?page=0&size=30", { headers, cache: "no-store" }),
        ])
        if (cancelled) return
        if (!bRes.ok || !cRes.ok || !rRes.ok) {
          setError("활동 기록을 불러오지 못했습니다.")
          return
        }
        const [bJson, cJson, rJson] = await Promise.all([bRes.json(), cRes.json(), rRes.json()])
        const boardData = unwrapApiData<BoardListItem[]>(bJson) ?? []
        const commentData = unwrapApiData<CommentItem[]>(cJson) ?? []
        const reactionData = unwrapApiData<ReactionItem[]>(rJson) ?? []
        setBoards(Array.isArray(boardData) ? boardData : [])
        setComments(Array.isArray(commentData) ? commentData : [])
        setReactions(Array.isArray(reactionData) ? reactionData : [])
      } catch {
        if (!cancelled) setError("활동 기록을 불러오지 못했습니다.")
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [ready, user, router])

  if (!ready || !user) {
    return <p className="text-sm text-slate-500">불러오는 중…</p>
  }

  return (
    <div className="mx-auto max-w-3xl space-y-8 px-4 py-8">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">나의 게시글</h1>
        <p className="mt-1 text-sm text-slate-600">내가 쓴 글·댓글·좋아요 기록을 확인합니다.</p>
      </header>

      {error ? (
        <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      ) : null}
      {loading ? <p className="text-sm text-slate-500">불러오는 중…</p> : null}

      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-800">작성한 글</h2>
        {boards.length === 0 ? (
          <p className="text-sm text-slate-500">작성한 글이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
            {boards.map((b) => (
              <li key={b.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
                <Link
                  href={boardHref(b.boardType, b.id)}
                  className="font-medium text-finsight-primary hover:underline"
                >
                  {b.title}
                </Link>
                <span className="text-xs text-slate-500">
                  {b.boardType} · {formatBoardDate(b.createdAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-800">작성한 댓글</h2>
        {comments.length === 0 ? (
          <p className="text-sm text-slate-500">작성한 댓글이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
            {comments.map((c) => (
              <li key={c.id} className="px-4 py-3">
                <p className="text-sm text-slate-800 whitespace-pre-wrap">{c.content}</p>
                <div className="mt-1 flex flex-wrap gap-2 text-xs text-slate-500">
                  <span>{c.createdAt ? formatBoardDate(c.createdAt) : ""}</span>
                  {c.commentType === "BOARD" ? (
                    <Link href={`/community/qna/${c.targetId}`} className="text-finsight-primary hover:underline">
                      글 보기
                    </Link>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-lg font-semibold text-slate-800">좋아요·싫어요</h2>
        {reactions.length === 0 ? (
          <p className="text-sm text-slate-500">반응 기록이 없습니다.</p>
        ) : (
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
            {reactions.map((r, idx) => (
              <li key={`${r.boardId}-${idx}`} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
                <Link
                  href={boardHref(r.boardType, r.boardId)}
                  className="font-medium text-finsight-primary hover:underline"
                >
                  {r.title}
                </Link>
                <span className="text-xs text-slate-500">
                  {r.reactionType === "LIKE" ? "좋아요" : r.reactionType === "DISLIKE" ? "싫어요" : r.reactionType}
                  {r.createdAt ? ` · ${formatBoardDate(r.createdAt)}` : ""}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
