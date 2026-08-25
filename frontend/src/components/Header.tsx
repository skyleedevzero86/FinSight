"use client"

import Link from "next/link"
import { Search, Menu, User } from "lucide-react"
import { useState } from "react"
import { useRouter } from "next/navigation"
import HeaderSearchOverlay from "@/components/HeaderSearchOverlay"
import BrandLogo from "@/components/BrandLogo"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"

function HeaderAvatar({ src }: { src: string | null }) {
  const [broken, setBroken] = useState(false)
  const useSnsPhoto = Boolean(src) && !broken

  if (useSnsPhoto) {
    return (
      <img
        src={src as string}
        alt=""
        className="h-5 w-5 shrink-0 rounded-full object-cover bg-white/20"
        onError={() => setBroken(true)}
      />
    )
  }

  return (
    <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white/25">
      <User className="h-3.5 w-3.5" strokeWidth={2.2} aria-hidden />
    </span>
  )
}

export default function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const router = useRouter()
  const { user, logout } = useAuthSession()

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    try {
      await logout()
      router.replace("/")
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <header className="bg-finsight-primary text-white sticky top-0 z-50">
      <div className="relative">
        <div className="absolute right-0 top-0 px-4 py-2 text-xs flex items-center gap-4 min-h-[2rem]">
          {user ? (
            <>
              <Link
                href="/my"
                className="flex max-w-[11rem] items-center gap-1.5 hover:text-finsight-secondary transition"
                title={user.nickname}
              >
                <HeaderAvatar src={user.profileImageUrl} />
                <span className="truncate">{user.nickname}</span>
              </Link>
              {canManageUsers(user.role) ? (
                <>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/users" className="hover:text-finsight-secondary transition">
                    사용자 관리
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/email-logs" className="hover:text-finsight-secondary transition">
                    메일 이력
                  </Link>
                </>
              ) : null}
              <span className="text-gray-400">|</span>
              <button
                type="button"
                onClick={() => void onLogout()}
                disabled={loggingOut}
                className="hover:text-finsight-secondary transition disabled:opacity-60"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link href="/signup" className="hover:text-finsight-secondary transition">회원가입</Link>
              <span className="text-gray-400">|</span>
              <Link href="/login" className="hover:text-finsight-secondary transition">로그인</Link>
            </>
          )}
        </div>

        <nav className="flex items-center justify-between px-4 md:px-8 pt-10 pb-4">
          <BrandLogo />

          <div className="ml-auto flex items-center gap-6">
            <Link href="/news" className="text-sm hover:text-finsight-secondary transition hidden md:block">
              뉴스
            </Link>
            <Link href="/economy-pick" className="text-sm hover:text-finsight-secondary transition hidden md:block">
              경제 Pick
            </Link>
            <Link href="/live-vod" className="text-sm hover:text-finsight-secondary transition hidden md:block">
              실시간 VOD
            </Link>
            <Link
              href="/community"
              className="text-sm hover:text-finsight-secondary transition hidden md:block"
            >
              커뮤니티
            </Link>
            <button
              type="button"
              className="hover:text-finsight-secondary transition"
              aria-expanded={searchOpen}
              aria-controls="site-search-layer"
              aria-label="검색 열기"
              onClick={() => setSearchOpen(true)}
            >
              <Search className="w-5 h-5" />
            </button>
            {user ? (
              <button
                type="button"
                className="hover:text-finsight-secondary transition"
                aria-expanded={false}
                aria-label="알림함 보기"
              >
                <svg
                  width="20"
                  height="22"
                  viewBox="0 0 20 22"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className="shrink-0"
                  aria-hidden
                >
                  <g>
                    <path
                      d="M8.16611 16.55C7.75272 16.996 7.5 17.593 7.5 18.2491C7.5 19.6298 8.61929 20.7491 10 20.7491C11.3807 20.7491 12.5 19.6298 12.5 18.2491C12.5 17.593 12.2473 16.996 11.8339 16.55"
                      stroke="currentColor"
                      strokeWidth="1.8"
                    />
                    <path
                      d="M3 13.537L3.82923 13.8869L3.9 13.7191V13.537H3ZM1.75 16.5L0.920772 16.1502L0.3935 17.4H1.75V16.5ZM18.25 16.5V17.4H19.6065L19.0792 16.1502L18.25 16.5ZM17 13.537H16.1V13.7191L16.1708 13.8869L17 13.537ZM3.9 7.75C3.9 4.65721 6.40721 2.15 9.5 2.15V0.35C5.41309 0.35 2.1 3.66309 2.1 7.75H3.9ZM3.9 13.537V7.75H2.1V13.537H3.9ZM2.57923 16.8498L3.82923 13.8869L2.17077 13.1872L0.920772 16.1502L2.57923 16.8498ZM18.25 15.6H1.75V17.4H18.25V15.6ZM16.1708 13.8869L17.4208 16.8498L19.0792 16.1502L17.8292 13.1872L16.1708 13.8869ZM16.1 7.75V13.537H17.9V7.75H16.1ZM10.5 2.15C13.5928 2.15 16.1 4.65721 16.1 7.75H17.9C17.9 3.66309 14.5869 0.35 10.5 0.35V2.15ZM9.5 2.15H10.5V0.35H9.5V2.15Z"
                      fill="currentColor"
                    />
                  </g>
                </svg>
              </button>
            ) : null}
            <button
              type="button"
              className="md:hidden hover:text-finsight-secondary transition"
              aria-expanded={isMenuOpen}
              aria-label={isMenuOpen ? "메뉴 닫기" : "메뉴 열기"}
              onClick={() => setIsMenuOpen((open) => !open)}
            >
              <Menu className="w-5 h-5" />
            </button>
          </div>
        </nav>

        {isMenuOpen && (
          <div className="md:hidden border-t border-white/10 bg-finsight-dark px-4 py-4">
            <ul className="flex flex-col gap-1">
              <li>
                <Link
                  href="/news"
                  className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                >
                  뉴스
                </Link>
              </li>
              <li>
                <Link
                  href="/economy-pick"
                  className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                >
                  경제PICK
                </Link>
              </li>
              <li>
                <Link
                  href="/live-vod"
                  className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                >
                  실시간 VOD
                </Link>
              </li>
              <li>
                <Link
                  href="/community"
                  className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                >
                  커뮤니티
                </Link>
              </li>
              {user && canManageUsers(user.role) ? (
                <>
                  <li>
                    <Link
                      href="/admin/users"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      사용자 관리
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/email-logs"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      메일 이력
                    </Link>
                  </li>
                </>
              ) : null}
            </ul>
          </div>
        )}
      </div>

      <div id="site-search-layer">
        <HeaderSearchOverlay open={searchOpen} onClose={() => setSearchOpen(false)} />
      </div>
    </header>
  )
}
