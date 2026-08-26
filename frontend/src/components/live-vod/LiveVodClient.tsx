"use client"

import Link from "next/link"
import { Suspense, useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import {
  fetchLiveVodFeed,
  liveVodWatchHref,
  toPrivacyEmbedUrl,
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
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
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
          <section key={`section-${sectionIndex}-${sec.heading}`} className="flv-relate">
            <h3>{sec.heading}</h3>
            <ul>
              {sec.items.map((it, itemIndex) => (
                <li key={`${it.videoId}-${itemIndex}`}>
                  <Link href={liveVodWatchHref(it, tab)}>
                    <img src={it.thumbnailUrl} alt="" />
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
