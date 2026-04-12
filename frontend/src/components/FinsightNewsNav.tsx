"use client"

import Link from "next/link"
import { Suspense } from "react"
import { usePathname, useSearchParams } from "next/navigation"

type NavItem = { label: string; href: string; category: string }

const NAV_TARGET_ITEMS: NavItem[] = [
  { label: "전체기사", category: "ALL", href: "/news" },
  { label: "SPY", category: "SPY", href: "/news?category=SPY" },
  { label: "QQQ", category: "QQQ", href: "/news?category=QQQ" },
  { label: "BTC", category: "BTC", href: "/news?category=BTC" },
  { label: "AAPL", category: "AAPL", href: "/news?category=AAPL" },
  { label: "MSFT", category: "MSFT", href: "/news?category=MSFT" },
  { label: "NVDA", category: "NVDA", href: "/news?category=NVDA" },
  { label: "GOOGL", category: "GOOGL", href: "/news?category=GOOGL" },
  { label: "META", category: "META", href: "/news?category=META" },
  { label: "TSLA", category: "TSLA", href: "/news?category=TSLA" },
  { label: "기타", category: "NONE", href: "/news?category=NONE" },
]

function navItemActive(pathname: string, categoryParam: string | null, itemCategory: string): boolean {
  if (pathname !== "/news") return false
  if (itemCategory === "ALL") {
    return !categoryParam || categoryParam.trim() === ""
  }
  const q = categoryParam?.trim().toUpperCase() ?? ""
  return q === itemCategory.toUpperCase()
}

function FinsightNewsNavContent({ categoryParam }: { categoryParam: string | null }) {
  const pathname = usePathname()

  return (
    <nav id="navi" aria-label="뉴스 타겟 자산·종목 분류">
      <div className="wrap_pc_nav">
        <div className="pc_nav">
          <div className="nav_d1">
            <ul>
              {NAV_TARGET_ITEMS.map(({ label, href, category }) => {
                const active = navItemActive(pathname, categoryParam, category)
                return (
                  <li key={category} className={active ? "on" : undefined}>
                    <Link href={href}>{label}</Link>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>

        <div className="progress_container" aria-hidden>
          <div className="progress_bar" id="progressBar" style={{ width: "0%" }} />
        </div>
      </div>

      <div className="m_navi">
        <div className="wrapper list_slider">
          <ul className="navi_w">
            {NAV_TARGET_ITEMS.map(({ label, href, category }) => {
              const active = navItemActive(pathname, categoryParam, category)
              return (
                <li key={`m-${category}`} className={active ? "active" : undefined}>
                  <Link href={href}>{label}</Link>
                </li>
              )
            })}
          </ul>
        </div>
      </div>
    </nav>
  )
}

function FinsightNewsNavWithSearchParams() {
  const searchParams = useSearchParams()
  return <FinsightNewsNavContent categoryParam={searchParams.get("category")} />
}

export default function FinsightNewsNav() {
  return (
    <Suspense fallback={<FinsightNewsNavContent categoryParam={null} />}>
      <FinsightNewsNavWithSearchParams />
    </Suspense>
  )
}
