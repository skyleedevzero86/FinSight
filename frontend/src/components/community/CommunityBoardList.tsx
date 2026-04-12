import Link from "next/link"
import type { BoardRow } from "@/data/communityBoardData"

type CategoryTab = { label: string; href: string; active?: boolean }

type CommunityBoardListProps = {
  boardId: string
  caption: string
  totalCount: number
  currentPage: number
  totalPages: number
  categoryTabs?: CategoryTab[]
  rows: BoardRow[]
}

export default function CommunityBoardList({
  boardId,
  caption,
  totalCount,
  currentPage,
  totalPages,
  categoryTabs,
  rows,
}: CommunityBoardListProps) {
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
            <b>{currentPage}</b> / {totalPages}page
          </span>
        </div>
        <div className="bbs_search">
          <fieldset>
            <h3>검색</h3>
            <form method="get" role="search">
              <label htmlFor={`${boardId}-search-type`} className="sr-only">
                검색항목
              </label>
              <select
                id={`${boardId}-search-type`}
                name="search_type"
                className="sch_select"
                defaultValue=""
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
            {rows.map((row) => (
              <tr key={`${row.num}-${row.title}`}>
                <td className="td_num">
                  {row.num === "pin" ? (
                    <b className="is_noti">공지</b>
                  ) : (
                    row.num
                  )}
                </td>
                <td className="td_subject">
                  <Link href={row.href}>{row.title}</Link>
                </td>
                <td className="td_name">{row.author}</td>
                <td className="td_file">
                  {row.hasFile ? (
                    <span className="inline-block h-4 w-4 rounded-sm bg-finsight-primary/80" title="첨부파일" />
                  ) : null}
                </td>
                <td className="td_date">{row.date}</td>
                <td className="td_hit">{row.hits}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bbs_paging" id={`${boardId}-paging`}>
        <nav className="pg_wrap" aria-label="페이지">
          <span className="pg">
            <span className="pg_page pg_start pg_empty" aria-hidden>
              처음
            </span>
            <span className="pg_page pg_prev pg_empty" aria-hidden>
              이전
            </span>
            <span className="pg_current pg_must" aria-current="page">
              <span className="sr-only">현재 </span>1
              <span className="sr-only"> 페이지</span>
            </span>
            {totalPages > 1 ? (
              <Link href="#" className="pg_page pg_must">
                2
              </Link>
            ) : null}
            {totalPages > 2 ? (
              <Link href="#" className="pg_page pg_must">
                3
              </Link>
            ) : null}
            {totalPages > 1 ? (
              <Link href="#" className="pg_page pg_next">
                다음
              </Link>
            ) : (
              <span className="pg_page pg_next pg_empty">다음</span>
            )}
            <Link href="#" className="pg_page pg_end">
              맨끝
            </Link>
          </span>
        </nav>
      </div>

      <div className="bbs_listbtn">
        <div className="left" />
        <div className="right" />
      </div>
    </div>
    </>
  )
}
