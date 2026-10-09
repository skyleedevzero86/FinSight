"use client"

import type { FormEvent, ReactNode } from "react"
import { pageWindow } from "@/data/communityBoardConfig"
import "@/styles/community-board.css"

type SearchProps = {
  type: string
  value: string
  typeOptions?: { value: string; label: string }[]
  onTypeChange: (value: string) => void
  onValueChange: (value: string) => void
  onSubmit: () => void
}

type Props = {
  boardId: string
  heading?: string
  description?: string
  caption: string
  totalCount: number
  currentPage: number
  totalPages: number
  onPage: (page: number) => void
  search?: SearchProps
  toolbar?: ReactNode
  beforeList?: ReactNode
  children: ReactNode
}

export default function FcbBoardFrame({
  boardId,
  heading,
  description,
  caption,
  totalCount,
  currentPage,
  totalPages,
  onPage,
  search,
  toolbar,
  beforeList,
  children,
}: Props) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, currentPage), safeTotalPages)
  const pages = pageWindow(safePage, safeTotalPages, 7)

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    search?.onSubmit()
  }

  return (
    <div className="fcb-page fcb-embed">
      <div className="fcb-main">
        <div className="fcb-container">
          {heading ? (
            <header className="fcb-heading">
              <h2>{heading}</h2>
              {description ? <p className="fcb-description">{description}</p> : null}
            </header>
          ) : null}
          {beforeList}
          <div className="bbs bbs_list bbs_basic" id={boardId}>
            <div className="bbs_leadin">
              <div className="bbs_count">
                <span className="list-count">
                  전체 <strong>{totalCount}</strong>건
                </span>
                <span className="page-count">
                  <b>{safePage}</b> / {safeTotalPages}page
                </span>
              </div>
              {search ? (
                <div className="bbs_search">
                  <fieldset>
                    <h3>검색</h3>
                    <form role="search" onSubmit={submitSearch}>
                      <label htmlFor={`${boardId}-search-type`} className="sr-only">
                        검색항목
                      </label>
                      <select
                        id={`${boardId}-search-type`}
                        className="sch_select"
                        value={search.type}
                        onChange={(event) => search.onTypeChange(event.target.value)}
                      >
                        <option value="">전체</option>
                        {(search.typeOptions ?? [
                          { value: "subject", label: "제목" },
                          { value: "content", label: "내용" },
                        ]).map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      <label htmlFor={`${boardId}-search-value`} className="sr-only">
                        검색어
                      </label>
                      <input
                        type="search"
                        id={`${boardId}-search-value`}
                        className="sch_input"
                        maxLength={80}
                        autoComplete="off"
                        placeholder="검색어를 입력해주세요."
                        value={search.value}
                        onChange={(event) => search.onValueChange(event.target.value)}
                      />
                      <button type="submit" className="sch_button">
                        <span>검색</span>
                      </button>
                    </form>
                  </fieldset>
                </div>
              ) : null}
              {toolbar}
            </div>
            <div className="bbs_listing">
              <table className="table">
                <caption>{caption}</caption>
                {children}
              </table>
            </div>
            <div className="bbs_paging" id={`${boardId}-paging`}>
              <nav className="pg_wrap" aria-label="페이지">
                <span className="pg">
                  <button
                    type="button"
                    className="pg_page pg_start"
                    disabled={safePage <= 1}
                    onClick={() => onPage(1)}
                  >
                    처음
                  </button>
                  <button
                    type="button"
                    className="pg_page pg_prev"
                    disabled={safePage <= 1}
                    onClick={() => onPage(safePage - 1)}
                  >
                    이전
                  </button>
                  {pages.map((pageNo) =>
                    pageNo === safePage ? (
                      <span key={pageNo} className="pg_current pg_must" aria-current="page">
                        <span className="sr-only">현재 </span>
                        {pageNo}
                        <span className="sr-only"> 페이지</span>
                      </span>
                    ) : (
                      <button
                        key={pageNo}
                        type="button"
                        className="pg_page pg_must"
                        onClick={() => onPage(pageNo)}
                      >
                        {pageNo}
                      </button>
                    ),
                  )}
                  <button
                    type="button"
                    className="pg_page pg_next"
                    disabled={safePage >= safeTotalPages}
                    onClick={() => onPage(safePage + 1)}
                  >
                    다음
                  </button>
                  <button
                    type="button"
                    className="pg_page pg_end"
                    disabled={safePage >= safeTotalPages}
                    onClick={() => onPage(safeTotalPages)}
                  >
                    맨끝
                  </button>
                </span>
              </nav>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
