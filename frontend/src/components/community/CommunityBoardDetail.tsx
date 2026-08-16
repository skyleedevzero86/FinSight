import Link from "next/link"
import type { BoardDetail } from "@/lib/boardApi"
import { formatAuthor, formatBoardDate } from "@/lib/boardApi"

type Props = {
  detail: BoardDetail
  basePath: string
}

export default function CommunityBoardDetail({ detail, basePath }: Props) {
  const html = detail.renderedHtml?.trim()
  const nav = detail.navigation
  const prev = nav?.previous
  const next = nav?.next

  return (
    <div className="bbs bbs_view bbs_basic">
      <div className="bbs_view_head">
        <h3 className="bbs_view_title">{detail.title}</h3>
        <div className="bbs_view_meta flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-600">
          <span>{formatAuthor(detail.authorEmail)}</span>
          <span>조회 {detail.viewCount}</span>
          <span>추천 {detail.likeCount}</span>
          <span>댓글 {detail.commentCount}</span>
          <span>{formatBoardDate(detail.createdAt)}</span>
        </div>
        {detail.hashtags && detail.hashtags.length > 0 ? (
          <div className="mt-2 flex flex-wrap gap-1 text-sm text-finsight-primary">
            {detail.hashtags.map((t) => (
              <span key={t}>#{t}</span>
            ))}
          </div>
        ) : null}
      </div>

      <div className="bbs_view_body mt-6 border-t border-gray-200 pt-6">
        {html ? (
          <div
            className="board-view prose prose-sm max-w-none text-gray-800 [&_a]:text-finsight-primary [&_img]:max-w-full [&_table]:w-full [&_table]:border-collapse [&_table]:text-[14px]"
            dangerouslySetInnerHTML={{ __html: html }}
          />
        ) : (
          <pre className="whitespace-pre-wrap font-sans text-[15px] leading-relaxed text-gray-800">
            {detail.content}
          </pre>
        )}
      </div>

      {(prev || next) && (
        <div className="mt-8 grid gap-2 border-t border-gray-100 pt-4 text-sm">
          {prev ? (
            <div>
              <span className="text-gray-500">이전</span>{" "}
              <Link href={`${basePath}/${prev.id}`} className="text-finsight-primary hover:underline">
                {prev.title}
              </Link>
            </div>
          ) : null}
          {next ? (
            <div>
              <span className="text-gray-500">다음</span>{" "}
              <Link href={`${basePath}/${next.id}`} className="text-finsight-primary hover:underline">
                {next.title}
              </Link>
            </div>
          ) : null}
        </div>
      )}

      <div className="mt-10 flex flex-wrap justify-between gap-3">
        <Link
          href={basePath}
          className="inline-flex items-center rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
        >
          목록
        </Link>
        <Link
          href={`${basePath}/${detail.id}/edit`}
          className="inline-flex items-center rounded border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
        >
          수정
        </Link>
      </div>
    </div>
  )
}
