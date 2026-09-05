"use client"

import { useEffect, useState } from "react"
import {
  POPUP_DEFAULT_HEIGHT,
  POPUP_DEFAULT_WIDTH,
  fetchPublicPopupItems,
  hidePopupToday,
  isPopupHiddenToday,
  isPopupInSchedule,
  type PopupItem,
} from "@/lib/popup"

export default function SitePopupLayer() {
  const [queue, setQueue] = useState<PopupItem[]>([])
  const [index, setIndex] = useState(0)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const result = await fetchPublicPopupItems({ size: 20 })
      if (cancelled || !result.ok) return
      const visible = result.data.filter(
        (item) =>
          item.noticeActive === "Y" &&
          isPopupInSchedule(item) &&
          !isPopupHiddenToday(item.id) &&
          Boolean((item.imgPath || "").trim() || (item.title || "").trim()),
      )
      setQueue(visible)
      setIndex(0)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const current = queue[index] ?? null
  if (!current) return null

  const width =
    current.widthSize && current.widthSize > 0 ? current.widthSize : POPUP_DEFAULT_WIDTH
  const height =
    current.verticalSize && current.verticalSize > 0
      ? current.verticalSize
      : POPUP_DEFAULT_HEIGHT
  const style: React.CSSProperties = {
    width: Math.min(width, typeof window !== "undefined" ? window.innerWidth - 32 : width),
    maxHeight: Math.min(
      height,
      typeof window !== "undefined" ? window.innerHeight - 48 : height,
    ),
  }

  function dismiss(hideToday: boolean) {
    if (hideToday && current.stopTodayHide === "Y") {
      hidePopupToday(current.id)
    }
    setIndex((i) => i + 1)
  }

  const image = (current.imgPath || "").trim()
  const href = (current.fileUrl || "").trim()
  const target = current.linkTarget === "_self" ? "_self" : "_blank"

  const body = (
    <>
      {image ? (
        <img src={image} alt={current.title} className="block w-full object-cover" />
      ) : (
        <div className="bg-gray-100 px-6 py-10 text-center text-base font-medium text-gray-800">
          {current.title}
        </div>
      )}
    </>
  )

  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/45 p-4">
      <div
        className="overflow-hidden rounded-lg bg-white shadow-xl"
        style={style}
        role="dialog"
        aria-modal="true"
        aria-label={current.title}
      >
        <div className="flex items-center justify-between border-b border-gray-100 px-3 py-2">
          <h2 className="truncate text-sm font-semibold text-gray-900">{current.title}</h2>
          <button
            type="button"
            className="rounded px-2 py-1 text-sm text-gray-500 hover:bg-gray-100"
            onClick={() => dismiss(false)}
          >
            닫기
          </button>
        </div>
        {href ? (
          <a href={href} target={target} rel={target === "_blank" ? "noopener noreferrer" : undefined}>
            {body}
          </a>
        ) : (
          body
        )}
        <div className="flex items-center justify-between gap-2 border-t border-gray-100 px-3 py-2">
          {current.stopTodayHide === "Y" ? (
            <button
              type="button"
              className="text-xs text-gray-600 hover:text-gray-900"
              onClick={() => dismiss(true)}
            >
              오늘 하루 보지 않기
            </button>
          ) : (
            <span />
          )}
          <button
            type="button"
            className="rounded bg-gray-900 px-3 py-1.5 text-xs text-white"
            onClick={() => dismiss(false)}
          >
            확인
          </button>
        </div>
      </div>
    </div>
  )
}
