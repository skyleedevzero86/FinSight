"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { usePathname } from "next/navigation"
import {
  Bookmark,
  Briefcase,
  ChevronUp,
  ClipboardList,
  ImageIcon,
  Link2,
  Mail,
  Megaphone,
  MessageSquare,
  PanelTop,
  Server,
  Settings,
  ShieldAlert,
  User,
  UserCircle,
  type LucideIcon,
} from "lucide-react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"

type MenuLink = {
  href: string
  label: string
  icon: LucideIcon
}

const USER_MENU_TOP: MenuLink[] = [
  { href: "/myinfo/userinfo", label: "나의정보", icon: UserCircle },
  { href: "/myinfo/history", label: "시청기록", icon: MessageSquare },
]

const USER_MENU_BOTTOM: MenuLink[] = [
  { href: "/myinfo/favorites", label: "나의즐겨찾기", icon: Bookmark },
  { href: "/myinfo/posts", label: "나의게시글", icon: Briefcase },
  { href: "/myinfo/portfolio", label: "나의포트폴리오", icon: ClipboardList },
]

const SYSTEM_MENU_ITEMS: MenuLink[] = [
  { href: "/admin/users", label: "사용자 관리", icon: User },
  { href: "/admin/health", label: "서버현황", icon: Server },
  { href: "/admin/mainimg", label: "메인이미지", icon: ImageIcon },
  { href: "/admin/popup", label: "팝업관리", icon: PanelTop },
  { href: "/admin/ulink", label: "통합링크", icon: Link2 },
  { href: "/admin/notifications", label: "공지사항", icon: Megaphone },
  { href: "/admin/email-logs", label: "메일이력", icon: Mail },
  { href: "/admin/stats", label: "통계", icon: ClipboardList },
]

const ADMIN_EXTRA_ITEMS: MenuLink[] = [
  { href: "/admin/moderation", label: "신고관리", icon: ShieldAlert },
]

function isActivePath(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`)
}

function isSystemPath(pathname: string): boolean {
  return SYSTEM_MENU_ITEMS.some((item) => isActivePath(pathname, item.href))
}

function MenuItemLink({
  item,
  pathname,
  nested,
}: {
  item: MenuLink
  pathname: string
  nested?: boolean
}) {
  const Icon = item.icon
  const active = isActivePath(pathname, item.href)
  return (
    <Link
      href={item.href}
      className={[
        "flex items-center gap-3 rounded-md text-[13px] transition",
        nested ? "px-3 py-2.5 pl-4" : "px-3 py-3.5",
        active
          ? nested
            ? "text-sky-400"
            : "bg-white/15 text-white"
          : "text-white/90 hover:bg-white/10 hover:text-white",
      ].join(" ")}
      aria-current={active ? "page" : undefined}
    >
      <Icon
        className={[
          "h-[18px] w-[18px] shrink-0",
          active && nested ? "text-sky-400" : "opacity-95",
        ].join(" ")}
        strokeWidth={1.7}
        aria-hidden
      />
      <span className="leading-none">{item.label}</span>
    </Link>
  )
}

export default function MyInfoSidebar() {
  const pathname = usePathname()
  const { user } = useAuthSession()
  const isAdmin = canManageUsers(user?.role)
  const [systemOpen, setSystemOpen] = useState(() => isSystemPath(pathname))

  useEffect(() => {
    if (isSystemPath(pathname)) setSystemOpen(true)
  }, [pathname])

  return (
    <aside
      className="flex min-h-full w-[13.75rem] shrink-0 flex-col self-stretch bg-[#1a1f2e] text-white md:w-60"
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
        {USER_MENU_TOP.map((item) => (
          <MenuItemLink key={item.href} item={item} pathname={pathname} />
        ))}

        {isAdmin ? (
          <div className="flex flex-col gap-0.5">
            <button
              type="button"
              className="flex w-full items-center gap-3 rounded-md px-3 py-3.5 text-[13px] text-white/90 transition hover:bg-white/10 hover:text-white"
              aria-expanded={systemOpen}
              onClick={() => setSystemOpen((open) => !open)}
            >
              <Settings className="h-[18px] w-[18px] shrink-0 opacity-95" strokeWidth={1.7} aria-hidden />
              <span className="flex-1 text-left leading-none">시스템 관리</span>
              <ChevronUp
                className={[
                  "h-4 w-4 shrink-0 opacity-80 transition-transform",
                  systemOpen ? "" : "rotate-180",
                ].join(" ")}
                strokeWidth={1.8}
                aria-hidden
              />
            </button>
            {systemOpen ? (
              <div className="flex flex-col gap-0.5 pb-1" role="group" aria-label="시스템 관리 하위 메뉴">
                {SYSTEM_MENU_ITEMS.map((item) => (
                  <MenuItemLink key={item.href} item={item} pathname={pathname} nested />
                ))}
              </div>
            ) : null}
          </div>
        ) : null}

        {USER_MENU_BOTTOM.map((item) => (
          <MenuItemLink key={item.href} item={item} pathname={pathname} />
        ))}

        {isAdmin
          ? ADMIN_EXTRA_ITEMS.map((item) => (
              <MenuItemLink key={item.href} item={item} pathname={pathname} />
            ))
          : null}
      </nav>
    </aside>
  )
}
