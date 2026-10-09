"use client"

import Link from "next/link"
import { useCallback, useEffect, useRef, useState } from "react"
import { ChevronRight, Search, X } from "lucide-react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  boardDetailPath,
  boardListPath,
  watchPopularBoardCards,
  type PopularBoardCard,
} from "@/lib/boardApi"
import { BOARD_HISTORY_PLACEHOLDER } from "@/lib/browseHistory"
import { promotePlaceholderLabel } from "@/lib/searchKeywords"

type HeaderSearchOverlayProps = {
  open: boolean
  onClose: () => void
}

export default function HeaderSearchOverlay({ open, onClose }: HeaderSearchOverlayProps) {
  const router = useRouter()
  const { user } = useAuthSession()
  const mobileInputRef = useRef<HTMLInputElement>(null)
  const desktopInputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState("")
  const [popularPosts, setPopularPosts] = useState<PopularBoardCard[]>([])
  const [popularReady, setPopularReady] = useState(false)
  const [keywords, setKeywords] = useState<string[]>([])
  const [keywordsReady, setKeywordsReady] = useState(false)
  const moreHref = user
    ? "/myinfo/history"
    : boardListPath(popularPosts[0]?.boardType ?? "NOTICE")

  const applyKeyword = useCallback((keyword: string) => {
    setQuery(keyword)
    if (window.matchMedia("(min-width: 768px)").matches) {
      desktopInputRef.current?.focus()
      return
    }
    mobileInputRef.current?.focus()
  }, [])

  const submitSearch = useCallback(() => {
    const q = query.trim()
    router.push(q ? `/search?q=${encodeURIComponent(q)}` : "/search")
    setQuery("")
    onClose()
  }, [query, router, onClose])

  useEffect(() => {
    if (!open) return
    setPopularReady(false)
    setKeywordsReady(false)
    return watchPopularBoardCards(
      3,
      (rows) => setPopularPosts(rows),
      () => {
        setPopularReady(true)
        setKeywordsReady(true)
      },
      (next) => setKeywords(next),
    )
  }, [open])

  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = "hidden"
    const t = window.setTimeout(() => {
      if (window.matchMedia("(min-width: 768px)").matches) {
        desktopInputRef.current?.focus()
      } else {
        mobileInputRef.current?.focus()
      }
    }, 0)
    return () => {
      document.body.style.overflow = prev
      window.clearTimeout(t)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose()
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [open, onClose])

  const popularPostsSection = (
    <div className="mt-4 md:mt-8">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-bold text-[#231f20]">많이 본 게시물</h3>
        <Link
          href={moreHref}
          onClick={onClose}
          className="text-[#231f20] transition hover:text-finsight-secondary"
          aria-label="많이 본 게시물 더보기"
        >
          <ChevronRight className="h-6 w-6" strokeWidth={2} aria-hidden />
        </Link>
      </div>
      {!popularReady && popularPosts.length === 0 ? (
        <p className="text-sm text-[#9a9a9a]">게시물을 불러오는 중입니다.</p>
      ) : popularPosts.length === 0 ? (
        <p className="text-sm text-[#9a9a9a]">아직 조회된 게시물이 없습니다.</p>
      ) : (
        <div className="grid grid-cols-3 divide-x divide-[#ebebeb]">
          {popularPosts.map((post) => (
            <Link
              key={post.id}
              href={boardDetailPath(post.boardType, post.id)}
              onClick={onClose}
              className="group block min-w-0 px-3 md:px-4"
            >
              <div className="relative mb-3 aspect-video overflow-hidden bg-[#eee]">
                <img
                  src={BOARD_HISTORY_PLACEHOLDER}
                  alt=""
                  className="absolute inset-0 h-full w-full object-cover transition group-hover:opacity-95"
                />
              </div>
              <p className="line-clamp-2 text-sm font-bold leading-snug text-[#231f20] group-hover:text-finsight-secondary md:text-[15px]">
                {promotePlaceholderLabel(post.title)}
              </p>
              <p className="mt-2 text-right text-xs text-[#9a9a9a]">{post.timeAgo}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  )

  const keywordSection = (
    <div className="mt-4 md:mt-8">
      <p className="mb-3 text-sm font-medium text-gray-500">추천 키워드</p>
      {!keywordsReady && keywords.length === 0 ? (
        <p className="text-sm text-[#9a9a9a]">추천 키워드를 불러오는 중입니다.</p>
      ) : keywords.length === 0 ? (
        <p className="text-sm text-[#9a9a9a]">추천할 키워드가 없습니다.</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {[...new Set(keywords.map(promotePlaceholderLabel))].map((tag) => (
            <button
              key={tag}
              type="button"
              className="bg-gray-100 px-3 py-2 text-sm text-gray-700 transition hover:bg-gray-200"
              onClick={() => applyKeyword(tag)}
            >
              {tag}
            </button>
          ))}
        </div>
      )}
    </div>
  )

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/45 p-4 backdrop-blur-[2px]"
      role="presentation"
      onClick={onClose}
    >
      <div
        className="relative flex max-h-[min(90vh,720px)] w-full max-w-lg flex-col overflow-hidden bg-white shadow-2xl ring-1 ring-black/5 md:max-w-2xl"
        role="dialog"
        aria-modal="true"
        aria-label="검색"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          aria-label="검색 닫기"
          className="absolute right-3 top-3 z-10 p-2 text-gray-600 transition hover:bg-gray-100 hover:text-gray-900"
          onClick={onClose}
        >
          <X className="h-5 w-5" strokeWidth={2} />
        </button>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-6 pt-12 md:px-8 md:pb-8 md:pt-14">
          <div className="flex min-h-0 flex-col md:hidden">
            <form
              className="shrink-0 overflow-hidden border border-finsight-secondary/80 bg-white"
              onSubmit={(e) => {
                e.preventDefault()
                submitSearch()
              }}
            >
              <div className="flex items-stretch">
                <input
                  ref={mobileInputRef}
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="검색어를 입력해주세요"
                  className="min-w-0 flex-1 border-0 bg-transparent px-3 py-3 text-[15px] text-gray-900 outline-none placeholder:text-gray-400"
                  autoComplete="off"
                />
                <button
                  type="submit"
                  className="flex shrink-0 items-center justify-center px-3 text-gray-900 hover:text-finsight-secondary"
                  aria-label="검색"
                >
                  <Search className="h-5 w-5" />
                </button>
              </div>
            </form>

            {popularPostsSection}
            {keywordSection}
          </div>

          <div className="hidden flex-col md:flex">
            <h2 className="pr-10 text-2xl font-bold tracking-tight text-black md:text-[26px] md:leading-snug">
              어떤 금융정보를 찾으시나요?
            </h2>

            <form
              className="mt-8 shrink-0"
              onSubmit={(e) => {
                e.preventDefault()
                submitSearch()
              }}
            >
              <div className="relative flex items-center border border-finsight-secondary bg-white py-0.5 pl-1 pr-1 focus-within:ring-2 focus-within:ring-finsight-secondary/25">
                <input
                  ref={desktopInputRef}
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="검색어를 입력해주세요"
                  className="min-w-0 flex-1 border-0 bg-transparent py-3.5 pl-4 pr-12 text-[17px] text-gray-900 outline-none placeholder:text-gray-400"
                  autoComplete="off"
                />
                <button
                  type="submit"
                  className="absolute right-2 flex h-10 w-10 items-center justify-center text-gray-900 hover:text-finsight-secondary"
                  aria-label="검색"
                >
                  <Search className="h-6 w-6" strokeWidth={2} />
                </button>
              </div>
            </form>

            {popularPostsSection}
            {keywordSection}
          </div>
        </div>
      </div>
    </div>
  )
}
