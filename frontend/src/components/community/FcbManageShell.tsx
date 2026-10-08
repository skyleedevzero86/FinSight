"use client"

import type { ReactNode } from "react"
import { pageWindow } from "@/data/communityBoardConfig"
import "@/styles/community-board.css"

type ShellProps = {
  title: string
  description?: string
  toolbar?: ReactNode
  children: ReactNode
}

export function FcbManageShell({ title, description, toolbar, children }: ShellProps) {
  return (
    <div className="fcb-page fcb-embed">
      <div className="fcb-main">
        <div className="fcb-container">
          <header className="fcb-heading">
            <h2>{title}</h2>
            {description ? <p className="fcb-description">{description}</p> : null}
          </header>
          {toolbar}
          {children}
        </div>
      </div>
    </div>
  )
}

type TabItem = {
  key: string
  label: string
}

export function FcbTabList({
  label,
  items,
  activeKey,
  onSelect,
}: {
  label: string
  items: TabItem[]
  activeKey: string
  onSelect: (key: string) => void
}) {
  return (
    <div className="bbs_cate tablist fcb-tablist" role="tablist" aria-label={label}>
      <ul className="tablist_3d fcb-tablist-3d">
        {items.map((item) => (
          <li key={item.key} className={item.key === activeKey ? "on fcb-on" : undefined}>
            <button type="button" onClick={() => onSelect(item.key)}>
              {item.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function FcbNumberPager({
  page,
  totalPages,
  onPage,
  disabled,
  label = "페이지",
}: {
  page: number
  totalPages: number
  onPage: (page: number) => void
  disabled?: boolean
  label?: string
}) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, page), safeTotalPages)
  const pages = pageWindow(safePage, safeTotalPages, 7)

  return (
    <div className="bbs_paging">
      <nav className="pg_wrap" aria-label={label}>
        <span className="pg">
          <button type="button" className="pg_page pg_start" disabled={disabled || safePage <= 1} onClick={() => onPage(1)}>
            처음
          </button>
          <button
            type="button"
            className="pg_page pg_prev"
            disabled={disabled || safePage <= 1}
            onClick={() => onPage(safePage - 1)}
          >
            이전
          </button>
          {pages.map((pageNo) =>
            pageNo === safePage ? (
              <span key={pageNo} className="pg_current pg_must" aria-current="page">
                {pageNo}
              </span>
            ) : (
              <button
                key={pageNo}
                type="button"
                className="pg_page pg_must"
                disabled={disabled}
                onClick={() => onPage(pageNo)}
              >
                {pageNo}
              </button>
            ),
          )}
          <button
            type="button"
            className="pg_page pg_next"
            disabled={disabled || safePage >= safeTotalPages}
            onClick={() => onPage(safePage + 1)}
          >
            다음
          </button>
          <button
            type="button"
            className="pg_page pg_end"
            disabled={disabled || safePage >= safeTotalPages}
            onClick={() => onPage(safeTotalPages)}
          >
            맨끝
          </button>
        </span>
      </nav>
    </div>
  )
}
