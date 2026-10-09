"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import Link from "next/link"
import { pageWindow } from "@/data/communityBoardConfig"
import CommunityBoardEditorForm from "@/components/community/CommunityBoardEditorForm"
import "@/styles/community-board.css"
import {
  formatAuthor,
  formatBoardDate,
  unwrapApiData,
  type BoardDetail,
  type BoardListItem,
  type BoardPagination,
} from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"

const buttonClass =
  "rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type EditorState =
  | { mode: "create" }
  | { mode: "edit"; detail: BoardDetail }

export default function AdminNoticeBoardSection() {
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<BoardListItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editor, setEditor] = useState<EditorState | null>(null)
  const [openingId, setOpeningId] = useState<number | null>(null)
  const [keyword, setKeyword] = useState("")
  const [searchInput, setSearchInput] = useState("")

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const params = new URLSearchParams({
      boardType: "NOTICE",
      page: String(page),
      size: "20",
    })
    if (keyword.trim()) params.set("keyword", keyword.trim())
    try {
      const res = await fetch(`/api/v1/boards?${params.toString()}`, {
        headers: { Accept: "application/json", ...authHeadersJson() },
        cache: "no-store",
      })
      const payload: unknown = await res.json().catch(() => null)
      if (!res.ok) {
        setError(readMessage(payload) || "공지 목록을 불러오지 못했습니다.")
        setRows([])
        setTotalElements(0)
        return
      }
      const data = unwrapApiData<BoardPagination>(payload)
      setRows(data?.content ?? [])
      setTotalElements(Number(data?.totalElements) || (data?.content?.length ?? 0))
      setTotalPages(Math.max(1, data?.totalPages ?? 1))
    } catch {
      setError("공지 목록을 불러오지 못했습니다.")
      setRows([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [page, keyword])

  useEffect(() => {
    void load()
  }, [load])

  async function openEdit(id: number) {
    setOpeningId(id)
    setError(null)
    try {
      const res = await fetch(`/api/v1/boards/${id}?trackView=false`, {
        headers: { Accept: "application/json", ...authHeadersJson() },
        cache: "no-store",
      })
      const payload: unknown = await res.json().catch(() => null)
      if (!res.ok) {
        setError(readMessage(payload) || "공지를 불러오지 못했습니다.")
        return
      }
      const detail = unwrapApiData<BoardDetail>(payload)
      if (!detail) {
        setError("공지를 불러오지 못했습니다.")
        return
      }
      setEditor({ mode: "edit", detail })
    } catch {
      setError("공지를 불러오지 못했습니다.")
    } finally {
      setOpeningId(null)
    }
  }

  function closeEditor() {
    setEditor(null)
    void load()
  }

  const safeTotalPages = Math.max(1, totalPages)
  const pages = pageWindow(page + 1, safeTotalPages, 7)

  return (
    <section>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-gray-500">
          커뮤니티 공지사항은 이 화면에서 작성하고 수정합니다.
        </p>
        {editor?.mode === "create" ? null : (
          <button
            type="button"
            className={primaryButtonClass}
            onClick={() => setEditor({ mode: "create" })}
          >
            공지 작성
          </button>
        )}
      </div>

      {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}

      {editor?.mode === "create" ? (
        <div className="mt-5">
          <CommunityBoardEditorForm
            key="notice-create"
            mode="create"
            boardType="NOTICE"
            basePath="/admin/notifications"
            onSaved={closeEditor}
            onCancel={() => setEditor(null)}
          />
        </div>
      ) : null}

      {editor?.mode === "edit" ? (
        <div className="mt-5">
          <CommunityBoardEditorForm
            key={`notice-edit-${editor.detail.id}`}
            mode="edit"
            boardType="NOTICE"
            basePath="/admin/notifications"
            boardId={editor.detail.id}
            authorEmail={editor.detail.authorEmail}
            initialTitle={editor.detail.title}
            initialContent={editor.detail.content}
            initialTags={(editor.detail.hashtags ?? []).join(", ")}
            initialHighlighted={Boolean(editor.detail.highlighted)}
            onSaved={closeEditor}
            onCancel={() => setEditor(null)}
          />
        </div>
      ) : null}

      <div className="bbs bbs_list bbs_basic" id="bbs_admin_notice">
        <div className="bbs_leadin">
          <div className="bbs_count">
            <span className="list-count">
              전체 <strong>{totalElements}</strong>건
            </span>
            <span className="page-count">
              <b>{page + 1}</b> / {safeTotalPages}page
            </span>
          </div>
          <div className="bbs_search">
            <fieldset>
              <h3>검색</h3>
              <form
                role="search"
                onSubmit={(event: FormEvent) => {
                  event.preventDefault()
                  setPage(0)
                  setKeyword(searchInput.trim())
                }}
              >
                <label htmlFor="admin-notice-search" className="sr-only">
                  검색어
                </label>
                <input
                  type="search"
                  id="admin-notice-search"
                  className="sch_input"
                  maxLength={80}
                  autoComplete="off"
                  placeholder="검색어를 입력해주세요."
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                />
                <button type="submit" className="sch_button">
                  <span>검색</span>
                </button>
              </form>
            </fieldset>
          </div>
        </div>
        <div className="bbs_listing">
          <table className="table">
            <caption>공지 게시판</caption>
            <thead>
              <tr>
                <th className="td_num">번호</th>
                <th className="td_subject" style={{ textAlign: "center" }}>
                  제목
                </th>
                <th className="td_name">작성자</th>
                <th className="td_file">첨부</th>
                <th className="td_date">작성일</th>
                <th className="td_hit">조회수</th>
                <th className="td_action">관리</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className="td_subject" colSpan={7}>
                    공지 목록을 불러오는 중…
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td className="td_subject" colSpan={7}>
                    등록된 공지가 없습니다.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.id}>
                    <td className="td_num">
                      {row.highlighted ? <b className="is_noti">공지</b> : row.id}
                    </td>
                    <td className="td_subject">
                      <Link
                        href={`/community/notice/${row.id}`}
                        className={row.highlighted ? "is_emphasis" : undefined}
                      >
                        {row.title}
                      </Link>
                    </td>
                    <td className="td_name">{formatAuthor(row.authorEmail)}</td>
                    <td className="td_file" />
                    <td className="td_date">{formatBoardDate(row.createdAt)}</td>
                    <td className="td_hit">{row.viewCount}</td>
                    <td className="td_action">
                      <div className="flex flex-wrap justify-center gap-1">
                        <Link href={`/community/notice/${row.id}`} className={buttonClass}>
                          보기
                        </Link>
                        <button
                          type="button"
                          className={buttonClass}
                          disabled={openingId === row.id}
                          onClick={() => void openEdit(row.id)}
                        >
                          {openingId === row.id ? "불러오는 중…" : "수정"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <div className="bbs_paging">
          <nav className="pg_wrap" aria-label="공지 페이지">
            <span className="pg">
              <button type="button" className="pg_page pg_start" disabled={page <= 0} onClick={() => setPage(0)}>
                처음
              </button>
              <button
                type="button"
                className="pg_page pg_prev"
                disabled={page <= 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                이전
              </button>
              {pages.map((pageNo) =>
                pageNo === page + 1 ? (
                  <span key={pageNo} className="pg_current pg_must" aria-current="page">
                    {pageNo}
                  </span>
                ) : (
                  <button
                    key={pageNo}
                    type="button"
                    className="pg_page pg_must"
                    onClick={() => setPage(pageNo - 1)}
                  >
                    {pageNo}
                  </button>
                ),
              )}
              <button
                type="button"
                className="pg_page pg_next"
                disabled={page + 1 >= safeTotalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                다음
              </button>
              <button
                type="button"
                className="pg_page pg_end"
                disabled={page + 1 >= safeTotalPages}
                onClick={() => setPage(safeTotalPages - 1)}
              >
                맨끝
              </button>
            </span>
          </nav>
        </div>
      </div>
    </section>
  )
}

function readMessage(payload: unknown): string {
  if (!payload || typeof payload !== "object" || !("message" in payload)) return ""
  const message = String((payload as { message?: string }).message ?? "")
  return /[가-힣]/.test(message) ? message : ""
}
