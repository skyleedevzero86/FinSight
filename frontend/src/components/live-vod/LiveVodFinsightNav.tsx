"use client"

import Link from "next/link"
import { Suspense } from "react"
import { usePathname, useSearchParams } from "next/navigation"
import { LIVE_VOD_NAV_ITEMS } from "@/data/liveVodNavData"

function navItemActive(pathname: string, tabParam: string | null, itemTab: string): boolean {
  if (pathname !== "/live-vod" && !pathname.startsWith("/live-vod/watch")) return false
  const q = tabParam?.trim().toUpperCase() ?? ""
  if (q === "FAVORITES") return false
  if (itemTab === "ALL") {
    return q === "" || q === "ALL"
  }
  return q === itemTab.toUpperCase()
}

function LiveVodFinsightNavContent({ tabParam }: { tabParam: string | null }) {
  const pathname = usePathname()

  return (
    <nav id="liveVodNavi" aria-label="실시간 VOD 분류">
      <div className="wrap_pc_nav">
        <div className="pc_nav">
          <div className="nav_d1">
            <ul>
              {LIVE_VOD_NAV_ITEMS.map(({ label, href, tab }) => {
                const active = navItemActive(pathname, tabParam, tab)
                return (
                  <li key={tab} className={active ? "on" : undefined}>
                    <Link href={href}>{label}</Link>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>

        <div className="progress_container" aria-hidden>
          <div className="progress_bar" id="liveVodProgressBar" style={{ width: "0%" }} />
        </div>
      </div>

      <div className="m_navi">
        <div className="wrapper list_slider">
          <ul className="navi_w">
            {LIVE_VOD_NAV_ITEMS.map(({ label, href, tab }) => {
              const active = navItemActive(pathname, tabParam, tab)
              return (
                <li key={`m-${tab}`} className={active ? "active" : undefined}>
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

function LiveVodFinsightNavWithSearchParams() {
  const searchParams = useSearchParams()
  return <LiveVodFinsightNavContent tabParam={searchParams.get("tab")} />
}

export default function LiveVodFinsightNav() {
  return (
    <Suspense fallback={<LiveVodFinsightNavContent tabParam={null} />}>
      <LiveVodFinsightNavWithSearchParams />
    </Suspense>
  )
}
