"use client"

import Link from "next/link"
import { Search, Menu, User } from "lucide-react"
import { useState } from "react"
import { useRouter } from "next/navigation"
import HeaderSearchOverlay from "@/components/HeaderSearchOverlay"
import BrandLogo from "@/components/BrandLogo"
import { useAuthSession } from "@/components/AuthSessionProvider"
import NotificationBellButton from "@/components/NotificationBellButton"

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
                href="/myinfo"
                className="flex max-w-[11rem] items-center gap-1.5 hover:text-finsight-secondary transition"
                title={user.nickname}
              >
                <HeaderAvatar src={user.profileImageUrl} />
                <span className="truncate">{user.nickname}</span>
              </Link>
              <span className="text-gray-400">|</span>
              <Link href="/myinfo/history" className="hover:text-finsight-secondary transition">
                시청 기록
              </Link>
              <span className="text-gray-400">|</span>
              <Link href="/myinfo/favorites" className="hover:text-finsight-secondary transition">
                나의 즐겨찾기
              </Link>
              <span className="text-gray-400">|</span>
              <Link href="/myinfo/posts" className="hover:text-finsight-secondary transition">
                나의 게시글
              </Link>
              {canManageUsers(user.role) ? (
                <>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/stats" className="hover:text-finsight-secondary transition">
                    통계
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/mainimg" className="hover:text-finsight-secondary transition">
                    메인이미지
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/popup" className="hover:text-finsight-secondary transition">
                    팝업
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/ulink" className="hover:text-finsight-secondary transition">
                    통합링크
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/moderation" className="hover:text-finsight-secondary transition">
                    신고 관리
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/users" className="hover:text-finsight-secondary transition">
                    사용자 관리
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/email-logs" className="hover:text-finsight-secondary transition">
                    메일 이력
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/notifications" className="hover:text-finsight-secondary transition">
                    알림
                  </Link>
                  <span className="text-gray-400">|</span>
                  <Link href="/admin/sms" className="hover:text-finsight-secondary transition">
                    SMS
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
            {user ? <NotificationBellButton /> : null}
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
              {user ? (
                <>
                  <li>
                    <Link
                      href="/myinfo/history"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      시청 기록
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/myinfo/favorites"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      나의 즐겨찾기
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/myinfo/posts"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      나의 게시글
                    </Link>
                  </li>
                </>
              ) : null}
              {user && canManageUsers(user.role) ? (
                <>
                  <li>
                    <Link
                      href="/admin/stats"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      통계
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/mainimg"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      메인이미지
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/popup"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      팝업
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/ulink"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      통합링크
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/moderation"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      신고 관리
                    </Link>
                  </li>
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
                  <li>
                    <Link
                      href="/admin/notifications"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      알림
                    </Link>
                  </li>
                  <li>
                    <Link
                      href="/admin/sms"
                      className="block rounded-md px-2 py-2.5 text-sm hover:bg-white/5 hover:text-finsight-secondary transition"
                    >
                      SMS
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
