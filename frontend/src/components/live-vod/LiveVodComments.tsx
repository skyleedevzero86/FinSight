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
  toggleLiveVodCommentReactionApi,
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
  const [replyRootId, setReplyRootId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [dislikeCount, setDislikeCount] = useState(0)
  const [myReaction, setMyReaction] = useState<"LIKE" | "DISLIKE" | null>(null)
  const [reactionBusy, setReactionBusy] = useState(false)
  const [commentReactionBusyId, setCommentReactionBusyId] = useState<number | null>(null)
  const [replyBusyId, setReplyBusyId] = useState<number | null>(null)
  const [expandedReplies, setExpandedReplies] = useState<Record<number, boolean>>({})
  const rootTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const replyTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)
  const loadingMoreRef = useRef(false)
  const hasNextRef = useRef(false)
  const pageRef = useRef(0)

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
        if (loadingMoreRef.current || !hasNextRef.current) return
        loadingMoreRef.current = true
        setLoadingMore(true)
      } else {
        setLoading(true)
      }
      setError(null)
      try {
        const result = await fetchLiveVodComments(videoId, nextPage, ROOT_COMMENT_PAGE_SIZE)
        setComments((prev) => {
          if (!append) return result.items
          const seen = new Set(prev.map((c) => c.id))
          const merged = [...prev]
          for (const item of result.items) {
            if (!seen.has(item.id)) merged.push(item)
          }
          return merged
        })
        setPage(result.page)
        pageRef.current = result.page
        setHasNext(result.hasNext)
        hasNextRef.current = result.hasNext
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
      void 0
    }
  }

  useEffect(() => {
    setComments([])
    setPage(0)
    pageRef.current = 0
    setHasNext(false)
    hasNextRef.current = false
    setExpandedReplies({})
    setReplyTo(null)
    setReplyRootId(null)
    void loadPage(0, false)
    void reloadReactions()
  }, [videoId, user?.email, loadPage])

  useEffect(() => {
    if (!replyTo) return
    const el = replyTextareaRef.current
    if (!el) return
    el.focus()
    el.scrollIntoView({ behavior: "smooth", block: "center" })
  }, [replyTo?.id, replyRootId])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node) return

    const tryLoadMore = () => {
      if (!hasNextRef.current || loadingMoreRef.current) return
      void loadPage(pageRef.current + 1, true).then(() => {
        requestAnimationFrame(() => {
          const rect = node.getBoundingClientRect()
          const stillVisible = rect.top < window.innerHeight + 200
          if (stillVisible && hasNextRef.current) tryLoadMore()
        })
      })
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries.some((e) => e.isIntersecting)) return
        tryLoadMore()
      },
      { root: null, rootMargin: "320px 0px", threshold: 0 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [loadPage, videoId])

  const expandReplies = (commentId: number) => {
    setExpandedReplies((prev) => ({ ...prev, [commentId]: true }))
  }

  const toggleReplies = (commentId: number) => {
    setExpandedReplies((prev) => ({ ...prev, [commentId]: !prev[commentId] }))
  }

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
    if (!ready || submitting || !replyTo || replyRootId == null) return
    const text = replyContent.trim()
    if (!text) return
    if (!user) {
      openLoginPrompt()
      return
    }
    const rootId = replyRootId
    const mentionTarget = replyTo.id !== rootId ? replyTo : null
    setSubmitting(true)
    setError(null)
    try {
      let body = text
      if (mentionTarget) {
        const name = (mentionTarget.authorNickname || mentionTarget.userEmail || "").trim()
        if (name && !body.startsWith("@")) {
          body = `@${name} ${body}`
        }
      }
      await createLiveVodComment(videoId, body, rootId)
      setReplyContent("")
      setReplyTo(null)
      setReplyRootId(null)
      expandReplies(rootId)
      const replyPage = await fetchLiveVodReplies(videoId, rootId, 0, REPLY_PAGE_SIZE)
      setComments((prev) =>
        prev.map((c) =>
          c.id === rootId
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

  const startReply = (root: LiveVodComment, mention?: LiveVodComment) => {
    if (!ready) return
    if (!user) {
      openLoginPrompt()
      return
    }
    const target = mention ?? root
    setReplyRootId(root.id)
    setReplyTo(target)
    setReplyContent("")
    setError(null)
    expandReplies(root.id)
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

  const patchCommentReaction = (
    commentId: number,
    patch: { myReaction: "LIKE" | "DISLIKE" | null; likeCount: number; dislikeCount: number },
  ) => {
    setComments((prev) =>
      prev.map((c) => {
        if (c.id === commentId) {
          return {
            ...c,
            likeCount: patch.likeCount,
            dislikeCount: patch.dislikeCount,
            myReaction: patch.myReaction,
          }
        }
        return {
          ...c,
          replies: c.replies.map((r) =>
            r.id === commentId
              ? {
                  ...r,
                  likeCount: patch.likeCount,
                  dislikeCount: patch.dislikeCount,
                  myReaction: patch.myReaction,
                }
              : r,
          ),
        }
      }),
    )
  }

  const onCommentReaction = async (comment: LiveVodComment, reaction: "LIKE" | "DISLIKE") => {
    if (!ready || commentReactionBusyId != null) return
    if (!user) {
      openLoginPrompt()
      return
    }
    setCommentReactionBusyId(comment.id)
    try {
      const result = await toggleLiveVodCommentReactionApi(videoId, comment.id, reaction)
      patchCommentReaction(comment.id, result)
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        openLoginPrompt()
        return
      }
      setError(message || "댓글 반응 저장에 실패했습니다.")
    } finally {
      setCommentReactionBusyId(null)
    }
  }

  const CommentReactions = ({ comment }: { comment: LiveVodComment }) => (
    <div className="flv-comment-reactions" role="group" aria-label="댓글 좋아요 싫어요">
      <button
        type="button"
        className={`flv-cmt-rxn-btn${comment.myReaction === "LIKE" ? " is-on" : ""}`}
        disabled={commentReactionBusyId === comment.id}
        aria-pressed={comment.myReaction === "LIKE"}
        onClick={(e) => {
          e.stopPropagation()
          void onCommentReaction(comment, "LIKE")
        }}
      >
        좋아요 {comment.likeCount}
      </button>
      <button
        type="button"
        className={`flv-cmt-rxn-btn${comment.myReaction === "DISLIKE" ? " is-on" : ""}`}
        disabled={commentReactionBusyId === comment.id}
        aria-pressed={comment.myReaction === "DISLIKE"}
        onClick={(e) => {
          e.stopPropagation()
          void onCommentReaction(comment, "DISLIKE")
        }}
      >
        싫어요 {comment.dislikeCount}
      </button>
    </div>
  )

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
        {comments.map((c) => {
          const repliesOpen = Boolean(expandedReplies[c.id])
          const composingHere = replyRootId === c.id && replyTo != null
          return (
            <li
              key={c.id}
              className="flv-comment-item"
              onDoubleClick={(e) => {
                const target = e.target as HTMLElement
                if (target.closest("button, textarea, a, form")) return
                if (c.replyCount > 0) toggleReplies(c.id)
              }}
            >
              <div className="flv-comment-head">
                <div className="flv-comment-head-left">
                  <strong>{c.authorNickname || c.userEmail}</strong>
                  {formatTime(c.createdAt) ? <time>{formatTime(c.createdAt)}</time> : null}
                </div>
                <CommentReactions comment={c} />
              </div>
              <p>{c.content}</p>
              <div className="flv-comment-tools">
                <button type="button" className="flv-reply-btn" onClick={() => startReply(c)}>
                  답글
                </button>
                {c.replyCount > 0 ? (
                  <button
                    type="button"
                    className="flv-reply-toggle"
                    onClick={() => toggleReplies(c.id)}
                    onDoubleClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      toggleReplies(c.id)
                    }}
                    aria-expanded={repliesOpen}
                  >
                    {repliesOpen ? `대댓글 숨기기 ${c.replyCount}` : `대댓글 ${c.replyCount}`}
                  </button>
                ) : null}
              </div>
              {c.replyCount > 0 && !repliesOpen ? (
                <p className="flv-reply-hint-soft">더블클릭하면 대댓글 목록을 볼 수 있습니다.</p>
              ) : null}

              {composingHere ? (
                <form className="flv-inline-reply" onSubmit={(e) => void onSubmitReply(e)}>
                  <p className="flv-reply-hint">
                    {replyTo.authorNickname || replyTo.userEmail} 님에게 답글{" "}
                    <button
                      type="button"
                      onClick={() => {
                        setReplyTo(null)
                        setReplyRootId(null)
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

              {c.replyCount > 0 && repliesOpen ? (
                <div className="flv-reply-block">
                  <ul className="flv-reply-list">
                    {c.replies.map((r) => (
                      <li
                        key={r.id}
                        onDoubleClick={(e) => {
                          e.stopPropagation()
                          startReply(c, r)
                        }}
                      >
                        <div className="flv-comment-head">
                          <div className="flv-comment-head-left">
                            <strong>{r.authorNickname || r.userEmail}</strong>
                            {formatTime(r.createdAt) ? <time>{formatTime(r.createdAt)}</time> : null}
                          </div>
                          <CommentReactions comment={r} />
                        </div>
                        <p>{r.content}</p>
                        <button
                          type="button"
                          className="flv-reply-btn"
                          onClick={() => startReply(c, r)}
                        >
                          답글
                        </button>
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
          )
        })}
      </ul>

      <div ref={sentinelRef} className="flv-comment-sentinel" aria-hidden />
      {loadingMore ? <p className="flv-comment-more">댓글 더 불러오는 중…</p> : null}

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
