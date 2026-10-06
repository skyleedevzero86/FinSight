"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { Bookmark, Flag, Star } from "lucide-react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { MyInfoAssetStatus, MyInfoAssetTrend } from "@/components/myinfo/MyInfoAssetPanels"
import {
  formatKoreanDate,
  loadMyInfoHome,
  myInfoMarkets,
  type MyInfoActivity,
  type MyInfoHomeData,
  type MyInfoMarket,
  type MyInfoNotice,
  type MyInfoThumb,
} from "@/lib/myInfoHome"

const EMPTY: MyInfoHomeData = {
  notices: [],
  continueItems: [],
  interestItems: [],
  activities: [],
  recommends: [],
}

function Panel({
  title,
  href,
  action = "전체보기",
  children,
}: {
  title: string
  href: string
  action?: string
  children: React.ReactNode
}) {
  return (
    <section className="flex h-full flex-col rounded-2xl border border-[#e7edf5] bg-white px-4 py-4 shadow-[0_8px_24px_rgba(15,23,42,0.04)]">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="text-[15px] font-bold text-slate-800">{title}</h2>
        <Link href={href} className="shrink-0 text-xs text-slate-400 hover:text-finsight-primary">
          {action} <span aria-hidden>›</span>
        </Link>
      </div>
      {children}
    </section>
  )
}

function EmptyCopy({ children }: { children: string }) {
  return <p className="py-8 text-center text-sm text-slate-400">{children}</p>
}

function SmartLink({
  href,
  external,
  className,
  children,
}: {
  href: string
  external?: boolean
  className?: string
  children: React.ReactNode
}) {
  if (external) {
    return (
      <a href={href} target="_blank" rel="noreferrer" className={className}>
        {children}
      </a>
    )
  }
  return (
    <Link href={href} className={className}>
      {children}
    </Link>
  )
}

function Thumb({ src, alt }: { src: string | null; alt: string }) {
  const [broken, setBroken] = useState(false)
  if (!src || broken) {
    return <div className="h-full w-full bg-gradient-to-br from-slate-200 via-slate-300 to-sky-200" />
  }
  return (
    <img
      src={src}
      alt={alt}
      className="h-full w-full object-cover"
      onError={() => setBroken(true)}
    />
  )
}

