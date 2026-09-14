"use client"

import Image from "next/image"
import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Play } from "lucide-react"
import {
  fetchLiveVodFeed,
  flattenLiveVodFeedItems,
  liveVodPopularityScore,
  liveVodWatchHref,
  stashLiveVodMetaHint,
  type LiveVodItem,
} from "@/lib/liveVod"

type VodTab = "upcoming" | "popular" | "latest"

const TABS: { id: VodTab; label: string }[] = [
  { id: "upcoming", label: "추후에 예정..." },
  { id: "popular", label: "인기순" },
  { id: "latest", label: "최신순" },
]

const HOME_VOD_LIMIT = 6

function formatEngagement(item: LiveVodItem): string {
  const score = liveVodPopularityScore(item)
  if (score <= 0) return "참여 0"
  return `참여 ${score.toLocaleString("ko-KR")}`
}

export default function VODSection() {
  const [activeTab, setActiveTab] = useState<VodTab>("latest")
  const [items, setItems] = useState<LiveVodItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      setLoading(true)
      const result = await fetchLiveVodFeed("ALL")
      if (cancelled) return
      if (!result.ok) {
        setItems([])
        setError(result.message)
        setLoading(false)
        return
      }
      setItems(flattenLiveVodFeedItems(result.data))
      setError(null)
      setLoading(false)
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const visibleItems = useMemo(() => {
    if (activeTab === "upcoming") return []
    if (activeTab === "popular") {
      return [...items]
        .sort((a, b) => liveVodPopularityScore(b) - liveVodPopularityScore(a))
        .slice(0, HOME_VOD_LIMIT)
    }
    return items.slice(0, HOME_VOD_LIMIT)
  }, [activeTab, items])

  return (
    <section className="py-12 px-4 md:px-8 max-w-7xl mx-auto">
      <div className="mb-8 grid grid-cols-2 items-center gap-x-4 gap-y-4 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
        <h2 className="col-span-1 row-start-1 justify-self-start text-2xl font-bold md:col-start-1 md:row-start-1 md:text-3xl">
          실시간 VOD
        </h2>
        <Link
          href="/live-vod"
          className="col-span-1 row-start-1 justify-self-end text-sm font-medium text-[#3c3e40] hover:text-finsight-primary hover:underline md:col-start-3 md:row-start-1"
        >
          더보기 →
        </Link>
        <div className="col-span-2 row-start-2 flex justify-center gap-3 sm:gap-4 md:col-span-1 md:col-start-2 md:row-start-1 md:justify-self-center">
          {TABS.map((tab) => {
            const selected = activeTab === tab.id
            return (
              <button
                key={tab.id}
                type="button"
                title={
                  tab.id === "upcoming"
                    ? "추후에 예정되어 있습니다"
                    : tab.id === "popular"
                      ? "즐겨찾기·좋아요·댓글 합계가 많은 게시물"
                      : "유튜브 최신 영상"
                }
                onClick={() => setActiveTab(tab.id)}
                className={`inline-flex min-w-[4.25rem] justify-center px-3 py-2 text-center text-sm font-semibold transition sm:min-w-[4.5rem] sm:px-4 ${
                  selected
                    ? "text-finsight-primary border-b-2 border-finsight-primary"
                    : "text-gray-500 hover:text-gray-700"
                }`}
              >
                {tab.label}
              </button>
            )
          })}
        </div>
      </div>

      {loading ? (
        <p className="py-10 text-center text-sm text-gray-500">영상을 불러오는 중…</p>
      ) : error ? (
        <p className="py-10 text-center text-sm text-gray-500">{error}</p>
      ) : activeTab === "upcoming" ? (
        <p className="py-10 text-center text-sm text-gray-500">추후에 예정되어 있습니다.</p>
      ) : visibleItems.length === 0 ? (
        <p className="py-10 text-center text-sm text-gray-500">표시할 영상이 없습니다.</p>
      ) : (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {visibleItems.map((vod) => {
            const href = liveVodWatchHref(vod, "ALL")
            return (
              <Link
                key={vod.videoId}
                href={href}
                className="group cursor-pointer"
                onClick={() => stashLiveVodMetaHint(vod)}
              >
                <div className="relative aspect-video overflow-hidden rounded-lg mb-3">
                  <Image
                    src={vod.thumbnailUrl}
                    alt={vod.title}
                    fill
                    sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
                    className="object-cover group-hover:scale-105 transition duration-300"
                  />
                  <div className="absolute inset-0 bg-black/20 group-hover:bg-black/40 transition flex items-center justify-center">
                    <div className="bg-white/90 rounded-full p-3 opacity-80 group-hover:opacity-100 transition">
                      <Play className="w-8 h-8 text-finsight-primary fill-finsight-primary" />
                    </div>
                  </div>
                </div>
                <div>
                  <h3 className="font-semibold text-sm line-clamp-2 group-hover:text-finsight-primary transition mb-1">
                    {vod.title}
                  </h3>
                  <div className="flex items-center gap-2 text-xs text-gray-500">
                    <span className="font-medium">{vod.channelTitle || "YouTube"}</span>
                    <span>•</span>
                    <span>
                      {activeTab === "popular"
                        ? formatEngagement(vod)
                        : "최신 영상"}
                    </span>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </section>
  )
}
