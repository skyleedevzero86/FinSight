"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import PortfolioShareSnapshot from "@/components/community/PortfolioShareSnapshot"
import { shareModeration } from "@/components/community/PortfolioShareArticle"
import {
  PORTFOLIO_REPORT_REASONS,
  addPortfolioShareComment,
  fetchPortfolioShare,
  fetchPortfolioShareComments,
  reactToPortfolioShare,
  reportPortfolioShare,
  type PortfolioShareComment,
  type PortfolioShareDetail,
} from "@/lib/portfolioApi"

export default function PortfolioShareDetailClient() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const shareId = Number(params.id)
  const { user } = useAuthSession()
  const [detail, setDetail] = useState<PortfolioShareDetail | null>(null)
  const [comments, setComments] = useState<PortfolioShareComment[]>([])
  const [draft, setDraft] = useState("")
  const [error, setError] = useState("")
  const [commentError, setCommentError] = useState("")
  const [pending, setPending] = useState(false)
  const [reportOpen, setReportOpen] = useState(false)
  const [reportReason, setReportReason] = useState<(typeof PORTFOLIO_REPORT_REASONS)[number]>(PORTFOLIO_REPORT_REASONS[0])
  const [reportNote, setReportNote] = useState("")
  const [reportError, setReportError] = useState("")
  const [reportDone, setReportDone] = useState(false)

  useEffect(() => {
    if (!Number.isFinite(shareId) || shareId <= 0) {
      setError("공유 게시물을 찾을 수 없습니다.")
      return
    }
    let alive = true
    fetchPortfolioShare(shareId)
      .then((next) => {
        if (alive) setDetail(next)
      })
      .catch((cause: unknown) => {
        if (alive) setError(cause instanceof Error ? cause.message : "포트폴리오 공유를 불러오지 못했습니다.")
      })
    fetchPortfolioShareComments(shareId)
      .then((next) => {
        if (alive) setComments(next)
      })
      .catch((cause: unknown) => {
        if (alive) setCommentError(cause instanceof Error ? cause.message : "공유 댓글을 불러오지 못했습니다.")
      })
    return () => {
      alive = false
    }
  }, [shareId])

  async function react(type: "LIKE" | "DISLIKE") {
    if (!user) {
      setError("로그인 후 반응할 수 있습니다.")
      return
    }
    setPending(true)
    setError("")
    try {
      setDetail(await reactToPortfolioShare(shareId, type))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "공유 반응을 저장하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  function openReport() {
    setReportError("")
    setReportDone(false)
    setReportOpen(true)
  }

  async function submitReport() {
    setPending(true)
    setReportError("")
    try {
      setDetail(await reportPortfolioShare(shareId, reportReason, reportNote))
      setReportDone(true)
      setReportNote("")
    } catch (cause) {
      setReportError(cause instanceof Error ? cause.message : "공유 게시물을 신고하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  async function submitComment() {
    setPending(true)
    setError("")
    try {
      setComments(await addPortfolioShareComment(shareId, draft))
      setDraft("")
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "댓글을 등록하지 못했습니다.")
    } finally {
      setPending(false)
    }
  }

  const status = detail ? shareModeration(detail.card) : "OPEN"
  const closed = status === "BLIND" || status === "REMOVED"
  const likes = detail?.card.likeCount ?? 0
  const dislikes = detail?.card.dislikeCount ?? 0
  const warned = status === "WARN"
  const normal = status === "OPEN" && likes >= 10
  const title = detail?.view.goalLabel || detail?.card.goalLabel || "포트폴리오 공유"
  const meta = detail
    ? [detail.card.authorName, `좋아요 ${likes}`, `싫어요 ${dislikes}`, `댓글 ${comments.length}`, detail.card.sharedAt]
        .filter(Boolean)
        .join(" · ")
    : ""

  return (
    <div className="fcb-md-detail">
      {error ? <p className="fcb-comment-error">{error}</p> : null}
      {!detail && !error ? <p className="fcb-comment-notice">공유 내용을 불러오는 중입니다.</p> : null}
      {detail ? (
        <>
          <article className={`fcb-md-preview fcb-md-detail__preview${warned ? " bg-rose-50" : ""}`}>
            <header className="fcb-md-preview__header">
              {normal ? <p className="fcb-md-preview__eyebrow">정상</p> : null}
              {warned ? <p className="fcb-md-preview__eyebrow">위험</p> : null}
              {status === "BLIND" ? <p className="fcb-md-preview__eyebrow">블라인드</p> : null}
              {status === "REMOVED" ? <p className="fcb-md-preview__eyebrow">삭제</p> : null}
              <h2 className="fcb-md-preview__title">{title}</h2>
              <p className="fcb-md-preview__meta">{meta}</p>
            </header>
            {status === "BLIND" ? (
              <div className="fcb-moderation-restricted">
                <div className="fcb-moderation-restricted__banner" role="status">블라인드 처리된 게시물입니다.</div>
              </div>
            ) : null}
            {status === "REMOVED" ? (
              <div className="fcb-moderation-restricted">
                <div className="fcb-moderation-restricted__banner" role="status">삭제된 게시물입니다.</div>
              </div>
            ) : null}
            {closed ? null : <PortfolioShareSnapshot view={detail.view} />}
          </article>
          {closed ? null : (
            <div className="fcb-md-reaction-row" role="group" aria-label="공유 반응">
              <button
                type="button"
                className={`fcb-md-reaction-btn${detail.myReaction === "LIKE" ? " is-on" : ""}`}
                disabled={pending}
                aria-pressed={detail.myReaction === "LIKE"}
                onClick={() => void react("LIKE")}
              >
                ❤️ 좋아요 {likes}
              </button>
              <button
                type="button"
                className={`fcb-md-reaction-btn${detail.myReaction === "DISLIKE" ? " is-on" : ""}`}
                disabled={pending}
                aria-pressed={detail.myReaction === "DISLIKE"}
                onClick={() => void react("DISLIKE")}
              >
                👎 싫어요 {dislikes}
              </button>
              <button
                type="button"
                className="fcb-md-reaction-btn"
                style={{ marginLeft: "auto" }}
                disabled={pending || detail.reportedByMe}
                onClick={openReport}
              >
                🚨 신고
              </button>
            </div>
          )}
          <section className="fcb-comments">
            <div className="fcb-comments__heading">
              <h3>댓글 {comments.length}</h3>
            </div>
            {status === "REMOVED" ? null : user ? (
              <form
                className="fcb-comment-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  void submitComment()
                }}
              >
                <textarea
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  maxLength={500}
                  rows={3}
                  placeholder="댓글을 입력하세요"
                />
                <div className="fcb-comment-actions">
                  <button type="submit" disabled={pending || draft.trim().length === 0}>
                    댓글 등록
                  </button>
                </div>
              </form>
            ) : (
              <p className="fcb-comment-notice">로그인 후 댓글을 남길 수 있습니다.</p>
            )}
            {commentError ? <p className="fcb-comment-error">{commentError}</p> : null}
            {comments.length === 0 && !commentError ? <p className="fcb-comment-notice">아직 댓글이 없습니다.</p> : null}
            <ul className="fcb-comment-list">
              {comments.map((comment) => (
                <li key={comment.id} className="fcb-comment-item">
                  <div className="fcb-comment-meta">
                    <strong>{comment.authorName}</strong>
                    <span>{comment.createdAt}</span>
                  </div>
                  <p className="fcb-comment-body">{comment.content}</p>
                </li>
              ))}
            </ul>
          </section>
          <div className="fcb-md-detail__footer">
            <Link href="/community/free" className="fcb-md-action fcb-md-action--ghost">
              목록
            </Link>
          </div>
        </>
      ) : null}
      {reportOpen ? (
        <div className="fcb-login-prompt" role="dialog" aria-modal="true" aria-label="공유 신고">
          <div className="fcb-login-prompt__card">
            <h3 className="text-sm font-semibold text-gray-900">공유 신고</h3>
            <p className="mt-1 text-xs text-gray-500">종류를 고르고 사유를 입력한 뒤 저장합니다. 허위 신고는 이용 제한 사유가 될 수 있습니다.</p>
            {!user ? (
              <>
                <p className="mt-3">로그인 후 신고할 수 있습니다.</p>
                <div className="fcb-comment-actions">
                  <button type="button" onClick={() => setReportOpen(false)}>닫기</button>
                  <button type="button" onClick={() => router.push(`/login?next=${encodeURIComponent(window.location.pathname)}`)}>
                    로그인
                  </button>
                </div>
              </>
            ) : reportDone ? (
              <>
                <p className="mt-3 text-sm text-emerald-700">신고가 접수되었습니다.</p>
                <div className="fcb-comment-actions mt-3">
                  <button type="button" onClick={() => setReportOpen(false)}>닫기</button>
                </div>
              </>
            ) : (
              <form
                className="mt-3 space-y-3 text-left"
                onSubmit={(event) => {
                  event.preventDefault()
                  void submitReport()
                }}
              >
                {reportError ? <p className="fcb-comment-error">{reportError}</p> : null}
                <label className="block text-xs text-gray-600">
                  신고 종류
                  <select
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm"
                    value={reportReason}
                    onChange={(event) => setReportReason(event.target.value as (typeof PORTFOLIO_REPORT_REASONS)[number])}
                  >
                    {PORTFOLIO_REPORT_REASONS.map((item) => (
                      <option key={item} value={item}>{item}</option>
                    ))}
                  </select>
                </label>
                <label className="block text-xs text-gray-600">
                  사유
                  <textarea
                    className="mt-1 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm"
                    rows={3}
                    maxLength={500}
                    value={reportNote}
                    onChange={(event) => setReportNote(event.target.value)}
                    placeholder="어떤 내용이 문제인지 적어 주세요."
                  />
                </label>
                <div className="fcb-comment-actions">
                  <button type="button" onClick={() => setReportOpen(false)}>취소</button>
                  <button type="submit" disabled={pending || reportNote.trim().length === 0}>신고 저장</button>
                </div>
              </form>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}
