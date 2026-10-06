"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import {
  Bell,
  Bookmark,
  Eye,
  MessageCircle,
  MessageSquare,
  Play,
  Star,
  ThumbsUp,
  Users,
} from "lucide-react"
import { fetchInboxUnreadCount } from "@/lib/inbox"
import {
  loadMyActivity,
  type ActivityAlertItem,
  type ActivityPostItem,
  type ActivityWatchItem,
  type MyActivityData,
} from "@/lib/myActivity"

type ActivityTab = "all" | "history" | "favorites" | "posts" | "alerts"

const TABS: { key: ActivityTab; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "history", label: "시청기록" },
  { key: "favorites", label: "즐겨찾기" },
  { key: "posts", label: "나의게시글" },
  { key: "alerts", label: "댓글/알림" },
]

const POST_TONES = ["bg-sky-500", "bg-rose-500", "bg-violet-500", "bg-orange-500", "bg-emerald-500"]

const EMPTY: MyActivityData = {
  watches: [],
  favorites: [],
  posts: [],
  alerts: [],
}

function Thumb({ src }: { src: string | null }) {
  const [broken, setBroken] = useState(false)
  if (!src || broken) {
    return <div className="h-full w-full bg-gradient-to-br from-slate-200 to-sky-200" />
  }
  return (
    <img src={src} alt="" className="h-full w-full object-cover" onError={() => setBroken(true)} />
  )
}

function SectionHead({ title, href }: { title: string; href: string }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-3">
      <h2 className="text-[15px] font-bold text-slate-800">{title}</h2>
      <Link href={href} className="shrink-0 text-xs text-slate-400 hover:text-finsight-primary">
        전체보기 <span aria-hidden>›</span>
      </Link>
    </div>
  )
}

function Panel({ children }: { children: React.ReactNode }) {
  return <section className="border border-[#e7edf5] bg-white px-4 py-4">{children}</section>
}

function EmptyCopy({ children }: { children: string }) {
  return <p className="py-8 text-center text-sm text-slate-400">{children}</p>
}

function WatchList({ items, empty }: { items: ActivityWatchItem[]; empty: string }) {
  if (!items.length) return <EmptyCopy>{empty}</EmptyCopy>
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item) => (
        <li key={item.key}>
          <Link href={item.href} className="flex items-center gap-3">
            <span className="h-14 w-[5.5rem] shrink-0 overflow-hidden bg-slate-100">
              <Thumb src={item.image} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="line-clamp-1 text-[13px] font-semibold text-slate-800">{item.title}</span>
              <span className="mt-1 block truncate text-[11px] text-slate-400">{item.source}</span>
            </span>
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function FavoriteList({ items }: { items: ActivityWatchItem[] }) {
  if (!items.length) return <EmptyCopy>즐겨찾기한 콘텐츠가 없습니다.</EmptyCopy>
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item) => (
        <li key={item.key} className="flex items-center gap-3">
          <Link href={item.href} className="flex min-w-0 flex-1 items-center gap-3">
            <span className="h-14 w-[5.5rem] shrink-0 overflow-hidden bg-slate-100">
              <Thumb src={item.image} />
            </span>
            <span className="min-w-0">
              <span className="line-clamp-1 text-[13px] font-semibold text-slate-800">{item.title}</span>
              <span className="mt-1 block truncate text-[11px] text-slate-400">
                {item.source}
                {item.timeLabel ? ` · ${item.timeLabel}` : ""}
              </span>
            </span>
          </Link>
          <Bookmark className="h-4 w-4 shrink-0 text-sky-500" fill="currentColor" aria-hidden />
        </li>
      ))}
    </ul>
  )
}

