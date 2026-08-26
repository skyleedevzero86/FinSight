"use client"

import Link from "next/link"
import { Suspense, useEffect, useState } from "react"
import { useParams, useRouter, useSearchParams } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { toPrivacyEmbedUrl, YOUTUBE_EMBED_ALLOW } from "@/lib/liveVod"
import {
  isLiveVodFavorite,
  toggleLiveVodFavorite,
} from "@/lib/liveVodFavorites"

function IconPrint({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width="20" height="20" aria-hidden>
      <path
        fill="currentColor"
        d="M6 9V3h12v6h2a2 2 0 0 1 2 2v6h-4v4H6v-4H2v-6a2 2 0 0 1 2-2zm2-4v4h8V5zm10 8H4v4h2v-2h12v2h2zm-4 6v-2H8v2z"
      />
    </svg>
  )
}

function IconStar({ filled, className }: { filled?: boolean; className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width="20" height="20" aria-hidden>
      <path
        fill={filled ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="1.8"
        d="M12 3.6 14.7 9l6 .5-4.6 3.9 1.4 5.8L12 16.6 6.5 19.2l1.4-5.8L3.3 9.5l6-.5z"
      />
    </svg>
  )
}

function IconShare({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width="20" height="20" aria-hidden>
      <path
        fill="currentColor"
        d="M18 16.1a3 3 0 0 0-2.1.85L9.9 13.5a3.1 3.1 0 0 0 0-3l6-3.45A3 3 0 1 0 15 5a3 3 0 0 0 .1.7L9.1 9.15a3 3 0 1 0 0 5.7l6.1 3.45A3 3 0 1 0 18 16.1z"
      />
    </svg>
  )
}

function LiveVodWatchBody() {
  const params = useParams<{ videoId: string }>()
  const searchParams = useSearchParams()
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const videoId = typeof params.videoId === "string" ? params.videoId : ""
  const title = searchParams.get("title")?.trim() || "VOD 상세"
  const channel = searchParams.get("channel")?.trim() || null
  const tab = (searchParams.get("tab") || "ALL").trim().toUpperCase() || "ALL"
  const backHref =
    tab === "FAVORITES"
      ? "/my/favorites"
      : tab === "ALL"
        ? "/live-vod"
        : `/live-vod?tab=${encodeURIComponent(tab)}`
  const embedSrc = toPrivacyEmbedUrl(videoId)
  const [favorited, setFavorited] = useState(false)
  const [shareHint, setShareHint] = useState<string | null>(null)

  useEffect(() => {
    setFavorited(isLiveVodFavorite(videoId))
    const sync = () => setFavorited(isLiveVodFavorite(videoId))
    window.addEventListener("finsight:live-vod-favorites-changed", sync)
    window.addEventListener("storage", sync)
    return () => {
      window.removeEventListener("finsight:live-vod-favorites-changed", sync)
      window.removeEventListener("storage", sync)
    }
  }, [videoId])

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

  const onPrint = () => {
    window.print()
  }

  const onToggleFavorite = () => {
    if (!ready) return
    if (!user) {
      const returnTo =
        typeof window !== "undefined"
          ? `${window.location.pathname}${window.location.search}`
          : "/my/favorites"
      router.push(`/login?next=${encodeURIComponent(returnTo)}`)
      return
    }
    const next = toggleLiveVodFavorite({
      videoId,
      title,
      channelTitle: channel,
      thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      tab: tab === "FAVORITES" ? "ALL" : tab,
    })
    setFavorited(next)
  }

  const onShare = async () => {
    const url = typeof window !== "undefined" ? window.location.href : ""
    try {
      if (typeof navigator !== "undefined" && typeof navigator.share === "function") {
        await navigator.share({ title, url, text: title })
        setShareHint(null)
        return
      }
      await navigator.clipboard.writeText(url)
      setShareHint("링크를 복사했습니다")
    } catch {
      setShareHint("공유에 실패했습니다")
    }
    window.setTimeout(() => setShareHint(null), 2000)
  }

  return (
    <div className="finsight-live-vod-page">
      <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
        <div className="flv-toolbar flv-watch-toolbar">
          <div className="flv-watch-top">
            <Link href={backHref} className="flv-back-link">
              이전으로
            </Link>
          </div>
          <div className="flv-watch-heading-row">
            <h1>{title}</h1>
            <div className="flv-watch-actions" role="toolbar" aria-label="영상 도구">
              <button type="button" className="flv-icon-btn" onClick={onPrint} aria-label="인쇄">
                <IconPrint />
              </button>
              <button
                type="button"
                className={`flv-icon-btn${favorited ? " is-on" : ""}`}
                onClick={onToggleFavorite}
                aria-label={favorited ? "즐겨찾기 해제" : "즐겨찾기"}
                aria-pressed={favorited}
              >
                <IconStar filled={favorited} />
              </button>
              <button type="button" className="flv-icon-btn" onClick={() => void onShare()} aria-label="공유">
                <IconShare />
              </button>
            </div>
          </div>
          {shareHint ? <p className="flv-share-hint">{shareHint}</p> : null}
        </div>

        <div className="flv-main-row">
          <div className="flv-video-col">
            <div className="flv-embed">
              <iframe
                title={title}
                src={embedSrc}
                referrerPolicy="strict-origin-when-cross-origin"
                allow={YOUTUBE_EMBED_ALLOW}
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
