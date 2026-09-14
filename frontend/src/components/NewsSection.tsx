"use client"

import Image from "next/image"
import Link from "next/link"
import { useEffect, useState } from "react"
import { Clock, Maximize2 } from "lucide-react"
import {
  fetchLiveVodFeed,
  flattenLiveVodFeedItems,
  liveVodWatchHref,
  shuffleLiveVodItems,
  stashLiveVodMetaHint,
  type LiveVodItem,
} from "@/lib/liveVod"

const mainNews = [
  {
    id: 1,
    title: "함수본 '전재수 785만원 까르띠에 수수 의심, 시효…",
    image: "https://images.unsplash.com/photo-1495020689067-958852a7765e?w=800&h=450&fit=crop",
    timeAgo: "2시간 전",
    duration: "02:14",
    category: "정치",
  },
  {
    id: 2,
    title: "유석열은 '딥 그림자'라더니…김건희 스스로 '달빛'",
    image: "https://images.unsplash.com/photo-1529078155058-5d716f45d604?w=800&h=450&fit=crop",
    timeAgo: "2시간 전",
    duration: "01:33",
    category: "정치",
  },
  {
    id: 3,
    title: "특검권 포기? 성역인 권 형님들…'제로동의안'",
    image: "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=600&h=338&fit=crop",
    timeAgo: "1시간 전",
    duration: "02:33",
    category: "정치",
  },
  {
    id: 4,
    title: "[단독] '외유성 출장' 추사 설인데…또 '누구 부부'",
    image: "https://images.unsplash.com/photo-1593113598332-cd288d649433?w=600&h=338&fit=crop",
    timeAgo: "1시간 전",
    duration: "02:46",
    category: "사회",
  },
  {
    id: 5,
    title: "이란 전역 대통령 부부 묻었다…윤석열 이어 김건희 조문",
    image: "https://images.unsplash.com/photo-1611162616475-46b635cb6868?w=600&h=338&fit=crop",
    timeAgo: "3시간 전",
    duration: "02:05",
    category: "정치",
  },
  {
    id: 6,
    title: "방송3법 개정안 처리 임박…여야 대치 격화",
    image: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600&h=338&fit=crop",
    timeAgo: "4시간 전",
    duration: "01:52",
    category: "사회",
  },
]

const replayPrograms = [
  {
    id: "p1",
    circleClass: "bg-[#e85d04]",
    circleLines: ["장르만", "여의도"],
    label: "시장브리핑",
    href: "/live-vod?tab=MARKET",
  },
  {
    id: "p2",
    circleClass: "bg-[#c026d3]",
    circleLines: ["백브", "RE핑"],
    label: "테마분석",
    href: "/live-vod?tab=THEME",
  },
  {
    id: "p3",
    circleClass: "bg-[#16a34a]",
    circleLines: ["부글", "터뷰"],
    label: "종목분석",
    href: "/live-vod?tab=THEME",
  },
  {
    id: "p4",
    circleClass: "bg-[#7c3aed]",
    circleLines: ["유기자의", "알탭"],
    label: "실적/기업이슈",
    href: "/live-vod?tab=THEME",
  },
  {
    id: "p5",
    circleClass: "bg-[#2563eb]",
    circleLines: ["투자", "상식"],
    label: "투자상식",
    href: "/live-vod",
  },
  {
    id: "p6",
    circleClass: "bg-[#0d9488]",
    circleLines: ["글로벌", "매크로"],
    label: "글로벌매크로",
    href: "/live-vod?tab=MACRO",
  },
] as const

const featuredPair = mainNews.slice(0, 2)
const smallQuad = mainNews.slice(2, 6)

