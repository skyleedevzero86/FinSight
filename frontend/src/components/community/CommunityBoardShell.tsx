"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useEffect } from "react"
import { COMMUNITY_NAV, type CommunityNavKey } from "@/data/communityBoardData"

function activeKeyFromPath(pathname: string | null): CommunityNavKey {
  if (!pathname) return "free"
  if (pathname.startsWith("/community/notice")) return "notice"
  if (pathname.startsWith("/community/qna")) return "qna"
  return "free"
}

function heroTitleForActive(active: CommunityNavKey): string {
  return COMMUNITY_NAV.find((n) => n.key === active)?.label ?? "커뮤니티"
}

export default function CommunityBoardShell({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const router = useRouter()
  const active = activeKeyFromPath(pathname)
  const heroTitle = heroTitleForActive(active)

  useEffect(() => {
    for (const item of COMMUNITY_NAV) {
      router.prefetch(item.href)
    }
  }, [router])

  return (
    <div className="fcb-page">
      <div className="fcb-vis">
        <div className="fcb-titler">
          <h1 className="fcb-leader">{heroTitle}</h1>
          <div className="fcb-menutab">
            <div className="fcb-dep-wrap">
              <div className="fcb-dep">
                <ul>
                  {COMMUNITY_NAV.map((item) => (
                    <li
                      key={item.key}
                      className={active === item.key ? "fcb-on" : undefined}
                    >
                      <Link href={item.href} prefetch>
                        {item.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="fcb-main">
        <div className="fcb-container">{children}</div>
      </div>
    </div>
  )
}
