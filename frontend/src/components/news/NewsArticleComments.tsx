"use client"

import Link from "next/link"
import { FormEvent, useEffect, useState } from "react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { createNewsComment, fetchNewsComments, type NewsComment } from "@/lib/newsComments"
import { fetchNewsReaction, toggleNewsReaction, type NewsReaction } from "@/lib/newsReactions"

function commentTime(value: string | null): string {
  if (!value) return ""
  const date = new Date(value.includes("T") ? value : value.replace(" ", "T"))
  if (Number.isNaN(date.getTime())) return ""
  const pad = (part: number) => String(part).padStart(2, "0")
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function CommentItem({ comment }: { comment: NewsComment }) {
  if (comment.status === "DELETED") return null
  return (
    <li className="border-b border-gray-100 py-3">
      <p className="text-xs text-gray-500">
        <span className="inline-block select-none blur-[6px]" aria-label="작성자 비공개">
          작성자
        </span>
        <span className="ml-2">{commentTime(comment.createdAt)}</span>
      </p>
      <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-gray-900">{comment.content}</p>
      {comment.replies.length > 0 ? (
        <ul className="mt-2 border-l border-gray-200 pl-3">
          {comment.replies.map((reply) => (
            <CommentItem key={reply.id} comment={reply} />
          ))}
        </ul>
      ) : null}
    </li>
  )
}

export default function NewsArticleComments({ newsId }: { newsId: string }) {
  const { hasToken } = useAuthSession()
  const [comments, setComments] = useState<NewsComment[]>([])
  const [reaction, setReaction] = useState<NewsReaction>({ myReaction: null, likeCount: 0, dislikeCount: 0 })
  const [content, setContent] = useState("")
  const [error, setError] = useState("")
  const [posting, setPosting] = useState(false)
  const [reacting, setReacting] = useState(false)
  const loginHref = `/login?next=${encodeURIComponent(`/news/${newsId}`)}`

  useEffect(() => {
    let alive = true
    fetchNewsComments(Number(newsId))
      .then((rows) => {
        if (alive) setComments(rows)
      })
      .catch((reason: unknown) => {
        if (!alive) return
        const message = reason instanceof Error ? reason.message : "댓글을 불러오지 못했습니다."
        if (message.includes("인증이 필요")) return
        setError(message)
      })
    fetchNewsReaction(Number(newsId)).then((row) => {
      if (alive) setReaction(row)
    })
    return () => {
      alive = false
    }
  }, [newsId])

  async function onReaction(next: "LIKE" | "DISLIKE") {
    if (!hasToken) {
      window.location.href = loginHref
      return
    }
    setReacting(true)
    setError("")
    try {
      setReaction(await toggleNewsReaction(Number(newsId), next))
    } catch (reason: unknown) {
      const message = reason instanceof Error ? reason.message : "반응 저장에 실패했습니다."
      if (message.includes("로그인") || message.includes("인증이 필요")) {
        window.location.href = loginHref
        return
      }
      setError(message)
    } finally {
      setReacting(false)
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const next = content.trim()
    if (!next) {
      setError("댓글 내용을 입력해 주세요.")
      return
    }
    setPosting(true)
    setError("")
    try {
      await createNewsComment(Number(newsId), next)
      setContent("")
      setComments(await fetchNewsComments(Number(newsId)))
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "댓글 등록에 실패했습니다.")
    } finally {
      setPosting(false)
    }
  }

  return (
    <section className="mt-8 border-t border-gray-200 pt-6">
      <div className="flex items-center gap-3">
        <h2 className="text-lg font-semibold text-gray-900">댓글</h2>
        <span className="text-lg font-semibold text-gray-900">{comments.length}</span>
        <div className="ml-auto flex items-center gap-2" role="group" aria-label="좋아요 싫어요">
          <button
            type="button"
            disabled={reacting}
            aria-pressed={reaction.myReaction === "LIKE"}
            className={`rounded border px-2 py-1 text-sm ${reaction.myReaction === "LIKE" ? "border-[#1f444b] bg-[#1f444b] text-white" : "border-gray-300 text-gray-800"}`}
            onClick={() => void onReaction("LIKE")}
          >
            좋아요 {reaction.likeCount}
          </button>
          <button
            type="button"
            disabled={reacting}
            aria-pressed={reaction.myReaction === "DISLIKE"}
            className={`rounded border px-2 py-1 text-sm ${reaction.myReaction === "DISLIKE" ? "border-[#1f444b] bg-[#1f444b] text-white" : "border-gray-300 text-gray-800"}`}
            onClick={() => void onReaction("DISLIKE")}
          >
            싫어요 {reaction.dislikeCount}
          </button>
        </div>
      </div>
      {hasToken ? (
        <form className="mt-3" onSubmit={onSubmit}>
          <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={1000}
            rows={3}
            placeholder="댓글을 입력해 주세요"
            className="w-full resize-y rounded border border-gray-300 px-3 py-2 text-sm"
          />
          <button
            type="submit"
            disabled={posting}
            className="mt-2 rounded bg-[#1f444b] px-4 py-2 text-sm text-white disabled:opacity-60"
          >
            등록
          </button>
        </form>
      ) : (
        <p className="mt-3 text-center text-sm text-gray-700">
          로그인 후 댓글을 작성할 수 있습니다.{" "}
          <Link href={loginHref} className="text-sky-700 underline">
            로그인
          </Link>
        </p>
      )}
      {error ? <p className="mt-2 text-center text-sm text-red-700">{error}</p> : null}
      {comments.length === 0 ? (
        <p className="mt-4 text-center text-sm text-gray-500">아직 댓글이 없습니다.</p>
      ) : (
        <ul className="mt-4">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}
        </ul>
      )}
    </section>
  )
}
