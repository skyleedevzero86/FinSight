"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Dumbbell, FileText, Globe2, MessagesSquare, Settings } from "lucide-react"

const MENU_ITEMS = [
  { href: "/myinfo/userinfo", label: "나의정보", icon: Globe2 },
  { href: "/myinfo/history", label: "시청기록", icon: MessagesSquare },
  { href: "/myinfo/favorites", label: "나의즐겨찾기", icon: Settings },
  { href: "/myinfo/posts", label: "나의게시글", icon: FileText },
  { href: "/myinfo/portfolio", label: "나의포트폴리오", icon: Dumbbell },
] as const

function isActivePath(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`)
}

export default function MyInfoSidebar() {
  const pathname = usePathname()

  return (
    <aside
      className="flex w-[13.75rem] shrink-0 flex-col self-stretch bg-[#1a1f2e] text-white md:w-60"
      aria-label="나의메뉴"
    >
      <div className="px-6 pb-4 pt-10">
        <Link
          href="/myinfo"
          className="inline-block text-[17px] font-bold tracking-tight text-white hover:text-white/90"
        >
          나의메뉴
        </Link>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 px-3 pb-12" aria-label="나의 메뉴 목록">
        {MENU_ITEMS.map((item) => {
          const Icon = item.icon
          const active = isActivePath(pathname, item.href)
          return (
            <Link
              key={item.href}
              href={item.href}
              className={[
                "flex items-center gap-3 rounded-md px-3 py-3.5 text-[13px] transition",
                active
                  ? "bg-white/15 text-white"
                  : "text-white/90 hover:bg-white/10 hover:text-white",
              ].join(" ")}
              aria-current={active ? "page" : undefined}
            >
              <Icon className="h-[18px] w-[18px] shrink-0 opacity-95" strokeWidth={1.7} aria-hidden />
              <span className="leading-none">{item.label}</span>
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}
