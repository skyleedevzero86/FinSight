"use client"

import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react"
import { createPortal } from "react-dom"
import {
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Pause,
  Play,
} from "lucide-react"

export type VodBannerItem = {
  id: string
  title: string
  href: string
}

const DEFAULT_BANNERS: VodBannerItem[] = [
  { id: "1", title: "finsight NOW 최신 클립", href: "https://example.com" },
  { id: "2", title: "경제 Pick 하이라이트", href: "https://example.com" },
  { id: "3", title: "실시간 뉴스 라이브", href: "https://example.com" },
  { id: "4", title: "이벤트 · 시청자 참여", href: "https://example.com" },
  { id: "5", title: "모바일 앱 다운로드", href: "https://example.com" },
  { id: "6", title: "제휴 · 광고 문의", href: "https://example.com" },
]

const AUTOPLAY_MS = 4500

type VODBannersBarProps = {
  title?: string
  banners?: VodBannerItem[]
}

export function VODBannersBar({
  title = "finsight 배너모음",
  banners = DEFAULT_BANNERS,
}: VODBannersBarProps) {
  const trackRef = useRef<HTMLDivElement>(null)
  const [paused, setPaused] = useState(false)
  const [allOpen, setAllOpen] = useState(false)

  const stepScroll = useCallback((dir: 1 | -1) => {
    const track = trackRef.current
    if (!track) return
    const slide = track.querySelector<HTMLElement>("[data-vod-banner-slide]")
    const gap = 12
    const w = slide?.getBoundingClientRect().width ?? 220
    const delta = (w + gap) * dir
    const max = track.scrollWidth - track.clientWidth
    let next = track.scrollLeft + delta
    if (next > max) next = 0
    if (next < 0) next = max
    track.scrollTo({ left: next, behavior: "smooth" })
  }, [])

  useEffect(() => {
    if (paused) return
    const id = window.setInterval(() => stepScroll(1), AUTOPLAY_MS)
    return () => window.clearInterval(id)
  }, [paused, stepScroll])

  useEffect(() => {
    if (!allOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setAllOpen(false)
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [allOpen])

  useEffect(() => {
    if (!allOpen) return
    const prev = document.body.style.overflow
    document.body.style.overflow = "hidden"
    return () => {
      document.body.style.overflow = prev
    }
  }, [allOpen])

  const modal =
    allOpen &&
    typeof document !== "undefined" &&
    createPortal(
      <div
        className="fixed inset-0 z-[120] flex items-center justify-center bg-black/50 p-4"
        role="presentation"
        onClick={() => setAllOpen(false)}
      >
        <div
          className="max-h-[85vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-6 shadow-xl md:max-w-2xl"
          role="dialog"
          aria-modal="true"
          aria-labelledby="vod-banner-all-title"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="mb-4 flex items-center justify-between gap-4 border-b border-gray-200 pb-4">
            <h2
              id="vod-banner-all-title"
              className="text-lg font-bold text-gray-900"
            >
              전체보기
            </h2>
            <button
              type="button"
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
              onClick={() => setAllOpen(false)}
            >
              닫기
            </button>
          </div>
          <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {banners.map((b) => (
              <li key={b.id}>
                <a
                  href={b.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-between gap-2 rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm font-medium text-gray-900 transition hover:border-finsight-secondary hover:bg-finsight-light/30"
                >
                  <span className="min-w-0 truncate">{b.title}</span>
                  <ExternalLink
                    className="h-4 w-4 shrink-0 text-gray-500"
                    aria-hidden
                  />
                </a>
              </li>
            ))}
          </ul>
        </div>
      </div>,
      document.body,
    )

  return (
    <>
      <div className="mt-8 w-screen relative left-1/2 right-1/2 -ml-[50vw] -mr-[50vw] border-t border-gray-200 bg-gray-100 py-4">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 md:flex-row md:items-center md:gap-6 md:px-8">
          <div className="flex flex-wrap items-center gap-3 md:max-w-[min(100%,20rem)] md:shrink-0">
            <h3 className="text-sm font-bold text-gray-900 md:text-[15px]">
              {title}
            </h3>
            <div className="flex items-center gap-1 border-l border-gray-300 pl-3">
              <button
                type="button"
                className="rounded-md p-1.5 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
                aria-label="이전 배너"
                onClick={() => stepScroll(-1)}
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <button
                type="button"
                className="rounded-md p-1.5 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
                aria-label={paused ? "자동 슬라이드 재생" : "자동 슬라이드 일시정지"}
                onClick={() => setPaused((p) => !p)}
              >
                {paused ? (
                  <Play className="h-4 w-4" />
                ) : (
                  <Pause className="h-4 w-4" />
                )}
              </button>
              <button
                type="button"
                className="rounded-md p-1.5 text-gray-600 hover:bg-gray-200 hover:text-gray-900"
                aria-label="다음 배너"
                onClick={() => stepScroll(1)}
              >
                <ChevronRight className="h-5 w-5" />
              </button>
              <button
                type="button"
                className="ml-1 rounded border border-gray-400 bg-white px-2.5 py-1 text-xs font-medium text-gray-800 hover:bg-gray-50"
                onClick={() => setAllOpen(true)}
              >
                전체보기
              </button>
            </div>
          </div>

          <div className="min-w-0 flex-1 overflow-hidden">
            <div
              ref={trackRef}
              className="-m-3 flex snap-x snap-mandatory gap-3 overflow-x-auto scroll-smooth pb-1 pl-3 pr-3 pt-1 [scrollbar-width:none] md:gap-3 [&::-webkit-scrollbar]:hidden"
            >
              {banners.map((b) => (
                <div
                  key={b.id}
                  data-vod-banner-slide
                  className="w-[min(100%,260px)] shrink-0 snap-start sm:w-[220px] md:w-[200px] lg:w-[220px]"
                >
                  <a
                    href={b.href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex h-[52px] items-center justify-between gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-900 shadow-sm transition hover:border-finsight-secondary hover:shadow-md"
                  >
                    <span className="min-w-0 truncate">{b.title}</span>
                    <ExternalLink
                      className="h-3.5 w-3.5 shrink-0 text-gray-500"
                      aria-hidden
                    />
                  </a>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
      {modal}
    </>
  )
}
