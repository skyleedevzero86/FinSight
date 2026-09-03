"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import type { BoardDetail } from "@/lib/boardApi"
import { formatAuthor, formatBoardDate, unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import CommunityMarkdownPreview from "@/components/community/markdown/CommunityMarkdownPreview"
import CommunityBoardComments from "@/components/community/CommunityBoardComments"

type Props = {
  detail: BoardDetail
  basePath: string
  enableComments?: boolean
}

export default function CommunityBoardDetail({
  detail: initialDetail,
  basePath,
  enableComments = false,
}: Props) {
  const [detail, setDetail] = useState(initialDetail)
  const [accessError, setAccessError] = useState<string | null>(null)
  const [commentCount, setCommentCount] = useState(initialDetail.commentCount)

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
    formatAuthor(detail.authorEmail),
    `조회 ${detail.viewCount}`,
    `추천 ${detail.likeCount}`,
    `댓글 ${commentCount}`,
    formatBoardDate(detail.createdAt),
  ]
    .filter(Boolean)
    .join(" · ")

  return (
    <div className="fcb-md-detail">
      <CommunityMarkdownPreview
        title={detail.title}
        markdown={detail.content || ""}
        tags={detail.hashtags ?? []}
        metaLine={metaLine}
        eyebrow={detail.status === "PRIVATE" ? "비공개 글" : ""}
        showTableOfContents
        className="fcb-md-detail__preview"
      />

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

      {enableComments ? (
        <CommunityBoardComments
          boardId={detail.id}
          boardAuthorEmail={detail.authorEmail}
          boardType={detail.boardType}
          onCountChange={setCommentCount}
        />
      ) : null}

      <div className="fcb-md-detail__footer">
        <Link href={basePath} className="fcb-md-action fcb-md-action--ghost">
          목록
        </Link>
        <Link href={`${basePath}/${detail.id}/edit`} className="fcb-md-action fcb-md-action--ghost">
          수정
        </Link>
      </div>
    </div>
  )
}
