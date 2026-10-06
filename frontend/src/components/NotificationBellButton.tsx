"use client"

import Link from "next/link"
import { useCallback, useEffect, useRef, useState } from "react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { fetchInboxUnreadCount } from "@/lib/inbox"

export default function NotificationBellButton() {
  const { user } = useAuthSession()
  const [unread, setUnread] = useState(0)
  const failStreakRef = useRef(0)
  const cooldownUntilRef = useRef(0)

  const refreshUnread = useCallback(async () => {
    if (!user) {
      setUnread(0)
      return
    }
    if (Date.now() < cooldownUntilRef.current) {
      return
    }
    if (typeof document !== "undefined" && document.visibilityState === "hidden") {
      return
    }
    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      failStreakRef.current += 1
      cooldownUntilRef.current = Date.now() + Math.min(120_000, 15_000 * failStreakRef.current)
      return
    }
    const count = await fetchInboxUnreadCount()
    if (count === null) {
      failStreakRef.current += 1
      cooldownUntilRef.current = Date.now() + Math.min(120_000, 15_000 * failStreakRef.current)
      return
    }
    setUnread(count)
    failStreakRef.current = 0
    cooldownUntilRef.current = 0
  }, [user])

  useEffect(() => {
    void refreshUnread()
    const timer = window.setInterval(() => {
      if (failStreakRef.current >= 3) return
      void refreshUnread()
    }, 30000)

    function onOnline() {
      if (Date.now() < cooldownUntilRef.current) return
      failStreakRef.current = 0
      void refreshUnread()
    }
    function onVisible() {
      if (document.visibilityState !== "visible") return
      if (Date.now() < cooldownUntilRef.current) return
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

  return (
    <div className="relative">
      <Link
        href="/myinfo/activity"
        className={`relative block transition ${unread > 0 ? "text-red-500" : "hover:text-finsight-secondary"}`}
        aria-label={unread > 0 ? `내 활동 보기, 읽지 않은 알림 ${unread}건` : "내 활동 보기"}
        title="내 활동"
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
          <span className="absolute -right-2 -top-2 flex h-[18px] min-w-[18px] items-center justify-center bg-red-600 px-1 text-[10px] font-bold leading-none text-white">
            {unread > 99 ? "99+" : unread}
          </span>
        ) : null}
      </Link>
    </div>
  )
}
