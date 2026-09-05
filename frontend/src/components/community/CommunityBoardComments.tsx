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
  isCommentRestricted,
  likeBoardComment,
  reportBoardComment,
  type BoardComment,
} from "@/lib/boardComments"
import type { BoardTypeCode } from "@/lib/boardApi"
import { formatAuthor } from "@/lib/boardApi"
import { MODERATION_RESTRICTED_MESSAGE, REPORT_REASONS } from "@/lib/boardModeration"

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

function RestrictedCommentBody() {
  return (
    <div className="fcb-moderation-restricted fcb-moderation-restricted--comment">
      <div className="fcb-moderation-restricted__banner" role="status">
        {MODERATION_RESTRICTED_MESSAGE}
      </div>
      <div className="fcb-moderation-restricted__blur">
        <p className="fcb-comment-body">표시할 수 없는 내용입니다.</p>
      </div>
    </div>
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
  const [reportTarget, setReportTarget] = useState<BoardComment | null>(null)
  const [reportReason, setReportReason] = useState<string>(REPORT_REASONS[0].value)
  const [reportDesc, setReportDesc] = useState("")
  const [reportBusy, setReportBusy] = useState(false)
  const [reportError, setReportError] = useState<string | null>(null)
  const [reportDone, setReportDone] = useState(false)
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
      const authLike = /인증|로그인|Unauthorized|401/i.test(message)
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
    if (!ready || reactionBusyId === comment.id || isCommentRestricted(comment.status)) return
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
    if (!user || isCommentRestricted(c.status)) return false
    const userRole = (user.role ?? "").toUpperCase().replace(/^ROLE_/, "")
    const isAdminOrManager = userRole === "ADMIN" || userRole === "MANAGER"
    const isAuthor = c.authorEmail?.toLowerCase() === user.email?.toLowerCase()
    return isAdminOrManager || isAuthor
  }

  const canReportComment = (c: BoardComment) => {
    if (!user || isCommentRestricted(c.status)) return false
    return c.authorEmail?.toLowerCase() !== user.email?.toLowerCase()
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
      const message = err instanceof Error ? err.message : "댓글 삭제에 실패했습니다."
      setError(message)
    } finally {
      setDeletingCommentId(null)
    }
  }

  function openReport(comment: BoardComment) {
    if (!user) {
      setLoginPromptOpen(true)
      return
    }
    setReportTarget(comment)
    setReportReason(REPORT_REASONS[0].value)
    setReportDesc("")
    setReportError(null)
    setReportDone(false)
  }

  async function onSubmitReport(e: FormEvent) {
    e.preventDefault()
    if (!reportTarget || reportBusy) return
    setReportBusy(true)
    setReportError(null)
    try {
      await reportBoardComment(reportTarget.id, reportReason, reportDesc.trim())
      setReportDone(true)
      await load()
    } catch (err) {
      setReportError(err instanceof Error ? err.message : "댓글 신고에 실패했습니다.")
    } finally {
      setReportBusy(false)
    }
  }

  const showRealComments = comments.length > 0
  const showPlaceholders = commentsLocked && !showRealComments

  function renderCommentActions(comment: BoardComment, isReply = false) {
    if (isCommentRestricted(comment.status)) return null
    return (
      <div className="fcb-comment-toolbar">
        <div className="fcb-comment-toolbar__left">
          {!isReply && canWriteComment ? (
            <button type="button" onClick={() => setReplyTo(comment)}>
              답글
            </button>
          ) : null}
          {canReportComment(comment) ? (
            <button type="button" onClick={() => openReport(comment)}>
              신고
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
          {isStaff && comment.reportCount > 0 ? (
            <span className="text-xs text-amber-700">신고 {comment.reportCount}</span>
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
    )
  }

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
                {isCommentRestricted(comment.status) ? (
                  <RestrictedCommentBody />
                ) : (
                  <p className="fcb-comment-body">{comment.content}</p>
                )}
                {renderCommentActions(comment)}

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
                        {isCommentRestricted(reply.status) ? (
                          <RestrictedCommentBody />
                        ) : (
                          <p className="fcb-comment-body">{reply.content}</p>
                        )}
                        {renderCommentActions(reply, true)}
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      {reportTarget ? (
        <div className="fcb-login-prompt" role="dialog" aria-modal="true">
          <div className="fcb-login-prompt__card">
            <h3 className="text-sm font-semibold text-gray-900">댓글 신고</h3>
            <p className="mt-1 text-xs text-gray-500">허위 신고는 이용 제한 사유가 될 수 있습니다.</p>
            {reportError ? (
              <p className="mt-2 text-sm text-red-600">{reportError}</p>
            ) : null}
            {reportDone ? (
              <p className="mt-2 text-sm text-emerald-700">신고가 접수되었습니다.</p>
            ) : (
              <form className="mt-3 space-y-3 text-left" onSubmit={(e) => void onSubmitReport(e)}>
                <label className="block text-xs text-gray-600">
                  사유
                  <select
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm"
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value)}
                  >
                    {REPORT_REASONS.map((r) => (
                      <option key={r.value} value={r.value}>
                        {r.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="block text-xs text-gray-600">
                  상세 설명 (선택)
                  <textarea
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm"
                    rows={3}
                    maxLength={500}
                    value={reportDesc}
                    onChange={(e) => setReportDesc(e.target.value)}
                  />
                </label>
                <div className="fcb-comment-actions">
                  <button type="button" onClick={() => setReportTarget(null)}>
                    취소
                  </button>
                  <button type="submit" disabled={reportBusy}>
                    신고 제출
                  </button>
                </div>
              </form>
            )}
            {reportDone ? (
              <div className="fcb-comment-actions mt-3">
                <button type="button" onClick={() => setReportTarget(null)}>
                  닫기
                </button>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}

      {loginPromptOpen ? (
        <div className="fcb-login-prompt" role="dialog" aria-modal="true">
          <div className="fcb-login-prompt__card">
            <p>댓글을 작성·신고하려면 로그인이 필요합니다.</p>
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
