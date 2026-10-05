"use client"

import { useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight, Pause, Play } from "lucide-react"
import { rewriteDummyAssetUrl, rewriteDummyLinkUrl } from "@/lib/dummyAssetUrl"
import {
  fetchPublicPopupItems,
  formatPopupSlideCounter,
  hideAllPopupsToday,
  isAllPopupsHiddenToday,
  isPopupHiddenToday,
  isPopupInSchedule,
  popupSlidePageSize,
  type PopupItem,
} from "@/lib/popup"

const AUTO_MS = 4500

function visibleWindow(items: PopupItem[], start: number, size: number): PopupItem[] {
  if (items.length === 0 || size <= 0) return []
  const out: PopupItem[] = []
  for (let i = 0; i < size; i += 1) {
    out.push(items[(start + i) % items.length]!)
  }
  return out
}

function PopupCard({ item }: { item: PopupItem }) {
  const image = rewriteDummyAssetUrl(item.imgPath)
  const href = rewriteDummyLinkUrl(item.fileUrl)
  const target = item.linkTarget === "_self" ? "_self" : "_blank"

  const body = (
    <div className="flex h-full min-h-[28rem] w-full flex-col overflow-hidden rounded-md bg-white shadow-[0_8px_28px_rgba(0,0,0,0.35)] sm:min-h-[32rem]">
      {image ? (
        <img
          src={image}
          alt={item.title}
          className="h-full w-full flex-1 object-cover object-top"
        />
      ) : (
        <div className="flex flex-1 items-center justify-center bg-slate-100 px-5 py-10 text-center text-base font-semibold text-slate-800">
          {item.title}
        </div>
      )}
    </div>
  )

  if (!href) return body
  return (
    <a
      href={href}
      target={target}
      rel={target === "_blank" ? "noopener noreferrer" : undefined}
      className="block h-full w-full"
    >
      {body}
    </a>
  )
}

export default function SitePopupLayer() {
  const [items, setItems] = useState<PopupItem[]>([])
  const [start, setStart] = useState(0)
  const [playing, setPlaying] = useState(true)
  const [open, setOpen] = useState(true)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      if (isAllPopupsHiddenToday()) {
        if (!cancelled) setItems([])
        return
      }
      const result = await fetchPublicPopupItems({ size: 50 })
      if (cancelled || !result.ok) return
      const visible = result.data.filter(
        (item) =>
          item.noticeActive === "Y" &&
          isPopupInSchedule(item) &&
          !isPopupHiddenToday(item.id) &&
          Boolean((item.imgPath || "").trim() || (item.title || "").trim()),
      )
      setItems(visible)
      setStart(0)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const total = items.length
  const pageSize = popupSlidePageSize(total)
  const slides = useMemo(
    () => visibleWindow(items, start, pageSize),
    [items, start, pageSize],
  )

  useEffect(() => {
    if (!playing || total <= 1 || !open) return
    const id = window.setInterval(() => {
      setStart((cur) => (cur + 1) % total)
    }, AUTO_MS)
    return () => window.clearInterval(id)
  }, [playing, total, open])

  if (!open || total === 0 || slides.length === 0) return null

  const counter = formatPopupSlideCounter((start % total) + 1, total)

  function goPrev() {
    if (total <= 0) return
    setStart((cur) => (cur - 1 + total) % total)
  }

  function goNext() {
    if (total <= 0) return
    setStart((cur) => (cur + 1) % total)
  }

  function onHideToday() {
    hideAllPopupsToday()
    setOpen(false)
  }

  function onClose() {
    setOpen(false)
  }

  const gridClass =
    pageSize >= 3
      ? "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
      : pageSize === 2
        ? "grid-cols-1 sm:grid-cols-2"
        : "grid-cols-1"

  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/55 p-4 sm:p-6">
      <div
        className="flex w-full max-w-[72rem] flex-col gap-4"
        role="dialog"
        aria-modal="true"
        aria-label="finsight 알리미"
      >
        <div className="relative flex items-center justify-center px-2 pt-1">
          <h2 className="text-center text-[1.35rem] font-semibold tracking-tight text-white sm:text-[1.6rem]">
            finsight 알리미
          </h2>
          <div className="absolute right-0 top-1/2 flex -translate-y-1/2 items-center gap-2 text-white sm:gap-3">
            <span className="hidden font-medium tabular-nums tracking-wide sm:inline" aria-live="polite">
              {counter}
            </span>
            <div className="flex items-center gap-0.5">
              <button
                type="button"
                className="rounded p-1.5 text-white/90 transition hover:bg-white/10 hover:text-white"
                aria-label="이전 팝업"
                onClick={goPrev}
              >
                <ChevronLeft className="h-5 w-5" strokeWidth={2.2} />
              </button>
              <button
                type="button"
                className="rounded p-1.5 text-white/90 transition hover:bg-white/10 hover:text-white"
                aria-label={playing ? "자동 슬라이드 일시정지" : "자동 슬라이드 재생"}
                onClick={() => setPlaying((v) => !v)}
              >
                {playing ? (
                  <Pause className="h-5 w-5" strokeWidth={2.2} />
                ) : (
                  <Play className="h-5 w-5" strokeWidth={2.2} />
                )}
              </button>
              <button
                type="button"
                className="rounded p-1.5 text-white/90 transition hover:bg-white/10 hover:text-white"
                aria-label="다음 팝업"
                onClick={goNext}
              >
                <ChevronRight className="h-5 w-5" strokeWidth={2.2} />
              </button>
            </div>
          </div>
        </div>

        <p className="text-center text-sm font-medium tabular-nums text-white/90 sm:hidden" aria-live="polite">
          {counter}
        </p>

        <div className={`grid gap-3 sm:gap-4 ${gridClass}`}>
          {slides.map((item, idx) => (
            <div key={`${item.id}-${start}-${idx}`} className="min-w-0">
              <PopupCard item={item} />
            </div>
          ))}
        </div>

        <div className="mt-1 flex flex-wrap items-center justify-center gap-3 pb-1">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-full bg-[#1f6b66] px-5 py-2.5 text-sm font-medium text-white transition hover:bg-[#1a5c58]"
            onClick={onHideToday}
          >
            <span
              className="flex h-4 w-4 items-center justify-center rounded-[2px] border border-white/80 text-[10px] leading-none"
              aria-hidden
            >
              ✓
            </span>
            오늘 하루 열지 않기
          </button>
          <button
            type="button"
            className="rounded-full bg-[#1f6b66] px-8 py-2.5 text-sm font-medium text-white transition hover:bg-[#1a5c58]"
            onClick={onClose}
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  )
}