function NoticeList({ items }: { items: MyInfoNotice[] }) {
  if (!items.length) return <EmptyCopy>등록된 공지가 없습니다.</EmptyCopy>
  return (
    <ul className="divide-y divide-slate-100">
      {items.map((item) => (
        <li key={item.id}>
          <Link href={item.href} className="flex items-start gap-2 py-2.5 hover:text-finsight-primary">
            <Flag className="mt-0.5 h-3.5 w-3.5 shrink-0 text-rose-500" fill="currentColor" aria-hidden />
            <span className="min-w-0 flex-1 truncate text-[13px] text-slate-700">{item.title}</span>
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function Sparkline({ up }: { up: boolean }) {
  const points = up
    ? "0,24 10,22 18,23 28,16 36,18 46,11 54,13 64,7 72,5"
    : "0,8 10,10 18,9 28,15 36,13 46,20 54,18 64,24 72,26"
  return (
    <svg viewBox="0 0 72 32" className="h-8 w-[4.5rem]" aria-hidden>
      <polyline fill="none" stroke={up ? "#e11d48" : "#2563eb"} strokeWidth="1.7" points={points} />
    </svg>
  )
}

function MarketCard({ item }: { item: MyInfoMarket }) {
  const tone = item.up ? "text-rose-600" : "text-blue-600"
  return (
    <article className="flex items-center justify-between gap-2 rounded-xl border border-[#e7edf5] bg-white px-3 py-3 shadow-[0_8px_24px_rgba(15,23,42,0.04)]">
      <div className="min-w-0">
        <p className="text-[11px] text-slate-400">{item.label}</p>
        <p className="mt-1 text-lg font-bold tracking-tight text-slate-900">{item.price}</p>
        <p className={`mt-1 text-xs font-semibold ${tone}`}>
          {item.up ? "▲" : "▼"} {item.change}
        </p>
      </div>
      <Sparkline up={item.up} />
    </article>
  )
}

function ContinueGrid({ items }: { items: MyInfoThumb[] }) {
  if (!items.length) return <EmptyCopy>이어볼 영상이 없습니다.</EmptyCopy>
  return (
    <ul className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      {items.map((item) => (
        <li key={item.key}>
          <SmartLink href={item.href} external={item.external} className="group block">
            <span className="block aspect-[16/10] overflow-hidden rounded-lg bg-slate-100">
              <Thumb src={item.image} alt="" />
            </span>
            <span className="mt-2 line-clamp-2 text-[13px] font-semibold leading-snug text-slate-800 group-hover:text-finsight-primary">
              {item.title}
            </span>
            <span className="mt-1 block truncate text-[11px] text-slate-400">{item.meta}</span>
          </SmartLink>
        </li>
      ))}
    </ul>
  )
}

function StackedThumbs({ items, empty }: { items: MyInfoThumb[]; empty: string }) {
  if (!items.length) return <EmptyCopy>{empty}</EmptyCopy>
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item) => (
        <li key={item.key} className="flex items-center gap-3">
          <SmartLink href={item.href} external={item.external} className="flex min-w-0 flex-1 items-center gap-3">
            <span className="h-14 w-[4.5rem] shrink-0 overflow-hidden rounded-lg bg-slate-100">
              <Thumb src={item.image} alt="" />
            </span>
            <span className="min-w-0">
              <span className="line-clamp-2 text-[13px] font-semibold leading-snug text-slate-800">{item.title}</span>
              <span className="mt-1 block truncate text-[11px] text-slate-400">{item.meta}</span>
            </span>
          </SmartLink>
          <Link href="/myinfo/favorites" className="shrink-0 text-slate-300 hover:text-finsight-primary" aria-label="나의 즐겨찾기">
            <Bookmark className="h-4 w-4" aria-hidden />
          </Link>
        </li>
      ))}
    </ul>
  )
}

function ActivityList({ items }: { items: MyInfoActivity[] }) {
  if (!items.length) return <EmptyCopy>최근 활동이 없습니다.</EmptyCopy>
  const tones = ["text-amber-400", "text-sky-500", "text-amber-400"]
  return (
    <ul className="flex flex-col gap-3">
      {items.map((item, index) => (
        <li key={item.key}>
          <Link href={item.href} className="flex items-start gap-2 hover:text-finsight-primary">
            <Star className={`mt-0.5 h-4 w-4 shrink-0 ${tones[index % tones.length]}`} fill="currentColor" aria-hidden />
            <span className="min-w-0 flex-1 text-[13px] leading-snug text-slate-700">{item.title}</span>
            <span className="shrink-0 text-[11px] text-slate-400">{item.timeLabel}</span>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function GreetingCard() {
  const { user } = useAuthSession()
  const name = user?.nickname || "회원"
  return (
    <div className="flex items-start justify-between gap-3 rounded-2xl border border-[#e7edf5] bg-white px-4 py-4 shadow-[0_8px_24px_rgba(15,23,42,0.04)]">
      <div className="min-w-0">
        <p className="truncate text-base font-bold text-slate-900">안녕하세요, {name}님</p>
        <p className="mt-1 text-[13px] text-slate-500">오늘도 FinSight와 함께 좋은 인사이트를 만나보세요.</p>
      </div>
      <div className="shrink-0 text-right">
        <p className="text-[11px] text-slate-400">{formatKoreanDate(new Date())}</p>
        <Link href="/myinfo/userinfo" className="mt-2 inline-block text-xs text-slate-400 hover:text-finsight-primary">
          더보기 <span aria-hidden>›</span>
        </Link>
      </div>
    </div>
  )
}

export default function MyInfoHomeClient() {
  const [data, setData] = useState<MyInfoHomeData>(EMPTY)
  const [loading, setLoading] = useState(true)
  const markets = myInfoMarkets()

  useEffect(() => {
    let alive = true
    void loadMyInfoHome()
      .then((next) => {
        if (!alive) return
        setData(next)
      })
      .catch((error: unknown) => {
        console.error("마이페이지를 불러오지 못했습니다.", error)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:px-5 md:py-5">
      <div className="grid items-stretch gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.15fr)]">
        <Panel title="공지사항 / 운영 알림" href="/community/notice">
          {loading ? <EmptyCopy>공지를 불러오는 중입니다.</EmptyCopy> : <NoticeList items={data.notices} />}
        </Panel>
        <div className="flex flex-col gap-3">
          <GreetingCard />
          <div className="grid gap-3 sm:grid-cols-3">
            {markets.map((item) => (
              <MarketCard key={item.id} item={item} />
            ))}
          </div>
        </div>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <Panel title="이어보기" href="/myinfo/history">
          {loading ? <EmptyCopy>시청 기록을 불러오는 중입니다.</EmptyCopy> : <ContinueGrid items={data.continueItems} />}
        </Panel>
        <Panel title="내가 관심 있는 콘텐츠" href="/myinfo/favorites">
          {loading ? (
            <EmptyCopy>관심 콘텐츠를 불러오는 중입니다.</EmptyCopy>
          ) : (
            <StackedThumbs items={data.interestItems} empty="관심 콘텐츠가 없습니다." />
          )}
        </Panel>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <MyInfoAssetStatus />
        <MyInfoAssetTrend />
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <Panel title="최근 나의 활동" href="/myinfo/posts">
          {loading ? <EmptyCopy>활동을 불러오는 중입니다.</EmptyCopy> : <ActivityList items={data.activities} />}
        </Panel>
        <Panel title="맞춤 추천 뉴스 · 콘텐츠" href="/news">
          {loading ? (
            <EmptyCopy>추천 콘텐츠를 불러오는 중입니다.</EmptyCopy>
          ) : (
            <StackedThumbs items={data.recommends} empty="추천 콘텐츠가 없습니다." />
          )}
        </Panel>
      </div>
    </div>
  )
}
