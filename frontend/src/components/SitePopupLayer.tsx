"use client"

import { useEffect, useLayoutEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight, Pause, Play } from "lucide-react"
import { rewriteDummyAssetUrl, rewriteDummyLinkUrl } from "@/lib/dummyAssetUrl"
import {
  fetchPublicPopupItems,
  formatPopupSlideCounter,
  hidePopupToday,
  isAllPopupsHiddenToday,
  isPopupHiddenToday,
  isPopupInSchedule,
  popupAllowsHideToday,
  popupSlidePageSize,
  type PopupItem,
} from "@/lib/popup"

const AUTO_MS = 4500
const SLIDE_GAP = 12
const POPUP_CARD_WIDTH = 480

function readPopupCardSize(): { width: number; height: number; offsetTop: number } {
  const layer = document.querySelector("[data-popup-layer]")
  const mainImage = document.querySelector("[data-popup-anchor='main-image']")
  const popular = document.querySelector("[data-popup-anchor='popular-news']")
  if (!mainImage || !popular) return { width: POPUP_CARD_WIDTH, height: 560, offsetTop: 0 }
  const imageTop = mainImage.getBoundingClientRect().top
  const newsTop = popular.getBoundingClientRect().top
  const layerTop = layer?.getBoundingClientRect().top ?? imageTop
  return {
    width: POPUP_CARD_WIDTH,
    height: Math.max(160, Math.round(newsTop - imageTop)),
    offsetTop: Math.round(imageTop - layerTop),
  }
}

function visibleWindow(items: PopupItem[], start: number, size: number): PopupItem[] {
  if (items.length === 0 || size <= 0) return []
  const out: PopupItem[] = []
  for (let i = 0; i < size; i += 1) {
    out.push(items[(start + i) % items.length]!)
  }
  return out
}

function readMainBand(): { top: number; height: number } {
  const header = document.querySelector("header")
  const footer = document.querySelector("footer")
  const headerH = header ? Math.ceil(header.getBoundingClientRect().height) : 0
  const footerTop = footer ? footer.getBoundingClientRect().top : window.innerHeight
  const bottom = Math.min(window.innerHeight, footerTop)
  return { top: headerH, height: Math.max(180, bottom - headerH) }
}

function PopupCard({
  item,
  width,
  height,
}: {
  item: PopupItem
  width: number
  height: number
}) {
  const image = rewriteDummyAssetUrl(item.imgPath)
  const href = rewriteDummyLinkUrl(item.fileUrl)
  const target = item.linkTarget === "_self" ? "_self" : "_blank"

  const body = (
    <div
      className="overflow-hidden rounded-md bg-white shadow-[0_8px_24px_rgba(0,0,0,0.28)]"
      style={{ width, height }}
    >
      {image ? (
        <img src={image} alt={item.title} className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full items-center justify-center px-4 text-center text-sm font-semibold text-slate-800">
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
      className="block shrink-0"
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
  const [band, setBand] = useState({ top: 96, height: 480 })
  const [card, setCard] = useState({ width: POPUP_CARD_WIDTH, height: 560, offsetTop: 0 })

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
  const showHideToday = slides.some(popupAllowsHideToday)

  useEffect(() => {
    if (!open || total === 0) return
    const apply = () => setBand(readMainBand())
    apply()
    window.addEventListener("resize", apply)
    window.addEventListener("scroll", apply, { passive: true })
    return () => {
      window.removeEventListener("resize", apply)
      window.removeEventListener("scroll", apply)
    }
  }, [open, total])

  useLayoutEffect(() => {
    if (!open || total === 0) return
    const apply = () => setCard(readPopupCardSize())
    apply()
    window.addEventListener("resize", apply)
    return () => window.removeEventListener("resize", apply)
  }, [open, total])

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
    const targets = slides.filter(popupAllowsHideToday)
    for (const item of targets) hidePopupToday(item.id)
    const hidden = new Set(targets.map((item) => item.id))
    const remaining = items.filter((item) => !hidden.has(item.id) && !isPopupHiddenToday(item.id))
    setItems(remaining)
    setStart(0)
    if (remaining.length === 0) setOpen(false)
  }

  const controlBtn =
    "inline-flex h-11 w-11 items-center justify-center rounded-full bg-white/15 text-white transition hover:bg-white/25"

  return (
    <div className="pointer-events-none absolute inset-0 z-40">
      <div
        className="pointer-events-auto sticky z-40 overflow-hidden bg-black/40 backdrop-blur-md"
        style={{ top: band.top, height: band.height }}
        data-popup-layer=""
        role="dialog"
        aria-modal="true"
        aria-label="finsight 알리미"
      >
        <div className="pointer-events-none absolute inset-x-0 top-3 z-10 px-4">
          <h2 className="text-center text-2xl font-semibold tracking-tight text-white sm:text-3xl">
            finsight 알리미
          </h2>
          <div className="pointer-events-auto absolute right-4 top-1/2 flex -translate-y-1/2 items-center gap-2">
            <span className="text-sm font-medium tabular-nums text-white" aria-live="polite">
              {counter}
            </span>
            {total > 1 ? (
              <>
                <button type="button" className={controlBtn} aria-label="이전 팝업" onClick={goPrev}>
                  <ChevronLeft className="h-6 w-6" strokeWidth={2.2} />
                </button>
                <button
                  type="button"
                  className={controlBtn}
                  aria-label={playing ? "자동 슬라이드 일시정지" : "자동 슬라이드 재생"}
                  onClick={() => setPlaying((v) => !v)}
                >
                  {playing ? (
                    <Pause className="h-5 w-5" strokeWidth={2.2} />
                  ) : (
                    <Play className="h-5 w-5" strokeWidth={2.2} />
                  )}
                </button>
                <button type="button" className={controlBtn} aria-label="다음 팝업" onClick={goNext}>
                  <ChevronRight className="h-6 w-6" strokeWidth={2.2} />
                </button>
              </>
            ) : null}
          </div>
        </div>

        <div
          className="absolute left-1/2 flex -translate-x-1/2 items-start justify-center"
          style={{ top: card.offsetTop, gap: SLIDE_GAP }}
        >
          {slides.map((item, idx) => (
            <PopupCard
              key={`${item.id}-${start}-${idx}`}
              item={item}
              width={card.width}
              height={card.height}
            />
          ))}
        </div>

        <div
          className="absolute left-1/2 z-10 flex -translate-x-1/2 flex-wrap items-center justify-center gap-3"
          style={{ top: card.offsetTop + card.height + 12 }}
        >
            {showHideToday ? (
              <label className="inline-flex min-h-11 cursor-pointer items-center gap-2 rounded-full bg-[#1f6b66] px-4 text-sm font-medium text-white">
                <input
                  type="checkbox"
                  className="h-5 w-5 accent-white"
                  onChange={onHideToday}
                />
                오늘 하루 열지 않기
              </label>
            ) : null}
            <button
              type="button"
              className="inline-flex min-h-11 items-center rounded-full bg-[#1f6b66] px-8 text-sm font-medium text-white transition hover:bg-[#1a5c58]"
              onClick={() => setOpen(false)}
            >
              닫기
            </button>
        </div>
      </div>
    </div>
  )
}
