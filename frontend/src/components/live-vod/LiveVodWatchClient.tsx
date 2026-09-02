"use client"

import Link from "next/link"
import { Suspense, useEffect, useState } from "react"
import { useParams, useRouter, useSearchParams } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import LiveVodComments from "@/components/live-vod/LiveVodComments"
import { toPrivacyEmbedUrl, YOUTUBE_EMBED_ALLOW } from "@/lib/liveVod"
import { toggleLiveVodFavorite } from "@/lib/liveVodFavorites"
import { recordLiveVodWatch } from "@/lib/liveVodHistory"
import {
  fetchLiveVodEngagement,
  toggleLiveVodFavoriteApi,
} from "@/lib/liveVodEngagement"

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

function IconBookmark({ filled, className }: { filled?: boolean; className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" width="20" height="20" aria-hidden>
      <path
        fill={filled ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="1.8"
        d="M6 4.5h12v16l-6-3.2L6 20.5z"
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

function WatchTitle({ title }: { title: string }) {
  const parts = title.split(/(#[^\s#]+)/g).filter((part) => part.length > 0)
  return (
    <h1 className="flv-watch-title">
      {parts.map((part, index) =>
        part.startsWith("#") ? (
          <span key={`${part}-${index}`} className="flv-watch-hashtag">
            {part}
          </span>
        ) : (
          <span key={`${part}-${index}`}>{part}</span>
        ),
      )}
    </h1>
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
      : tab === "HISTORY"
        ? "/my/history"
        : tab === "ALL"
          ? "/live-vod"
          : `/live-vod?tab=${encodeURIComponent(tab)}`
  const embedSrc = toPrivacyEmbedUrl(videoId)
  const [favorited, setFavorited] = useState(false)
  const [favoriteCount, setFavoriteCount] = useState(0)
  const [commentCount, setCommentCount] = useState(0)
  const [shareHint, setShareHint] = useState<string | null>(null)

  const requireLogin = () => {
    const returnTo =
      typeof window !== "undefined"
        ? `${window.location.pathname}${window.location.search}`
        : "/live-vod"
    router.push(`/login?next=${encodeURIComponent(returnTo)}`)
  }

  useEffect(() => {
    if (!videoId) return
    recordLiveVodWatch({
      videoId,
      title,
      channelTitle: channel,
      thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      tab: tab === "FAVORITES" || tab === "HISTORY" ? "ALL" : tab,
      watchUrl: `https://www.youtube.com/watch?v=${videoId}`,
    })
  }, [videoId, title, channel, tab])

  useEffect(() => {
    if (!videoId) return
    let cancelled = false
    void fetchLiveVodEngagement(videoId)
      .then((eng) => {
        if (cancelled) return
        setFavoriteCount(eng.favoriteCount)
        setCommentCount(eng.commentCount)
        setFavorited(Boolean(eng.favorited))
      })
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [videoId, user?.email])

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

  const onToggleFavorite = async () => {
    if (!ready) return
    if (!user) {
      requireLogin()
      return
    }
    try {
      const result = await toggleLiveVodFavoriteApi(videoId)
      setFavorited(result.favorited)
      setFavoriteCount(result.favoriteCount)
      const localOn = isStillLocalFavorite(videoId)
      if (result.favorited !== localOn) {
        toggleLiveVodFavorite({
          videoId,
          title,
          channelTitle: channel,
          thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
          tab: tab === "FAVORITES" ? "ALL" : tab,
        })
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : ""
      if (message.includes("로그인")) {
        requireLogin()
        return
      }
      setShareHint("즐겨찾기 저장에 실패했습니다")
      window.setTimeout(() => setShareHint(null), 2000)
    }
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
            <WatchTitle title={title} />
            <div className="flv-watch-actions" role="toolbar" aria-label="영상 도구">
              <button type="button" className="flv-icon-btn" onClick={() => window.print()} aria-label="인쇄">
                <IconPrint />
              </button>
              <button
                type="button"
                className={`flv-icon-btn${favorited ? " is-on" : ""}`}
                onClick={() => void onToggleFavorite()}
                aria-label={favorited ? "즐겨찾기 해제" : "즐겨찾기"}
                aria-pressed={favorited}
              >
                <IconBookmark filled={favorited} />
              </button>
              <button type="button" className="flv-icon-btn" onClick={() => void onShare()} aria-label="공유">
                <IconShare />
              </button>
            </div>
          </div>
          <div className="flv-engage-row">
            <span>♡ 즐겨찾기 {favoriteCount}</span>
            <span>💬 댓글 {commentCount}</span>
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

        <LiveVodComments videoId={videoId} onCountChange={setCommentCount} />
      </div>
    </div>
  )
}

function isStillLocalFavorite(videoId: string): boolean {
  try {
    const raw = window.localStorage.getItem("finsight.liveVod.favorites.v1")
    if (!raw) return false
    const parsed = JSON.parse(raw) as Array<{ videoId?: string }>
    return Array.isArray(parsed) && parsed.some((item) => item.videoId === videoId)
  } catch {
    return false
  }
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
