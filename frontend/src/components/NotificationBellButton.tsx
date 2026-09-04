"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import NotificationInboxPanel from "@/components/NotificationInboxPanel"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { fetchInboxUnreadCount } from "@/lib/inbox"
import { readUsableAccessToken } from "@/lib/finsightToken"

export default function NotificationBellButton() {
  const { user } = useAuthSession()
  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(0)
  const wrapRef = useRef<HTMLDivElement | null>(null)
  const failStreakRef = useRef(0)

  const refreshUnread = useCallback(async () => {
    if (!user || !readUsableAccessToken()) {
      setUnread(0)
      return
    }
    if (typeof document !== "undefined" && document.visibilityState === "hidden") {
      return
    }
    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      failStreakRef.current += 1
      return
    }
    try {
      const count = await fetchInboxUnreadCount()
      setUnread(count)
      failStreakRef.current = 0
    } catch {
      failStreakRef.current += 1
    }
  }, [user])

  useEffect(() => {
    void refreshUnread()
    const timer = window.setInterval(() => {
      if (failStreakRef.current >= 3) return
      void refreshUnread()
    }, 30000)

    function onOnline() {
      failStreakRef.current = 0
      void refreshUnread()
    }
    function onVisible() {
      if (document.visibilityState !== "visible") return
      failStreakRef.current = 0
      void refreshUnread()
    }

    window.addEventListener("online", onOnline)
    document.addEventListener("visibilitychange", onVisible)
    return () => {
      window.clearInterval(timer)
      window.removeEventListener("online", onOnline)
      document.removeEventListener("visibilitychange", onVisible)
    }
  }, [refreshUnread])

  useEffect(() => {
    if (!open) return
    function onDocClick(e: MouseEvent) {
      if (!wrapRef.current?.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false)
    }
    document.addEventListener("mousedown", onDocClick)
    document.addEventListener("keydown", onKey)
    return () => {
      document.removeEventListener("mousedown", onDocClick)
      document.removeEventListener("keydown", onKey)
    }
  }, [open])

  return (
    <div ref={wrapRef} className="relative">
      <button
        type="button"
        className="relative hover:text-finsight-secondary transition"
        aria-expanded={open}
        aria-label="알림함 보기"
        onClick={() => {
          setOpen((v) => !v)
          if (!open) {
            failStreakRef.current = 0
            void refreshUnread()
          }
        }}
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
        {unread > 0 ? (
          <span className="absolute -right-2 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-semibold leading-none text-white">
            {unread > 99 ? "99+" : unread}
          </span>
        ) : null}
      </button>
      <NotificationInboxPanel
        open={open}
        onClose={() => setOpen(false)}
        onUnreadChange={(v) => {
          if (v === 0) {
            setUnread(0)
            return
          }
          if (v < 0) {
            setUnread((c) => Math.max(0, c + v))
            return
          }
          setUnread(v)
        }}
      />
    </div>
  )
}