function PostList({ items }: { items: ActivityPostItem[] }) {
  if (!items.length) return <EmptyCopy>작성한 게시글이 없습니다.</EmptyCopy>
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item, index) => (
        <li key={item.key}>
          <Link href={item.href} className="flex items-start gap-3">
            <span
              className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center text-white ${POST_TONES[index % POST_TONES.length]}`}
            >
              <MessageSquare className="h-4 w-4" aria-hidden />
            </span>
            <span className="min-w-0 flex-1">
              <span className="line-clamp-1 text-[13px] font-semibold text-slate-800">{item.title}</span>
              <span className="mt-1 flex flex-wrap items-center gap-2 text-[11px] text-slate-400">
                <span className="bg-slate-100 px-1.5 py-0.5 text-slate-500">{item.boardLabel}</span>
                <span className="inline-flex items-center gap-1">
                  <Eye className="h-3 w-3" aria-hidden />
                  {item.views}
                </span>
                <span className="inline-flex items-center gap-1">
                  <MessageCircle className="h-3 w-3" aria-hidden />
                  {item.comments}
                </span>
                <span className="inline-flex items-center gap-1">
                  <ThumbsUp className="h-3 w-3" aria-hidden />
                  {item.likes}
                </span>
              </span>
            </span>
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function AlertIcon({ category }: { category: ActivityAlertItem["category"] }) {
  if (category === "YOUTUBE") return <Play className="h-4 w-4" aria-hidden />
  if (category === "NEWS" || category === "WATCHLIST") return <Star className="h-4 w-4" aria-hidden />
  if (category === "ADMIN") return <Bell className="h-4 w-4" aria-hidden />
  return <MessageCircle className="h-4 w-4" aria-hidden />
}

function AlertList({ items }: { items: ActivityAlertItem[] }) {
  if (!items.length) return <EmptyCopy>받은 댓글과 알림이 없습니다.</EmptyCopy>
  const tones = ["bg-sky-100 text-sky-600", "bg-amber-100 text-amber-500", "bg-violet-100 text-violet-600", "bg-rose-100 text-rose-500"]
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item, index) => (
        <li key={item.key}>
          <Link href={item.href} className="flex items-start gap-3">
            <span
              className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center ${
                item.unread ? "bg-red-100 text-red-600" : tones[index % tones.length]
              }`}
            >
              <AlertIcon category={item.category} />
            </span>
            <span className="min-w-0 flex-1">
              <span
                className={`line-clamp-2 text-[13px] font-semibold leading-snug ${
                  item.unread ? "text-red-600" : "text-slate-800"
                }`}
              >
                {item.title}
              </span>
              <span className="mt-1 block truncate text-[11px] text-slate-400">{item.detail}</span>
            </span>
            {item.unread ? (
              <span className="mt-1.5 h-2 w-2 shrink-0 bg-red-600" aria-label="읽지 않음" />
            ) : null}
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

export default function MyActivityClient() {
  const [tab, setTab] = useState<ActivityTab>("all")
  const [data, setData] = useState<MyActivityData>(EMPTY)
  const [loading, setLoading] = useState(true)
  const [serverUnread, setServerUnread] = useState<number | null>(null)

  useEffect(() => {
    let alive = true
    void loadMyActivity()
      .then((next) => {
        if (alive) setData(next)
      })
      .catch((error: unknown) => {
        console.error("내 활동을 불러오지 못했습니다.", error)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    void fetchInboxUnreadCount().then((count) => {
      if (alive && typeof count === "number") setServerUnread(count)
    })
    return () => {
      alive = false
    }
  }, [])

  const showHistory = tab === "all" || tab === "history"
  const showFavorites = tab === "all" || tab === "favorites"
  const showPosts = tab === "all" || tab === "posts"
  const showAlerts = tab === "all" || tab === "alerts"
  const listedUnread = data.alerts.filter((item) => item.unread).length
  const unreadAlerts = serverUnread ?? listedUnread

  return (
    <div className="px-4 py-5 md:px-6">
      <header className="mb-4 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center bg-sky-500 text-white">
          <Users className="h-5 w-5" aria-hidden />
        </span>
        <div>
          <h1 className="text-xl font-bold text-slate-900">내 활동</h1>
          <p className="mt-0.5 text-sm text-slate-500">지금까지의 활동 내역을 한눈에 확인하세요.</p>
        </div>
      </header>

      <div className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-5">
        {TABS.map((item) => {
          const active = tab === item.key
          return (
            <button
              key={item.key}
              type="button"
              className={
                active
                  ? "bg-[#2f6bff] px-3 py-2.5 text-sm font-semibold text-white"
                  : "border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-600 hover:bg-slate-50"
              }
              aria-pressed={active}
              onClick={() => setTab(item.key)}
            >
              <span className="inline-flex items-center justify-center gap-1.5">
                {item.label}
                {item.key === "alerts" && unreadAlerts > 0 ? (
                  <span className="inline-flex h-4 min-w-4 items-center justify-center bg-red-600 px-1 text-[10px] font-bold leading-none text-white">
                    {unreadAlerts > 99 ? "99+" : unreadAlerts}
                  </span>
                ) : null}
              </span>
            </button>
          )
        })}
      </div>

      {loading ? (
        <p className="py-16 text-center text-sm text-slate-400">활동을 불러오는 중입니다.</p>
      ) : (
        <div className={tab === "all" ? "grid gap-4 xl:grid-cols-2" : "grid gap-4"}>
          {showHistory ? (
            <Panel>
              <SectionHead title="최근 시청한 콘텐츠" href="/myinfo/history" />
              <WatchList items={data.watches} empty="최근 시청한 콘텐츠가 없습니다." />
            </Panel>
          ) : null}
          {showFavorites ? (
            <Panel>
              <SectionHead title="즐겨찾기한 콘텐츠" href="/myinfo/favorites" />
              <FavoriteList items={data.favorites} />
            </Panel>
          ) : null}
          {showPosts ? (
            <Panel>
              <SectionHead title="내가 작성한 게시글" href="/myinfo/posts" />
              <PostList items={data.posts} />
            </Panel>
          ) : null}
          {showAlerts ? (
            <Panel>
              <SectionHead title="받은 댓글 및 알림" href="/myinfo/posts" />
              <AlertList items={data.alerts} />
            </Panel>
          ) : null}
        </div>
      )}
    </div>
  )
}
