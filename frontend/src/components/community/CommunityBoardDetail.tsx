"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import type { BoardDetail } from "@/lib/boardApi"
import { formatAuthor, formatBoardDate, unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import { useAuthSession } from "@/components/AuthSessionProvider"
import CommunityMarkdownPreview from "@/components/community/markdown/CommunityMarkdownPreview"
import CommunityBoardComments from "@/components/community/CommunityBoardComments"
import { reportBoard, REPORT_REASONS } from "@/lib/boardModeration"

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
  const [reportOpen, setReportOpen] = useState(false)
  const [reportReason, setReportReason] = useState<string>(REPORT_REASONS[0].value)
  const [reportDesc, setReportDesc] = useState("")
  const [reportBusy, setReportBusy] = useState(false)
  const [reportError, setReportError] = useState<string | null>(null)
  const [reportDone, setReportDone] = useState(false)

  const role = (user?.role ?? "").toUpperCase().replace(/^ROLE_/, "")
  const isStaff = role === "ADMIN" || role === "MANAGER"
  const isAuthor =
    !!user?.email &&
    !!detail.authorEmail &&
    user.email.toLowerCase() === detail.authorEmail.toLowerCase()
  const canEdit = ready && !!user && (isAuthor || isStaff)

  async function onReport() {
    setReportBusy(true)
    setReportError(null)
    const result = await reportBoard(detail.id, reportReason, reportDesc.trim())
    setReportBusy(false)
    if (!result.ok) {
      setReportError(result.message)
      return
    }
    setReportDone(true)
    setDetail((prev) => ({ ...prev, reportCount: (prev.reportCount ?? 0) + 1 }))
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
          commentsAuthorOnly={commentsAuthorOnly || detail.boardType === "QNA"}
          initialCommentCount={detail.commentCount}
          onCountChange={setCommentCount}
        />
      ) : null}

      <div className="fcb-md-detail__footer">
        <Link href={basePath} className="fcb-md-action fcb-md-action--ghost">
          목록
        </Link>
        {canEdit ? (
          <Link href={`${basePath}/${detail.id}/edit`} className="fcb-md-action fcb-md-action--ghost">
            수정
          </Link>
        ) : null}
        {ready && user && !isAuthor ? (
          <button
            type="button"
            className="fcb-md-action fcb-md-action--ghost"
            onClick={() => setReportOpen((v) => !v)}
          >
            신고
          </button>
        ) : null}
        {isStaff && detail.reportCount > 0 ? (
          <span className="ml-auto text-xs text-amber-700">신고 {detail.reportCount}회</span>
        ) : null}
      </div>

      {reportOpen ? (
        <div className="mt-4 rounded-xl border border-gray-200 bg-gray-50 p-4">
          <h3 className="text-sm font-semibold text-gray-900">게시글 신고</h3>
          <p className="mt-1 text-xs text-gray-500">허위 신고는 이용 제한 사유가 될 수 있습니다.</p>
          {reportError ? (
            <div className="mt-2 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {reportError}
            </div>
          ) : null}
          {reportDone ? (
            <div className="mt-2 rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              신고가 접수되었습니다.
            </div>
          ) : (
            <form
              className="mt-3 space-y-3"
              onSubmit={(e) => {
                e.preventDefault()
                void onReport()
              }}
            >
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
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={reportBusy}
                  className="rounded bg-finsight-primary px-4 py-2 text-sm text-white disabled:opacity-50"
                >
                  신고 제출
                </button>
                <button
                  type="button"
                  className="rounded border border-gray-300 bg-white px-4 py-2 text-sm"
                  onClick={() => setReportOpen(false)}
                >
                  취소
                </button>
              </div>
            </form>
          )}
        </div>
      ) : null}
    </div>
  )
}
