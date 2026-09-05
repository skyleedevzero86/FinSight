"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import type { BoardDetail } from "@/lib/boardApi"
import {
  dislikeBoard,
  fetchBoardReactionStatus,
  formatAuthor,
  formatBoardDate,
  likeBoard,
  unwrapApiData,
} from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import { useAuthSession } from "@/components/AuthSessionProvider"
import CommunityMarkdownPreview from "@/components/community/markdown/CommunityMarkdownPreview"
import CommunityBoardComments from "@/components/community/CommunityBoardComments"
import { MODERATION_RESTRICTED_MESSAGE } from "@/lib/boardModeration"

type Props = {
  detail: BoardDetail
  basePath: string
  enableComments?: boolean
  commentsAuthorOnly?: boolean
}

export default function CommunityBoardDetail({
  detail: initialDetail,
  basePath,
  enableComments = false,
  commentsAuthorOnly = false,
}: Props) {
  const [detail, setDetail] = useState(initialDetail)
  const [accessError, setAccessError] = useState<string | null>(null)
  const [commentCount, setCommentCount] = useState(initialDetail.commentCount)
  const { user, ready } = useAuthSession()
  const [liked, setLiked] = useState(false)
  const [disliked, setDisliked] = useState(false)
  const [reactionBusy, setReactionBusy] = useState(false)
  const [reactionError, setReactionError] = useState<string | null>(null)

  const role = (user?.role ?? "").toUpperCase().replace(/^ROLE_/, "")
  const isStaff = role === "ADMIN" || role === "MANAGER"
  const isAuthor =
    !!user?.email &&
    !!detail.authorEmail &&
    user.email.toLowerCase() === detail.authorEmail.toLowerCase()
  const canEdit = ready && !!user && (isAuthor || isStaff)
  const isRestricted =
    detail.status === "HIDDEN" || detail.status === "BLOCKED"

  async function onBoardReaction(kind: "LIKE" | "DISLIKE") {
    if (!ready || reactionBusy || isRestricted) return
    if (!user) {
      setReactionError("로그인 후 반응할 수 있습니다.")
      return
    }
    setReactionBusy(true)
    setReactionError(null)
    try {
      const result = kind === "LIKE" ? await likeBoard(detail.id) : await dislikeBoard(detail.id)
      if (!result.ok) {
        setReactionError(result.message)
        return
      }
      setDetail((prev) => ({
        ...prev,
        likeCount: result.likeCount,
        dislikeCount: result.dislikeCount,
      }))
      const status = await fetchBoardReactionStatus(detail.id)
      if (status) {
        setLiked(status.liked)
        setDisliked(status.disliked)
      } else if (kind === "LIKE") {
        setLiked((v) => !v)
        setDisliked(false)
      } else {
        setDisliked((v) => !v)
        setLiked(false)
      }
    } finally {
      setReactionBusy(false)
    }
  }

  useEffect(() => {
    setDetail(initialDetail)
    setCommentCount(initialDetail.commentCount)
  }, [initialDetail])

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      try {
        const res = await fetch(`/api/v1/boards/${initialDetail.id}?trackView=false`, {
          headers: { Accept: "application/json", ...authHeadersJson() },
          cache: "no-store",
        })
        if (cancelled) return
        if (res.status === 403) {
          setAccessError("비공개 글은 작성자와 관리자만 볼 수 있습니다.")
          return
        }
        if (!res.ok) return
        const payload: unknown = await res.json().catch(() => null)
        const data = unwrapApiData<BoardDetail>(payload)
        if (data) {
          setDetail(data)
          setCommentCount(data.commentCount)
          setAccessError(null)
        }
      } catch {
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [initialDetail.id])

  useEffect(() => {
    if (!ready || !user) {
      setLiked(false)
      setDisliked(false)
      return
    }
    let cancelled = false
    const run = async () => {
      const status = await fetchBoardReactionStatus(initialDetail.id)
      if (cancelled || !status) return
      setLiked(status.liked)
      setDisliked(status.disliked)
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [ready, user, initialDetail.id])

  if (accessError) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-6 text-sm text-amber-900">
        {accessError}
        <div className="mt-4">
          <Link href={basePath} className="fcb-md-action fcb-md-action--ghost">
            목록으로
          </Link>
        </div>
      </div>
    )
  }

  const nav = detail.navigation
  const prev = nav?.previous
  const next = nav?.next
  const metaLine = [
    detail.status === "PRIVATE" ? "비공개" : null,
    isRestricted ? "게시 중단" : null,
    formatAuthor(detail.authorEmail),
    `조회 ${detail.viewCount}`,
    `추천 ${detail.likeCount}`,
    `비추천 ${detail.dislikeCount}`,
    `댓글 ${commentCount}`,
    formatBoardDate(detail.createdAt),
  ]
    .filter(Boolean)
    .join(" · ")

  return (
    <div className="fcb-md-detail">
      <div className={isRestricted ? "fcb-moderation-restricted" : undefined}>
        {isRestricted ? (
          <div className="fcb-moderation-restricted__banner" role="status">
            {MODERATION_RESTRICTED_MESSAGE}
          </div>
        ) : null}
        <div className={isRestricted ? "fcb-moderation-restricted__blur" : undefined}>
          <CommunityMarkdownPreview
            title={detail.title}
            markdown={detail.content || ""}
            tags={detail.hashtags ?? []}
            metaLine={metaLine}
            eyebrow={
              detail.status === "PRIVATE"
                ? "비공개 글"
                : isRestricted
                  ? "게시 중단"
                  : ""
            }
            showTableOfContents={!isRestricted}
            className="fcb-md-detail__preview"
          />
        </div>
      </div>

      {!isRestricted ? (
        <>
          <div className="fcb-md-reaction-row" role="group" aria-label="게시글 좋아요 싫어요">
            <button
              type="button"
              className={`fcb-md-reaction-btn${liked ? " is-on" : ""}`}
              disabled={reactionBusy}
              aria-pressed={liked}
              onClick={() => void onBoardReaction("LIKE")}
            >
              좋아요 {detail.likeCount}
            </button>
            <button
              type="button"
              className={`fcb-md-reaction-btn${disliked ? " is-on" : ""}`}
              disabled={reactionBusy}
              aria-pressed={disliked}
              onClick={() => void onBoardReaction("DISLIKE")}
            >
              싫어요 {detail.dislikeCount}
            </button>
          </div>
          {reactionError ? (
            <p className="mt-2 text-sm text-red-600">{reactionError}</p>
          ) : null}
        </>
      ) : null}

      {(prev || next) && (
        <div className="fcb-md-nav">
          {prev ? (
            <Link href={`${basePath}/${prev.id}`} className="fcb-md-nav__card">
              <span className="fcb-md-nav__label">이전 글</span>
              <strong className="fcb-md-nav__title">{prev.title}</strong>
            </Link>
          ) : (
            <div className="fcb-md-nav__card fcb-md-nav__card--empty">이전 글이 없습니다</div>
          )}
          {next ? (
            <Link href={`${basePath}/${next.id}`} className="fcb-md-nav__card">
              <span className="fcb-md-nav__label">다음 글</span>
              <strong className="fcb-md-nav__title">{next.title}</strong>
            </Link>
          ) : (
            <div className="fcb-md-nav__card fcb-md-nav__card--empty">다음 글이 없습니다</div>
          )}
        </div>
      )}

      {enableComments && !isRestricted ? (
        <CommunityBoardComments
          boardId={detail.id}
          boardAuthorEmail={detail.authorEmail}
          boardType={detail.boardType}
          commentsAuthorOnly={commentsAuthorOnly || detail.boardType === "QNA"}
          initialCommentCount={detail.commentCount}
          onCountChange={setCommentCount}
        />
      ) : null}

      <div className="fcb-md-detail__footer">
        <Link href={basePath} className="fcb-md-action fcb-md-action--ghost">
          목록
        </Link>
        {canEdit && !isRestricted ? (
          <Link href={`${basePath}/${detail.id}/edit`} className="fcb-md-action fcb-md-action--ghost">
            수정
          </Link>
        ) : null}
        {isStaff && detail.reportCount > 0 ? (
          <span className="ml-auto text-xs text-amber-700">신고 {detail.reportCount}회</span>
        ) : null}
      </div>
    </div>
  )
}
