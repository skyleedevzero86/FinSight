import Link from "next/link"
import type { ReactNode } from "react"
import type { BoardRow } from "@/data/communityBoardData"
import { boardListHref, pageWindow } from "@/data/communityBoardConfig"

type CategoryTab = { label: string; href: string; active?: boolean }

type CommunityBoardListProps = {
  boardId: string
  caption: string
  basePath: string
  extraSearchParams?: Record<string, string | undefined>
  totalCount: number
  currentPage: number
  totalPages: number
  categoryTabs?: CategoryTab[]
  rows: BoardRow[]
  initialSearchType?: string
  initialSearchValue?: string
  showWriteButton?: boolean
  writeButton?: ReactNode
}

export default function CommunityBoardList({
  boardId,
  caption,
  basePath,
  extraSearchParams,
  totalCount,
  currentPage,
  totalPages,
  categoryTabs,
  rows,
  initialSearchType = "",
  initialSearchValue = "",
  showWriteButton = true,
  writeButton,
}: CommunityBoardListProps) {
  const safeTotalPages = Math.max(1, totalPages)
  const pages = pageWindow(currentPage, safeTotalPages, 7)
  const writeHref = `${basePath}/write`

  const listQuery = (page: number) =>
    boardListHref(basePath, {
      page: page > 1 ? page : undefined,
      search_type: initialSearchType || undefined,
      search_value: initialSearchValue || undefined,
      extra: extraSearchParams,
    })

  return (
    <>
      <div className="board-decorate" aria-hidden>
        <p>&nbsp;</p>
      </div>
      <div className="bbs bbs_list bbs_basic" id={boardId}>
        {categoryTabs && categoryTabs.length > 0 ? (
          <div className="bbs_cate tablist fcb-tablist">
            <ul className="tablist_3d fcb-tablist-3d">
              {categoryTabs.map((tab) => (
                <li
                  key={tab.href}
                  className={tab.active ? "on fcb-on" : undefined}
                >
                  <Link href={tab.href}>{tab.label}</Link>
                </li>
              ))}
            </ul>
          </div>
        ) : null}

        <div className="bbs_leadin">
          <div className="bbs_count">
            <span className="list-count">
              전체 <strong>{totalCount}</strong>건
            </span>
            <span className="page-count">
              <b>{currentPage}</b> / {safeTotalPages}page
            </span>
          </div>
          <div className="bbs_search">
            <fieldset>
              <h3>검색</h3>
              <form method="get" action={basePath} role="search">
                {extraSearchParams
                  ? Object.entries(extraSearchParams).map(([k, v]) =>
                      v ? (
                        <input type="hidden" key={k} name={k} value={v} />
                      ) : null,
                    )
                  : null}
                <label htmlFor={`${boardId}-search-type`} className="sr-only">
                  검색항목
                </label>
                <select
                  id={`${boardId}-search-type`}
                  name="search_type"
                  className="sch_select"
                  defaultValue={initialSearchType}
                >
                  <option value="">전체</option>
                  <option value="subject">제목</option>
                  <option value="content">내용</option>
                </select>
                <label htmlFor={`${boardId}-search-value`} className="sr-only">
                  검색어
                </label>
                <input
                  type="search"
                  name="search_value"
                  id={`${boardId}-search-value`}
                  className="sch_input"
                  maxLength={80}
                  autoComplete="off"
                  placeholder="검색어를 입력해주세요."
                  defaultValue={initialSearchValue}
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
            <caption>{caption}</caption>
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
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="td_subject py-10 text-center text-gray-500">
                    등록된 글이 없습니다.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr key={row.id ?? `${String(row.num)}-${row.title}`}>
                    <td className="td_num">
                      {row.num === "pin" ? (
                        <b className="is_noti">공지</b>
                      ) : (
                        row.num
                      )}
                    </td>
                    <td className="td_subject">
                      <Link href={row.href}>
                        {row.privatePost ? (
                          <span className="mr-1.5 inline-flex items-center rounded bg-slate-100 px-1.5 py-0.5 text-[11px] font-semibold text-slate-600">
                            비공개
                          </span>
                        ) : null}
                        {row.title}
                      </Link>
                    </td>
                    <td className="td_name">{row.author}</td>
                    <td className="td_file">
                      {row.hasFile ? (
                        <span
                          className="inline-block h-4 w-4 rounded-sm bg-finsight-primary/80"
                          title="첨부파일"
                        />
                      ) : null}
                    </td>
                    <td className="td_date">{row.date}</td>
                    <td className="td_hit">{row.hits}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="bbs_paging" id={`${boardId}-paging`}>
          <nav className="pg_wrap" aria-label="페이지">
            <span className="pg">
              {currentPage <= 1 ? (
                <span className="pg_page pg_start pg_empty" aria-hidden>
                  처음
                </span>
              ) : (
                <Link href={listQuery(1)} className="pg_page pg_start">
                  처음
                </Link>
              )}
              {currentPage <= 1 ? (
                <span className="pg_page pg_prev pg_empty" aria-hidden>
                  이전
                </span>
              ) : (
                <Link href={listQuery(currentPage - 1)} className="pg_page pg_prev">
                  이전
                </Link>
              )}
              {pages.map((p) =>
                p === currentPage ? (
                  <span
                    key={p}
                    className="pg_current pg_must"
                    aria-current="page"
                  >
                    <span className="sr-only">현재 </span>
                    {p}
                    <span className="sr-only"> 페이지</span>
                  </span>
                ) : (
                  <Link key={p} href={listQuery(p)} className="pg_page pg_must">
                    {p}
                  </Link>
                ),
              )}
              {currentPage >= safeTotalPages ? (
                <span className="pg_page pg_next pg_empty">다음</span>
              ) : (
                <Link href={listQuery(currentPage + 1)} className="pg_page pg_next">
                  다음
                </Link>
              )}
              {currentPage >= safeTotalPages ? (
                <span className="pg_page pg_end pg_empty">맨끝</span>
              ) : (
                <Link href={listQuery(safeTotalPages)} className="pg_page pg_end">
                  맨끝
                </Link>
              )}
            </span>
          </nav>
        </div>

        <div className="bbs_listbtn">
          <div className="left" />
          <div className="right">
            {writeButton != null
              ? writeButton
              : showWriteButton ? (
                  <Link
                    href={writeHref}
                    className="inline-flex items-center justify-center rounded border border-finsight-primary bg-finsight-primary px-4 py-2 text-sm font-medium text-white hover:opacity-95"
                  >
                    글쓰기
                  </Link>
                ) : null}
          </div>
        </div>
      </div>
    </>
  )
}
