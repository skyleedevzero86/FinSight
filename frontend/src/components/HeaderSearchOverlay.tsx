"use client"

import Image from "next/image"
import Link from "next/link"
import { useCallback, useEffect, useRef, useState } from "react"
import { ChevronRight, Search, X } from "lucide-react"
import { useRouter } from "next/navigation"

type PopularTab = "broadcast" | "news"

const POPULAR_BROADCAST: string[] = [
  "한블리 (한문철의 블랙박스 리뷰)",
  "히든싱어8",
  "냉장고를 부탁해 since 2014",
  "최강야구",
  "아는 형님",
  "싱어게인3",
  "1호가 될 순 없어",
  "유 퀴즈 온 더 블럭",
  "뭉쳐야 찬다",
  "히든싱어6",
]

const POPULAR_NEWS: string[] = [
  "코스피·환율 동향",
  "부동산 정책",
  "반도체 수출",
  "가계대출",
  "금리 인하",
  "AI 규제",
  "지역화폐",
  "전기차 보조금",
  "원자력 발전",
  "ESG 공시",
]

const SUGGESTED_TAGS: string[] = [
  "기후",
  "기상",
  "산불",
  "재난",
  "지진",
  "화재",
  "마약사건",
  "김창민",
  "동계올림픽",
  "22년 지방선거",
]

const MOST_VIEWED_ARTICLES: {
  id: string
  title: string
  timeAgo: string
  duration?: string
  thumb: string
  cornerBadge?: string
}[] = [
  {
    id: "mv1",
    title:
      "'불쾌감' 드러냈던 이 대통령…\"누 끼쳤다\" 정청래 사과로 '사진 금지령' 논란 …",
    timeAgo: "27분 전",
    duration: "01:59",
    thumb: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=400&h=225&fit=crop",
    cornerBadge: "지금 이 뉴스",
  },
  {
    id: "mv2",
    title: "트럼프도 모르게…멜라니아, 깜짝 성명 '엡스타인 연루설' 부인",
    timeAgo: "3시간 전",
    thumb: "https://images.unsplash.com/photo-1529107386315-e1a2ed48a620?w=400&h=225&fit=crop",
  },
  {
    id: "mv3",
    title: "하정우 \"이 대통령 '작업' 발언, 액면 그대로 지시하신대로 봐야\"",
    timeAgo: "55분 전",
    thumb: "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=400&h=225&fit=crop",
  },
]

function formatPopularTimestamp(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  const h = String(d.getHours()).padStart(2, "0")
  const min = String(d.getMinutes()).padStart(2, "0")
  const s = String(d.getSeconds()).padStart(2, "0")
  return `${y}-${m}-${day} ${h}:${min}:${s}기준`
}

type HeaderSearchOverlayProps = {
  open: boolean
  onClose: () => void
}

