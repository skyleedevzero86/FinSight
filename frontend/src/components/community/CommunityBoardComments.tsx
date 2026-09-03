"use client"

import type { FormEvent } from "react"
import { useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  createBoardComment,
  dislikeBoardComment,
  fetchBoardComments,
  likeBoardComment,
  type BoardComment,
} from "@/lib/boardComments"
import { formatAuthor } from "@/lib/boardApi"

function formatTime(value: string | null): string | null {
  if (!value) return null
  return value.replace("T", " ").slice(0, 16)
}

export default function CommunityBoardComments({
  boardId,
  onCountChange,
}: {
  boardId: number
  onCountChange?: (count: number) => void
}) {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [comments, setComments] = useState<BoardComment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [content, setContent] = useState("")
  const [replyContent, setReplyContent] = useState("")
  const [replyTo, setReplyTo] = useState<BoardComment | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const [reactionBusyId, setReactionBusyId] = useState<number | null>(null)
  const replyTextareaRef = useRef<HTMLTextAreaElement | null>(null)

  const loginNext = () => {
    if (typeof window === "undefined") return "/login"
    return `/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`
  }

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const items = await fetchBoardComments(boardId, 0, 50)
      setComments(items)
      const total = items.reduce((sum, c) => sum + 1 + (c.replies?.length ?? 0), 0)
      onCountChange?.(total)
    } catch (e) {
      setError(e instanceof Error ? e.message : "댓글을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [boardId, onCountChange])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!replyTo) return
    replyTextareaRef.current?.focus()
  }, [replyTo?.id])

  const onSubmitRoot = async (e: FormEvent) => {
    e.preventDefault()
    if (!ready || submitting) return
    const text = content.trim()
    if (!text) return
    if (!user) {
      setLoginPromptOpen(true)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await createBoardComment(boardId, text, null)
      setContent("")
      await load()
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        setLoginPromptOpen(true)
        return
      }
      setError(message || "댓글 등록에 실패했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  const onSubmitReply = async (e: FormEvent) => {
    e.preventDefault()
    if (!ready || submitting || !replyTo) return
    const text = replyContent.trim()
    if (!text) return
    if (!user) {
      setLoginPromptOpen(true)
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await createBoardComment(boardId, text, replyTo.id)
      setReplyContent("")
      setReplyTo(null)
      await load()
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        setLoginPromptOpen(true)
        return
      }
      setError(message || "답글 등록에 실패했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  const onReaction = async (comment: BoardComment, kind: "LIKE" | "DISLIKE") => {
    if (!ready || reactionBusyId === comment.id) return
    if (!user) {
      setLoginPromptOpen(true)
      return
    }
    setReactionBusyId(comment.id)
    try {
      if (kind === "LIKE") await likeBoardComment(comment.id)
      else await dislikeBoardComment(comment.id)
      await load()
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        setLoginPromptOpen(true)
        return
      }
      setError(message || "반응 저장에 실패했습니다.")
    } finally {
      setReactionBusyId(null)
    }
  }

  const total = comments.reduce((sum, c) => sum + 1 + (c.replies?.length ?? 0), 0)

  return (
    <section className="fcb-comments">
      <div className="fcb-comments__heading">
        <h3>댓글 {total}</h3>
      </div>

      <form className="fcb-comment-form" onSubmit={(e) => void onSubmitRoot(e)}>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="댓글을 입력하세요"
          rows={3}
          maxLength={2000}
        />
        <div className="fcb-comment-actions">
          <button type="submit" disabled={submitting || !content.trim()}>
            댓글 등록
          </button>
        </div>
      </form>

      {error ? <p className="fcb-comment-error">{error}</p> : null}
      {loading ? <p className="text-sm text-gray-500">댓글 불러오는 중…</p> : null}

      <ul className="fcb-comment-list">
        {comments.map((comment) => (
          <li key={comment.id} className="fcb-comment-item">
            <div className="fcb-comment-meta">
              <strong>{formatAuthor(comment.authorEmail)}</strong>
              <span>{formatTime(comment.createdAt)}</span>
            </div>
            <p className="fcb-comment-body">{comment.content}</p>
            <div className="fcb-comment-toolbar">
              <button type="button" onClick={() => setReplyTo(comment)}>
                답글
              </button>
              <button
                type="button"
                disabled={reactionBusyId === comment.id}
                onClick={() => void onReaction(comment, "LIKE")}
              >
                좋아요 {comment.likeCount}
              </button>
              <button
                type="button"
                disabled={reactionBusyId === comment.id}
                onClick={() => void onReaction(comment, "DISLIKE")}
              >
                싫어요 {comment.dislikeCount}
              </button>
            </div>

            {replyTo?.id === comment.id ? (
              <form className="fcb-comment-form fcb-comment-form--reply" onSubmit={(e) => void onSubmitReply(e)}>
                <textarea
                  ref={replyTextareaRef}
                  value={replyContent}
                  onChange={(e) => setReplyContent(e.target.value)}
                  placeholder="답글을 입력하세요"
                  rows={2}
                  maxLength={2000}
                />
                <div className="fcb-comment-actions">
                  <button type="button" onClick={() => setReplyTo(null)}>
                    취소
                  </button>
                  <button type="submit" disabled={submitting || !replyContent.trim()}>
                    답글 등록
                  </button>
                </div>
              </form>
            ) : null}

            {comment.replies.length > 0 ? (
              <ul className="fcb-comment-replies">
                {comment.replies.map((reply) => (
                  <li key={reply.id} className="fcb-comment-item fcb-comment-item--reply">
                    <div className="fcb-comment-meta">
                      <strong>{formatAuthor(reply.authorEmail)}</strong>
                      <span>{formatTime(reply.createdAt)}</span>
                    </div>
                    <p className="fcb-comment-body">{reply.content}</p>
                    <div className="fcb-comment-toolbar">
                      <button
                        type="button"
                        disabled={reactionBusyId === reply.id}
                        onClick={() => void onReaction(reply, "LIKE")}
                      >
                        좋아요 {reply.likeCount}
                      </button>
                      <button
                        type="button"
                        disabled={reactionBusyId === reply.id}
                        onClick={() => void onReaction(reply, "DISLIKE")}
                      >
                        싫어요 {reply.dislikeCount}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            ) : null}
          </li>
        ))}
      </ul>

      {loginPromptOpen ? (
        <div className="fcb-login-prompt" role="dialog" aria-modal="true">
          <div className="fcb-login-prompt__card">
            <p>댓글을 작성하려면 로그인이 필요합니다.</p>
            <div className="fcb-comment-actions">
              <button type="button" onClick={() => setLoginPromptOpen(false)}>
                닫기
              </button>
              <button type="button" onClick={() => router.push(loginNext())}>
                로그인
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
