"use client"

import { FormEvent, useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  createLiveVodComment,
  fetchLiveVodComments,
  fetchLiveVodEngagement,
  fetchLiveVodReplies,
  REPLY_PAGE_SIZE,
  ROOT_COMMENT_PAGE_SIZE,
  toggleLiveVodReactionApi,
  type LiveVodComment,
} from "@/lib/liveVodEngagement"

function formatTime(value: string | null): string | null {
  if (!value) return null
  return value.replace("T", " ").slice(0, 16)
}

export default function LiveVodComments({
  videoId,
  onCountChange,
}: {
  videoId: string
  onCountChange?: (count: number) => void
}) {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [comments, setComments] = useState<LiveVodComment[]>([])
  const [page, setPage] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [totalComments, setTotalComments] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [content, setContent] = useState("")
  const [replyContent, setReplyContent] = useState("")
  const [replyTo, setReplyTo] = useState<LiveVodComment | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [dislikeCount, setDislikeCount] = useState(0)
  const [myReaction, setMyReaction] = useState<"LIKE" | "DISLIKE" | null>(null)
  const [reactionBusy, setReactionBusy] = useState(false)
  const [replyBusyId, setReplyBusyId] = useState<number | null>(null)
  const rootTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const replyTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)
  const loadingMoreRef = useRef(false)

  const loginNext = () => {
    if (typeof window === "undefined") return "/login"
    return `/login?next=${encodeURIComponent(window.location.pathname + window.location.search)}`
  }

  const openLoginPrompt = () => setLoginPromptOpen(true)

  const goLogin = () => {
    setLoginPromptOpen(false)
    router.push(loginNext())
  }

  const loadPage = useCallback(
    async (nextPage: number, append: boolean) => {
      if (append) {
        if (loadingMoreRef.current) return
        loadingMoreRef.current = true
        setLoadingMore(true)
      } else {
        setLoading(true)
      }
      setError(null)
      try {
        const result = await fetchLiveVodComments(videoId, nextPage, ROOT_COMMENT_PAGE_SIZE)
        setComments((prev) => (append ? [...prev, ...result.items] : result.items))
        setPage(result.page)
        setHasNext(result.hasNext)
        setTotalComments(result.totalComments)
        onCountChange?.(result.totalComments)
      } catch (e) {
        setError(e instanceof Error ? e.message : "댓글을 불러오지 못했습니다.")
      } finally {
        setLoading(false)
        setLoadingMore(false)
        loadingMoreRef.current = false
      }
    },
    [onCountChange, videoId],
  )

  const reloadReactions = async () => {
    try {
      const eng = await fetchLiveVodEngagement(videoId)
      setLikeCount(eng.likeCount)
      setDislikeCount(eng.dislikeCount)
      setMyReaction(eng.myReaction)
      setTotalComments(eng.commentCount)
      onCountChange?.(eng.commentCount)
    } catch {
      /* ignore */
    }
  }

  useEffect(() => {
    setComments([])
    setPage(0)
    setHasNext(false)
    void loadPage(0, false)
    void reloadReactions()
  }, [videoId, user?.email, loadPage])

  useEffect(() => {
    if (!replyTo) return
    const el = replyTextareaRef.current
    if (!el) return
    el.focus()
    el.scrollIntoView({ behavior: "smooth", block: "center" })
  }, [replyTo?.id])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node) return
    const observer = new IntersectionObserver(
      (entries) => {
        const hit = entries.some((e) => e.isIntersecting)
        if (!hit || !hasNext || loading || loadingMoreRef.current) return
        void loadPage(page + 1, true)
      },
      { root: null, rootMargin: "160px 0px", threshold: 0 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [hasNext, loading, loadPage, page])

  const onSubmitRoot = async (e: FormEvent) => {
    e.preventDefault()
    if (!ready || submitting) return
    const text = content.trim()
    if (!text) return
    if (!user) {
      openLoginPrompt()
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await createLiveVodComment(videoId, text, null)
      setContent("")
      await loadPage(0, false)
      await reloadReactions()
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        openLoginPrompt()
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
      openLoginPrompt()
      return
    }
    const parentId = replyTo.id
    setSubmitting(true)
    setError(null)
    try {
      await createLiveVodComment(videoId, text, parentId)
      setReplyContent("")
      setReplyTo(null)
      const replyPage = await fetchLiveVodReplies(videoId, parentId, 0, REPLY_PAGE_SIZE)
      setComments((prev) =>
        prev.map((c) =>
          c.id === parentId
            ? {
                ...c,
                replies: replyPage.items,
                replyCount: replyPage.totalElements,
                replyPage: replyPage.page,
                replyTotalPages: replyPage.totalPages,
              }
            : c,
        ),
      )
      await reloadReactions()
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        openLoginPrompt()
        return
      }
      setError(message || "답글 등록에 실패했습니다.")
    } finally {
      setSubmitting(false)
    }
  }

  const startReply = (comment: LiveVodComment) => {
    if (!ready) return
    if (!user) {
      openLoginPrompt()
      return
    }
    setReplyTo(comment)
    setReplyContent("")
    setError(null)
  }

  const changeReplyPage = async (comment: LiveVodComment, nextPage: number) => {
    if (replyBusyId != null) return
    if (nextPage < 0 || (comment.replyTotalPages > 0 && nextPage >= comment.replyTotalPages)) return
    setReplyBusyId(comment.id)
    try {
      const replyPage = await fetchLiveVodReplies(videoId, comment.id, nextPage, REPLY_PAGE_SIZE)
      setComments((prev) =>
        prev.map((c) =>
          c.id === comment.id
            ? {
                ...c,
                replies: replyPage.items,
                replyCount: replyPage.totalElements,
                replyPage: replyPage.page,
                replyTotalPages: replyPage.totalPages,
              }
            : c,
        ),
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : "대댓글을 불러오지 못했습니다.")
    } finally {
      setReplyBusyId(null)
    }
  }

  const onReaction = async (reaction: "LIKE" | "DISLIKE") => {
    if (!ready || reactionBusy) return
    if (!user) {
      openLoginPrompt()
      return
    }
    setReactionBusy(true)
    try {
      const result = await toggleLiveVodReactionApi(videoId, reaction)
      setMyReaction(result.myReaction)
      setLikeCount(result.likeCount)
      setDislikeCount(result.dislikeCount)
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        openLoginPrompt()
        return
      }
      setError(message || "반응 저장에 실패했습니다.")
    } finally {
      setReactionBusy(false)
    }
  }

  return (
    <section className="flv-comments">
      <div className="flv-comments-heading">
        <h2>댓글 {totalComments}</h2>
        <div className="flv-reaction-row" role="group" aria-label="좋아요 싫어요">
          <button
            type="button"
            className={`flv-reaction-btn${myReaction === "LIKE" ? " is-on" : ""}`}
            disabled={reactionBusy}
            aria-pressed={myReaction === "LIKE"}
            onClick={() => void onReaction("LIKE")}
          >
            좋아요 {likeCount}
          </button>
          <button
            type="button"
            className={`flv-reaction-btn${myReaction === "DISLIKE" ? " is-on" : ""}`}
            disabled={reactionBusy}
            aria-pressed={myReaction === "DISLIKE"}
            onClick={() => void onReaction("DISLIKE")}
          >
            싫어요 {dislikeCount}
          </button>
        </div>
      </div>

      <form className="flv-comment-form" onSubmit={(e) => void onSubmitRoot(e)}>
        <textarea
          ref={rootTextareaRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="댓글을 입력하세요"
          rows={3}
          maxLength={2000}
        />
        <div className="flv-comment-actions">
          <button type="submit" disabled={submitting || !content.trim()}>
            댓글 등록
          </button>
        </div>
      </form>

      {error ? <p className="flv-comment-error">{error}</p> : null}
      {loading ? <p className="text-sm text-gray-500">댓글 불러오는 중…</p> : null}

      <ul className="flv-comment-list">
        {comments.map((c) => (
          <li key={c.id} className="flv-comment-item">
            <div className="flv-comment-head">
              <strong>{c.authorNickname || c.userEmail}</strong>
              {formatTime(c.createdAt) ? <time>{formatTime(c.createdAt)}</time> : null}
            </div>
            <p>{c.content}</p>
            <button type="button" className="flv-reply-btn" onClick={() => startReply(c)}>
              답글{c.replyCount > 0 ? ` ${c.replyCount}` : ""}
            </button>

            {replyTo?.id === c.id ? (
              <form className="flv-inline-reply" onSubmit={(e) => void onSubmitReply(e)}>
                <p className="flv-reply-hint">
                  {c.authorNickname || c.userEmail} 님에게 답글{" "}
                  <button
                    type="button"
                    onClick={() => {
                      setReplyTo(null)
                      setReplyContent("")
                    }}
                  >
                    취소
                  </button>
                </p>
                <textarea
                  ref={replyTextareaRef}
                  value={replyContent}
                  onChange={(e) => setReplyContent(e.target.value)}
                  placeholder="답글을 입력하세요"
                  rows={2}
                  maxLength={2000}
                />
                <div className="flv-comment-actions">
                  <button type="submit" disabled={submitting || !replyContent.trim()}>
                    답글 등록
                  </button>
                </div>
              </form>
            ) : null}

            {c.replyCount > 0 ? (
              <div className="flv-reply-block">
                <ul className="flv-reply-list">
                  {c.replies.map((r) => (
                    <li key={r.id}>
                      <div className="flv-comment-head">
                        <strong>{r.authorNickname || r.userEmail}</strong>
                        {formatTime(r.createdAt) ? <time>{formatTime(r.createdAt)}</time> : null}
                      </div>
                      <p>{r.content}</p>
                    </li>
                  ))}
                </ul>
                {c.replyTotalPages > 1 ? (
                  <div className="flv-reply-pager" role="navigation" aria-label="대댓글 페이지">
                    <button
                      type="button"
                      className="flv-reply-page-btn"
                      disabled={c.replyPage <= 0 || replyBusyId === c.id}
                      aria-label="이전 대댓글"
                      onClick={() => void changeReplyPage(c, c.replyPage - 1)}
                    >
                      ‹
                    </button>
                    <span className="flv-reply-page-indicator">
                      {c.replyPage + 1} / {c.replyTotalPages}
                    </span>
                    <button
                      type="button"
                      className="flv-reply-page-btn"
                      disabled={c.replyPage >= c.replyTotalPages - 1 || replyBusyId === c.id}
                      aria-label="다음 대댓글"
                      onClick={() => void changeReplyPage(c, c.replyPage + 1)}
                    >
                      ›
                    </button>
                  </div>
                ) : null}
              </div>
            ) : null}
          </li>
        ))}
      </ul>

      <div ref={sentinelRef} className="flv-comment-sentinel" aria-hidden />
      {loadingMore ? <p className="flv-comment-more">댓글 더 불러오는 중…</p> : null}
      {!loading && !hasNext && comments.length > 0 ? (
        <p className="flv-comment-end">댓글을 모두 불러왔습니다.</p>
      ) : null}

      {loginPromptOpen ? (
        <div className="flv-login-prompt" role="dialog" aria-modal="true" aria-labelledby="flv-login-prompt-title">
          <div className="flv-login-prompt-card">
            <p id="flv-login-prompt-title">로그인해야 글을 남길 수 있습니다.</p>
            <div className="flv-login-prompt-actions">
              <button type="button" className="flv-login-prompt-ok" onClick={goLogin}>
                확인
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