export default function NewsSection() {
  const [replayItems, setReplayItems] = useState<LiveVodItem[]>([])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const result = await fetchLiveVodFeed("ALL")
      if (cancelled || !result.ok) return
      const picked = shuffleLiveVodItems(flattenLiveVodFeedItems(result.data)).slice(0, 2)
      setReplayItems(picked)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section className="border-t border-[#ebebeb] bg-[#f9f9f9] py-10 md:py-12">
      <div className="mx-auto max-w-7xl px-4 md:px-8">
        <div className="relative mb-8 md:mb-10">
          <h2 className="text-center text-2xl font-bold tracking-tight text-[#231f20] md:text-[26px]">
            최신뉴스
          </h2>
          <div className="mt-3 text-center md:mt-0">
            <Link
              href="/live-vod"
              className="text-sm font-medium text-[#3c3e40] hover:text-finsight-primary hover:underline md:absolute md:right-0 md:top-1/2 md:mt-0 md:inline md:-translate-y-1/2"
            >
              더보기 →
            </Link>
          </div>
        </div>

        <div className="flex flex-col gap-8 lg:flex-row lg:items-start lg:gap-10">
          <div className="min-w-0 flex-1 lg:max-w-[calc(100%-20rem)]">
            <div className="mb-5 grid grid-cols-1 gap-5 md:grid-cols-2 md:gap-4">
              {featuredPair.map((news) => (
                <Link
                  key={news.id}
                  href="#"
                  className="group block overflow-hidden border border-[#ebebeb] bg-white shadow-[0_1px_0_rgba(0,0,0,0.04)] transition hover:shadow-md"
                >
                  <div className="relative aspect-[16/9] w-full overflow-hidden bg-[#f0f0f0]">
                    <Image
                      src={news.image}
                      alt={news.title}
                      fill
                      className="object-cover transition duration-300 group-hover:scale-[1.02]"
                      sizes="(max-width: 768px) 100vw, 50vw"
                    />
                    {news.duration ? (
                      <div className="absolute bottom-2 right-2 flex items-center gap-1 rounded bg-black/75 px-2 py-0.5 text-xs font-medium text-white">
                        <Clock className="h-3 w-3" aria-hidden />
                        {news.duration}
                      </div>
                    ) : null}
                  </div>
                  <div className="p-4 md:p-5">
                    <h3 className="text-[15px] font-bold leading-snug tracking-tight text-[#231f20] line-clamp-2 md:text-base group-hover:text-finsight-primary">
                      {news.title}
                    </h3>
                    <p className="mt-3 text-xs text-[#737475]">{news.timeAgo}</p>
                  </div>
                </Link>
              ))}
            </div>

            <div className="grid grid-cols-2 gap-3 md:gap-4 lg:grid-cols-4">
              {smallQuad.map((news) => (
                <Link key={news.id} href="#" className="group block">
                  <div className="relative mb-2 aspect-video overflow-hidden border border-[#ebebeb] bg-[#f5f5f5]">
                    <Image
                      src={news.image}
                      alt={news.title}
                      fill
                      className="object-cover transition duration-300 group-hover:scale-105"
                      sizes="(max-width: 1024px) 50vw, 25vw"
                    />
                    <div className="absolute bottom-1.5 right-1.5 flex items-center gap-0.5 rounded bg-black/75 px-1.5 py-0.5 text-[10px] font-medium text-white md:text-xs">
                      <Clock className="h-2.5 w-2.5 md:h-3 md:w-3" aria-hidden />
                      {news.duration}
                    </div>
                  </div>
                  <h3 className="line-clamp-2 text-xs font-bold leading-snug text-[#231f20] md:text-[13px] group-hover:text-finsight-primary">
                    {news.title}
                  </h3>
                  <p className="mt-1 text-[10px] text-[#737475] md:text-xs">{news.timeAgo}</p>
                </Link>
              ))}
            </div>
          </div>

          <aside className="w-full shrink-0 border border-[#d6d6d6] bg-white p-4 shadow-sm lg:w-[280px] xl:w-[300px]">
            <div className="mb-4 text-right">
              <Link
                href="/live-vod"
                className="inline-block text-base font-bold tracking-tight text-[#231f20] hover:text-finsight-primary"
              >
                다시보기 &gt;
              </Link>
            </div>

            <div className="mb-5 grid grid-cols-3 gap-x-2 gap-y-4 border-b border-[#ebebeb] pb-5">
              {replayPrograms.map((p) => (
                <Link
                  key={p.id}
                  href={p.href}
                  className="flex min-w-0 flex-col items-center gap-2 text-center"
                >
                  <span
                    className={`flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-full px-1 text-center text-[9px] font-bold leading-tight text-white shadow-sm md:h-16 md:w-16 md:text-[10px] ${p.circleClass}`}
                  >
                    {p.circleLines.map((line) => (
                      <span key={line} className="block max-w-[3.25rem] truncate">
                        {line}
                      </span>
                    ))}
                  </span>
                  <span className="w-full truncate text-[11px] text-[#3c3e40] md:text-xs">{p.label}</span>
                </Link>
              ))}
            </div>

            <div className="border border-[#ebebeb] bg-white">
              {replayItems.length === 0 ? (
                <p className="p-4 text-center text-xs text-[#737475]">유튜브 영상을 불러오는 중…</p>
              ) : (
                replayItems.map((item, idx) => {
                  const href = liveVodWatchHref(item, "ALL")
                  return (
                    <div
                      key={item.videoId}
                      className={`flex gap-3 p-3 md:gap-4 md:p-4 ${idx > 0 ? "border-t border-[#ebebeb]" : ""}`}
                    >
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-bold leading-snug text-[#231f20] line-clamp-2 md:text-[15px]">
                          {item.title}
                        </p>
                        <p className="mt-1 text-xs text-[#737475]">
                          {item.channelTitle || "YouTube"}
                        </p>
                      </div>
                      <Link
                        href={href}
                        aria-label={`${item.title} 영상 보기`}
                        className="group/thumb relative h-[4.5rem] w-[7.5rem] shrink-0 overflow-hidden rounded-md bg-[#eee] md:h-[4.75rem] md:w-[8rem]"
                        onClick={() => stashLiveVodMetaHint(item)}
                      >
                        <Image
                          src={item.thumbnailUrl}
                          alt=""
                          fill
                          className="object-cover transition group-hover/thumb:opacity-95"
                          sizes="128px"
                        />
                        <span className="absolute right-1 top-1 rounded bg-black/45 p-0.5 text-white">
                          <Maximize2 className="h-3 w-3" aria-hidden />
                        </span>
                      </Link>
                    </div>
                  )
                })
              )}
            </div>
          </aside>
        </div>
      </div>
    </section>
  )
}
