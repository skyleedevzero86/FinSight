"use client"

import Link from "next/link"
import { User } from "lucide-react"
import { useCallback, useEffect, useRef, useState } from "react"
import {
  deleteAllInbox,
  fetchInboxPage,
  fetchInboxSettings,
  formatInboxTime,
  markAllInboxRead,
  markInboxRead,
  updateInboxSettings,
  type InboxItem,
  type InboxSettings,
} from "@/lib/inbox"

type Tab = "all" | "unread"

function Avatar({ src, name }: { src: string | null; name: string | null }) {
  const [broken, setBroken] = useState(false)
  if (src && !broken) {
    return (
      <img
        src={src}
        alt=""
        className="h-10 w-10 shrink-0 rounded-full object-cover bg-gray-100"
        onError={() => setBroken(true)}
      />
    )
  }
  const initial = name?.trim()?.charAt(0) || "?"
  return (
    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-teal-50 text-sm font-medium text-teal-700">
      {initial === "?" ? <User className="h-5 w-5" aria-hidden /> : initial}
    </span>
  )
}

export default function NotificationInboxPanel({
  open,
  onClose,
  onUnreadChange,
}: {
  open: boolean
  onClose: () => void
  onUnreadChange: (count: number) => void
}) {
  const [tab, setTab] = useState<Tab>("all")
  const [showSettings, setShowSettings] = useState(false)
  const [items, setItems] = useState<InboxItem[]>([])
  const [page, setPage] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [settings, setSettings] = useState<InboxSettings | null>(null)
  const [savingSettings, setSavingSettings] = useState(false)
  const scrollRef = useRef<HTMLDivElement | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)
  const loadingMoreLock = useRef(false)

  const loadPage = useCallback(
    async (pageToLoad: number, replace: boolean) => {
      if (replace) setLoading(true)
      else {
        if (loadingMoreLock.current) return
        loadingMoreLock.current = true
        setLoadingMore(true)
      }
      setError(null)
      const result = await fetchInboxPage({
        page: pageToLoad,
        size: 15,
        unreadOnly: tab === "unread",
      })
      if (replace) setLoading(false)
      else {
        setLoadingMore(false)
        loadingMoreLock.current = false
      }
      if (!result.ok) {
        setError(result.message)
        return
      }
      setPage(result.page.page)
      setHasNext(result.page.hasNext)
      setItems((prev) => (replace ? result.page.content : [...prev, ...result.page.content]))
    },
    [tab]
  )

  useEffect(() => {
    if (!open) return
    setShowSettings(false)
    setItems([])
    setPage(0)
    setHasNext(false)
    void loadPage(0, true)
    void fetchInboxSettings().then((s) => {
      if (s) setSettings(s)
    })
  }, [open, tab, loadPage])

  useEffect(() => {
    if (!open || showSettings || !hasNext || loading || loadingMore) return
    const root = scrollRef.current
    const sentinel = sentinelRef.current
    if (!root || !sentinel) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          void loadPage(page + 1, false)
        }
      },
      { root, rootMargin: "80px", threshold: 0 }
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [open, showSettings, hasNext, loading, loadingMore, page, loadPage])

  async function onMarkAllRead() {
    const ok = await markAllInboxRead()
    if (!ok) return
    setItems((prev) => prev.map((i) => ({ ...i, read: true })))
    onUnreadChange(0)
    if (tab === "unread") {
      setItems([])
      setHasNext(false)
    }
  }

  async function onDeleteAll() {
    const ok = await deleteAllInbox()
    if (!ok) return
    setItems([])
    setHasNext(false)
    onUnreadChange(0)
  }

  async function onItemClick(item: InboxItem) {
    if (!item.read) {
      const ok = await markInboxRead(item.id)
      if (ok) {
        setItems((prev) =>
          prev.map((i) => (i.id === item.id ? { ...i, read: true } : i))
        )
        onUnreadChange(-1)
      }
    }
    onClose()
  }

  async function onSaveSettings() {
    if (!settings) return
    setSavingSettings(true)
    const result = await updateInboxSettings(settings)
    setSavingSettings(false)
    if (result.ok) setSettings(result.settings)
  }

  if (!open) return null

  return (
    <div
      className="absolute right-0 top-full z-[60] mt-2 w-[min(100vw-1.5rem,26rem)] overflow-hidden rounded-xl border border-gray-200 bg-white text-gray-900 shadow-xl"
      role="dialog"
      aria-label="알림함"
    >
      <div className="flex items-center justify-between gap-2 border-b border-gray-100 px-3 py-2.5">
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => {
              setShowSettings(false)
              setTab("all")
            }}
            className={`rounded-full px-3 py-1 text-sm font-medium transition ${
              !showSettings && tab === "all"
                ? "bg-teal-50 text-teal-700"
                : "bg-gray-50 text-gray-500 hover:bg-gray-100"
            }`}
          >
            전체
          </button>
          <button
            type="button"
            onClick={() => {
              setShowSettings(false)
              setTab("unread")
            }}
            className={`rounded-full px-3 py-1 text-sm font-medium transition ${
              !showSettings && tab === "unread"
                ? "bg-teal-50 text-teal-700"
                : "bg-gray-50 text-gray-500 hover:bg-gray-100"
            }`}
          >
            읽지 않음
          </button>
        </div>
        <div className="flex items-center gap-3 text-xs text-gray-400">
          <button
            type="button"
            className="hover:text-gray-600"
            onClick={() => setShowSettings((v) => !v)}
          >
            설정
          </button>
          <button type="button" className="hover:text-gray-600" onClick={() => void onMarkAllRead()}>
            모두 읽음
          </button>
          <button type="button" className="hover:text-gray-600" onClick={() => void onDeleteAll()}>
            모두 삭제
          </button>
        </div>
      </div>

      {showSettings ? (
        <div className="px-4 py-4">
          <p className="text-sm font-medium text-gray-800">알림 수신 설정</p>
          <p className="mt-1 text-xs text-gray-500">
            유튜브·뉴스·댓글·QnA. 관심종목은 회원가입 시 선택한 종목을 따릅니다.
          </p>
          <div className="mt-3 grid gap-2">
            {settings ? (
              (
                [
                  ["youtubeEnabled", "유튜브"],
                  ["newsEnabled", "뉴스"],
                  ["commentEnabled", "댓글"],
                  ["qnaEnabled", "QnA"],
                ] as const
              ).map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 text-sm text-gray-800">
                  <input
                    type="checkbox"
                    checked={settings[key]}
                    onChange={(e) =>
                      setSettings((prev) =>
                        prev ? { ...prev, [key]: e.target.checked } : prev
                      )
                    }
                  />
                  {label}
                </label>
              ))
            ) : (
              <p className="text-xs text-gray-400">설정을 불러오는 중…</p>
            )}
          </div>
          <button
            type="button"
            disabled={!settings || savingSettings}
            onClick={() => void onSaveSettings()}
            className="mt-4 rounded-full border border-teal-500 px-4 py-1.5 text-sm text-teal-600 hover:bg-teal-50 disabled:opacity-50"
          >
            {savingSettings ? "저장 중…" : "저장"}
          </button>
        </div>
      ) : (
        <div ref={scrollRef} className="max-h-[min(70vh,28rem)] overflow-y-auto">
          {loading ? (
            <p className="px-4 py-10 text-center text-sm text-gray-400">불러오는 중…</p>
          ) : error ? (
            <p className="px-4 py-10 text-center text-sm text-red-500">{error}</p>
          ) : items.length === 0 ? (
            <p className="px-4 py-10 text-center text-sm text-gray-400">알림이 없습니다.</p>
          ) : (
            <ul className="divide-y divide-gray-50">
              {items.map((item) => {
                const inner = (
                  <div
                    className={`flex gap-3 px-3 py-3.5 transition hover:bg-gray-50 ${
                      item.read ? "opacity-80" : "bg-teal-50/30"
                    }`}
                  >
                    <Avatar src={item.actorAvatarUrl} name={item.actorName} />
                    <div className="min-w-0 flex-1">
                      <p className="text-[13px] leading-5 text-gray-800">
                        {item.title}
                        <span className="ml-1 text-gray-400">
                          {formatInboxTime(item.createdAt)}
                        </span>
                      </p>
                      {item.body ? (
                        <p className="mt-1 line-clamp-2 text-xs text-gray-500">{item.body}</p>
                      ) : null}
                    </div>
                  </div>
                )
                return (
                  <li key={item.id}>
                    {item.linkUrl ? (
                      <Link href={item.linkUrl} onClick={() => void onItemClick(item)}>
                        {inner}
                      </Link>
                    ) : (
                      <button
                        type="button"
                        className="w-full text-left"
                        onClick={() => void onItemClick(item)}
                      >
                        {inner}
                      </button>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
          <div ref={sentinelRef} className="h-4" />
          {loadingMore ? (
            <p className="pb-3 text-center text-xs text-gray-400">더 불러오는 중…</p>
          ) : null}
        </div>
      )}
    </div>
  )
}
