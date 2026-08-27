"use client"

import Link from "next/link"
import { Suspense, useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import {
  fetchLiveVodFeed,
  liveVodWatchHref,
  toPrivacyEmbedUrl,
  YOUTUBE_EMBED_ALLOW,
  type LiveVodFeed,
} from "@/lib/liveVod"

function LiveVodBody({ tab }: { tab: string }) {
  const [feed, setFeed] = useState<LiveVodFeed | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    void fetchLiveVodFeed(tab).then((result) => {
      if (cancelled) return
      setLoading(false)
      if (!result.ok) {
        setFeed(null)
        setError(result.message)
        return
      }
      setFeed(result.data)
    })
    return () => {
      cancelled = true
    }
  }, [tab])

  if (loading) {
    return (
      <div className="finsight-live-vod-page">
        <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
          <p className="text-sm text-gray-500">영상을 불러오는 중…</p>
        </div>
      </div>
    )
  }

  if (error || !feed?.featuredVideoId) {
    return (
      <div className="finsight-live-vod-page">
        <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
          <p className="text-sm text-red-600" role="alert">
            {error || "표시할 YouTube 영상이 없습니다. API 키·할당량을 확인해 주세요."}
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="finsight-live-vod-page">
      <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
        <div className="flv-toolbar">
          <h1>{feed.title}</h1>
        </div>

        <div className="flv-main-row">
          <div className="flv-video-col">
            <div className="flv-embed">
              <iframe
                title={feed.featuredTitle || "finsight LIVE"}
                src={toPrivacyEmbedUrl(feed.featuredVideoId, feed.embedUrl)}
                referrerPolicy="strict-origin-when-cross-origin"
                allow={YOUTUBE_EMBED_ALLOW}
                allowFullScreen
              />
            </div>
            {feed.featuredTitle ? (
              <p className="mt-3 text-sm font-medium text-gray-900 md:text-base">
                {feed.featuredTitle}
              </p>
            ) : null}
          </div>
        </div>

        {feed.sections.map((sec, sectionIndex) => (
          <section key={`section-${sectionIndex}-${sec.heading || "list"}`} className="flv-relate">
            {sec.heading ? <h3>{sec.heading}</h3> : null}
            <ul>
              {sec.items.map((it, itemIndex) => (
                <li key={`${it.videoId}-${itemIndex}`}>
                  <Link href={liveVodWatchHref(it, tab)} className="flv-thumb-link">
                    <div className="flv-thumb-wrap">
                      <img src={it.thumbnailUrl} alt="" />
                      <div className="flv-thumb-meta" aria-label="참여 수">
                        <span title="즐겨찾기">♡ {it.favoriteCount}</span>
                        <span title="댓글">💬 {it.commentCount}</span>
                      </div>
                    </div>
                    <div className="flv-vod-title">{it.title}</div>
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}

function LiveVodWithTab() {
  const searchParams = useSearchParams()
  const tab = (searchParams.get("tab") || "ALL").trim().toUpperCase() || "ALL"
  if (tab === "FAVORITES") {
    return (
      <div className="finsight-live-vod-page">
        <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
          <p className="text-sm text-gray-600">
            즐겨찾기는{" "}
            <Link href="/my/favorites" className="underline">
              나의 즐겨찾기
            </Link>
            에서 확인할 수 있습니다.
          </p>
        </div>
      </div>
    )
  }
  if (tab === "HISTORY") {
    return (
      <div className="finsight-live-vod-page">
        <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
          <p className="text-sm text-gray-600">
            시청 기록은{" "}
            <Link href="/my/history" className="underline">
              나의 히스토리
            </Link>
            에서 확인할 수 있습니다.
          </p>
        </div>
      </div>
    )
  }
  return <LiveVodBody tab={tab} />
}

export default function LiveVodClient() {
  return (
    <Suspense
      fallback={
        <div className="finsight-live-vod-page">
          <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
            <p className="text-sm text-gray-500">영상을 불러오는 중…</p>
          </div>
        </div>
      }
    >
      <LiveVodWithTab />
    </Suspense>
  )
}
