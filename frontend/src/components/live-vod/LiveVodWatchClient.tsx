"use client"

import Link from "next/link"
import { Suspense } from "react"
import { useParams, useSearchParams } from "next/navigation"
import { toPrivacyEmbedUrl } from "@/lib/liveVod"

function LiveVodWatchBody() {
  const params = useParams<{ videoId: string }>()
  const searchParams = useSearchParams()
  const videoId = typeof params.videoId === "string" ? params.videoId : ""
  const title = searchParams.get("title")?.trim() || "VOD 상세"
  const channel = searchParams.get("channel")?.trim() || null
  const tab = (searchParams.get("tab") || "ALL").trim().toUpperCase() || "ALL"
  const backHref = tab === "ALL" ? "/live-vod" : `/live-vod?tab=${encodeURIComponent(tab)}`
  const embedSrc = toPrivacyEmbedUrl(videoId)

  if (!videoId || !embedSrc) {
    return (
      <div className="finsight-live-vod-page">
        <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
          <p className="text-sm text-red-600" role="alert">
            유효하지 않은 영상입니다.
          </p>
          <Link href="/live-vod" className="mt-4 inline-block text-sm text-gray-700 underline">
            LIVE/VOD 목록으로
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="finsight-live-vod-page">
      <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
        <div className="flv-toolbar flv-watch-toolbar">
          <Link href={backHref} className="flv-back-link">
            ← LIVE/VOD
          </Link>
          <h1>{title}</h1>
          {channel ? <p className="flv-watch-channel">{channel}</p> : null}
        </div>

        <div className="flv-main-row">
          <div className="flv-video-col">
            <div className="flv-embed">
              <iframe
                title={title}
                src={embedSrc}
                referrerPolicy="strict-origin-when-cross-origin"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
                allowFullScreen
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function LiveVodWatchClient() {
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
      <LiveVodWatchBody />
    </Suspense>
  )
}