export default function HeaderSearchOverlay({ open, onClose }: HeaderSearchOverlayProps) {
  const router = useRouter()
  const mobileInputRef = useRef<HTMLInputElement>(null)
  const desktopInputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState("")
  const [popularTab, setPopularTab] = useState<PopularTab>("broadcast")
  const [stamp] = useState(() => formatPopularTimestamp())

  const popularList = popularTab === "broadcast" ? POPULAR_BROADCAST : POPULAR_NEWS

  const submitSearch = useCallback(() => {
    const q = query.trim()
    if (!q) return
    router.push(`/search?q=${encodeURIComponent(q)}`)
    setQuery("")
    onClose()
  }, [query, router, onClose])

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

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/45 p-4 backdrop-blur-[2px]"
      role="presentation"
      onClick={onClose}
    >
      <div
        className="relative flex max-h-[min(90vh,720px)] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-2xl ring-1 ring-black/5 md:max-w-2xl"
        role="dialog"
        aria-modal="true"
        aria-label="검색"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          aria-label="검색 닫기"
          className="absolute right-3 top-3 z-10 rounded-lg p-2 text-gray-600 transition hover:bg-gray-100 hover:text-gray-900"
          onClick={onClose}
        >
          <X className="h-5 w-5" strokeWidth={2} />
        </button>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-6 pt-12 md:px-8 md:pb-8 md:pt-14">
          <div className="flex min-h-0 flex-col md:hidden">
            <form
              className="shrink-0 overflow-hidden rounded-lg border border-finsight-secondary/80 bg-white"
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

            <div className="mt-3 overflow-hidden rounded-lg border border-gray-200 bg-white">
              <div className="flex items-center justify-between border-b border-gray-100 px-3 py-3">
                <span className="text-[15px] font-bold text-black">인기 검색어</span>
                <div className="flex items-center gap-1 text-sm">
                  <button
                    type="button"
                    onClick={() => setPopularTab("broadcast")}
                    className={
                      popularTab === "broadcast"
                        ? "font-medium text-finsight-secondary"
                        : "text-gray-400 hover:text-gray-600"
                    }
                  >
                    방송
                  </button>
                  <span className="text-gray-300" aria-hidden>
                    |
                  </span>
                  <button
                    type="button"
                    onClick={() => setPopularTab("news")}
                    className={
                      popularTab === "news"
                        ? "font-medium text-finsight-secondary"
                        : "text-gray-400 hover:text-gray-600"
                    }
                  >
                    뉴스
                  </button>
                </div>
              </div>
              <ol className="max-h-[min(40vh,320px)] overflow-y-auto px-3 py-2">
                {popularList.map((term, i) => (
                  <li key={`${popularTab}-${i}`} className="flex gap-3 py-2 text-[15px] leading-snug">
                    <span className="w-5 shrink-0 font-semibold text-finsight-secondary">{i + 1}</span>
                    <button
                      type="button"
                      className="text-left text-gray-800 hover:text-finsight-secondary"
                      onClick={() => {
                        setQuery(term)
                        mobileInputRef.current?.focus()
                      }}
                    >
                      {term}
                    </button>
                  </li>
                ))}
              </ol>
              <p className="border-t border-gray-100 px-3 py-2 text-right text-xs text-gray-400">{stamp}</p>
            </div>
          </div>

          <div className="hidden flex-col md:flex">
            <h2 className="pr-10 text-2xl font-bold tracking-tight text-black md:text-[26px] md:leading-snug">
              어떤 기사를 찾으시나요?
            </h2>

            <form
              className="mt-8 shrink-0"
              onSubmit={(e) => {
                e.preventDefault()
                submitSearch()
              }}
            >
              <div className="relative flex items-center rounded-xl border border-finsight-secondary bg-white py-0.5 pl-1 pr-1 focus-within:ring-2 focus-within:ring-finsight-secondary/25">
                <input
                  ref={desktopInputRef}
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="검색어를 입력해주세요"
                  className="min-w-0 flex-1 rounded-lg border-0 bg-transparent py-3.5 pl-4 pr-12 text-[17px] text-gray-900 outline-none placeholder:text-gray-400"
                  autoComplete="off"
                />
                <button
                  type="submit"
                  className="absolute right-2 flex h-10 w-10 items-center justify-center rounded-lg text-gray-900 hover:text-finsight-secondary"
                  aria-label="검색"
                >
                  <Search className="h-6 w-6" strokeWidth={2} />
                </button>
              </div>
            </form>

            <div className="mt-10">
              <p className="mb-3 text-sm font-medium text-gray-500">추천 키워드</p>
              <div className="flex flex-wrap gap-2">
                {SUGGESTED_TAGS.map((tag) => (
                  <button
                    key={tag}
                    type="button"
                    className="rounded-md bg-gray-100 px-3 py-2 text-sm text-gray-700 transition hover:bg-gray-200"
                    onClick={() => {
                      setQuery(tag)
                      desktopInputRef.current?.focus()
                    }}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-8">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="text-lg font-bold text-[#231f20]">많이 본 기사</h3>
                <Link
                  href="#"
                  className="text-[#231f20] transition hover:text-finsight-secondary"
                  aria-label="많이 본 기사 더보기"
                >
                  <ChevronRight className="h-6 w-6" strokeWidth={2} aria-hidden />
                </Link>
              </div>
              <div className="grid grid-cols-3 divide-x divide-[#ebebeb]">
                {MOST_VIEWED_ARTICLES.map((article) => (
                  <Link
                    key={article.id}
                    href="#"
                    className="group block min-w-0 px-3 md:px-4"
                  >
                    <div className="relative mb-3 aspect-video overflow-hidden rounded-lg bg-[#eee]">
                      <Image
                        src={article.thumb}
                        alt=""
                        fill
                        className="object-cover transition group-hover:opacity-95"
                        sizes="(max-width: 768px) 0px, 200px"
                      />
                      {article.cornerBadge ? (
                        <span className="absolute left-2 top-2 rounded bg-black/55 px-1.5 py-0.5 text-[10px] font-medium leading-tight text-white">
                          {article.cornerBadge}
                        </span>
                      ) : null}
                      {article.duration ? (
                        <span className="absolute bottom-2 right-2 rounded bg-black/80 px-1.5 py-0.5 font-mono text-[10px] font-medium text-white">
                          {article.duration}
                        </span>
                      ) : null}
                    </div>
                    <p className="text-sm font-bold leading-snug text-[#231f20] line-clamp-2 group-hover:text-finsight-secondary md:text-[15px]">
                      {article.title}
                    </p>
                    <p className="mt-2 text-xs text-[#9a9a9a]">{article.timeAgo}</p>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
