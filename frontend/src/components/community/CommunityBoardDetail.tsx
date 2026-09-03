"use client"

import Link from "next/link"
import type { BoardDetail } from "@/lib/boardApi"
import { formatAuthor, formatBoardDate } from "@/lib/boardApi"
import CommunityMarkdownPreview from "@/components/community/markdown/CommunityMarkdownPreview"

type Props = {
  detail: BoardDetail
  basePath: string
}

export default function CommunityBoardDetail({ detail, basePath }: Props) {
  const nav = detail.navigation
  const prev = nav?.previous
  const next = nav?.next
  const metaLine = [
    formatAuthor(detail.authorEmail),
    `조회 ${detail.viewCount}`,
    `추천 ${detail.likeCount}`,
    `댓글 ${detail.commentCount}`,
    formatBoardDate(detail.createdAt),
  ].join(" · ")

  return (
    <div className="fcb-md-detail">
      <CommunityMarkdownPreview
        title={detail.title}
        markdown={detail.content || ""}
        tags={detail.hashtags ?? []}
        metaLine={metaLine}
        eyebrow=""
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
