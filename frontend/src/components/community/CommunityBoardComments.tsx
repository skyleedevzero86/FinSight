"use client"

import type { FormEvent } from "react"
import { useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  createBoardComment,
  dislikeBoardComment,
  fetchBoardComments,
  deleteBoardComment,
  likeBoardComment,
  type BoardComment,
} from "@/lib/boardComments"
import type { BoardTypeCode } from "@/lib/boardApi"
import { formatAuthor } from "@/lib/boardApi"

function formatTime(value: string | null): string | null {
  if (!value) return null
  return value.replace("T", " ").slice(0, 16)
}

function GuestCommentPlaceholders({ count }: { count: number }) {
  const n = Math.max(2, Math.min(4, count || 2))
  return (
    <ul className="fcb-comment-list fcb-comment-list--placeholders" aria-hidden>
      {Array.from({ length: n }, (_, i) => (
        <li key={i} className="fcb-comment-item fcb-comment-item--placeholder">
          <div className="fcb-comment-meta">
            <strong>회원{i + 1}</strong>
            <span>2026-01-01 12:00</span>
          </div>
          <p className="fcb-comment-body">
            로그인 후 확인할 수 있는 댓글 미리보기 텍스트입니다. 실제 내용은 가려져 있습니다.
          </p>
        </li>
      ))}
    </ul>
  )
}

export default function CommunityBoardComments({
  boardId,
  boardAuthorEmail,
  boardType,
  commentsAuthorOnly = false,
  initialCommentCount = 0,
  onCountChange,
}: {
  boardId: number
  boardAuthorEmail?: string
  boardType?: BoardTypeCode
  commentsAuthorOnly?: boolean
  initialCommentCount?: number
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
  const [deletingCommentId, setDeletingCommentId] = useState<number | null>(null)
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
      const message = e instanceof Error ? e.message : "댓글을 불러오지 못했습니다."
      const authLike =
        /인증|로그인|Unauthorized|401/i.test(message)
      if (!authLike) setError(message)
      setComments([])
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

  const role = (user?.role ?? "").toUpperCase().replace(/^ROLE_/, "")
  const isStaff = role === "ADMIN" || role === "MANAGER"
  const isBoardAuthor =
    !!user?.email &&
    !!boardAuthorEmail &&
    user.email.toLowerCase() === boardAuthorEmail.toLowerCase()
  const restrictWrite = commentsAuthorOnly || boardType === "QNA"
  const canWriteComment = !!user && (!restrictWrite || isBoardAuthor || isStaff)
  const isGuest = ready && !user
  const commentsLocked = !ready || !user

  const onSubmitRoot = async (e: FormEvent) => {
    e.preventDefault()
    if (!ready || submitting || !canWriteComment) return
    const text = content.trim()
    if (!text) return
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
    if (!ready || submitting || !replyTo || !canWriteComment) return
    const text = replyContent.trim()
    if (!text) return
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

  const loadedTotal = comments.reduce((sum, c) => sum + 1 + (c.replies?.length ?? 0), 0)
  const total = loadedTotal > 0 ? loadedTotal : initialCommentCount

  const canDeleteComment = (c: BoardComment) => {
    if (!user) return false
    const userRole = (user.role ?? "").toUpperCase().replace(/^ROLE_/, "")
    const isAdminOrManager = userRole === "ADMIN" || userRole === "MANAGER"
    const isAuthor = c.authorEmail?.toLowerCase() === user.email?.toLowerCase()
    return isAdminOrManager || isAuthor
  }

  const onDeleteComment = async (commentId: number) => {
    if (!ready) return
    if (!user) {
      setLoginPromptOpen(true)
      return
    }

    setDeletingCommentId(commentId)
    setError(null)

    try {
      await deleteBoardComment(commentId)
      await load()
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "댓글 삭제에 실패했습니다."
      setError(message)
    } finally {
      setDeletingCommentId(null)
    }
  }

  const showRealComments = comments.length > 0
  const showPlaceholders = commentsLocked && !showRealComments

  return (
    <section className="fcb-comments">
      <div className="fcb-comments__heading">
        <h3>댓글 {total}</h3>
      </div>

      {canWriteComment ? (
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
      ) : restrictWrite && user ? (
        <p className="fcb-comment-notice">
          Q&amp;A 댓글·답글은 <strong>게시글 작성자</strong>와 <strong>관리자</strong>만 남길 수 있습니다.
        </p>
      ) : isGuest ? (
        <p className="fcb-comment-notice">로그인 후 댓글을 확인할 수 있습니다.</p>
      ) : null}

      {error && !isGuest ? <p className="fcb-comment-error">{error}</p> : null}
      {loading ? <p className="text-sm text-gray-500">댓글 불러오는 중…</p> : null}

      <div
        className={
          commentsLocked
            ? "fcb-comment-list-wrap fcb-comment-list-wrap--locked"
            : "fcb-comment-list-wrap"
        }
      >
        {isGuest ? (
          <div className="fcb-comment-list-wrap__overlay">
            <p>로그인하면 댓글 내용을 볼 수 있어요</p>
            <button type="button" onClick={() => router.push(loginNext())}>
              로그인
            </button>
          </div>
        ) : null}

        {showPlaceholders ? (
          <GuestCommentPlaceholders count={initialCommentCount || total} />
        ) : (
          <ul className="fcb-comment-list">
            {comments.map((comment) => (
              <li key={comment.id} className="fcb-comment-item">
                <div className="fcb-comment-meta">
                  <strong>{formatAuthor(comment.authorEmail)}</strong>
                  <span>{formatTime(comment.createdAt)}</span>
                </div>
                <p className="fcb-comment-body">{comment.content}</p>
                <div className="fcb-comment-toolbar">
                  <div className="fcb-comment-toolbar__left">
                    {canWriteComment ? (
                      <button type="button" onClick={() => setReplyTo(comment)}>
                        답글
                      </button>
                    ) : null}
                    {canDeleteComment(comment) ? (
                      <button
                        type="button"
                        className="fcb-comment-delete"
                        disabled={deletingCommentId === comment.id}
                        onClick={() => void onDeleteComment(comment.id)}
                      >
                        삭제
                      </button>
                    ) : null}
                  </div>
                  <div className="fcb-comment-toolbar__reactions">
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
                </div>

                {replyTo?.id === comment.id ? (
                  <form
                    className="fcb-comment-form fcb-comment-form--reply"
                    onSubmit={(e) => void onSubmitReply(e)}
                  >
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
                          <div className="fcb-comment-toolbar__left">
                            {canDeleteComment(reply) ? (
                              <button
                                type="button"
                                className="fcb-comment-delete"
                                disabled={deletingCommentId === reply.id}
                                onClick={() => void onDeleteComment(reply.id)}
                              >
                                삭제
                              </button>
                            ) : null}
                          </div>
                          <div className="fcb-comment-toolbar__reactions">
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
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

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
