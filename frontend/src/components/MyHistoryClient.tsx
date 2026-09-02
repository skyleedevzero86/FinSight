"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { liveVodWatchHref, stashLiveVodMetaHint } from "@/lib/liveVod"
import {
  listLiveVodFavorites,
  removeLiveVodFavorite,
  type LiveVodFavorite,
} from "@/lib/liveVodFavorites"
import {
  clearLiveVodHistory,
  listLiveVodHistory,
  LIVE_VOD_HISTORY_CHANGED_EVENT,
  removeLiveVodHistory,
  type LiveVodHistoryItem,
} from "@/lib/liveVodHistory"

function formatWatchedAt(iso: string): string {
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return ""
    return new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    }).format(d)
  } catch {
    return ""
  }
}

function HistoryGalleryCard({
  videoId,
  title,
  channelTitle,
  thumbnailUrl,
  tab,
  meta,
  onRemove,
  removeLabel,
}: {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  tab: string
  meta?: string
  onRemove: () => void
  removeLabel: string
}) {
  return (
    <li>
      <Link
        href={liveVodWatchHref({ videoId }, tab)}
        className="flv-thumb-link"
        onClick={() =>
          stashLiveVodMetaHint({
            videoId,
            title,
            channelTitle,
            thumbnailUrl,
          })
        }
      >
        <div className="flv-thumb-wrap">
          <img src={thumbnailUrl} alt="" />
        </div>
        <div className="flv-vod-title">{title}</div>
        {channelTitle ? <div className="flv-hist-channel">{channelTitle}</div> : null}
        {meta ? <div className="flv-hist-meta">{meta}</div> : null}
      </Link>
      <button type="button" className="flv-fav-remove" onClick={onRemove}>
        {removeLabel}
      </button>
    </li>
  )
}

export default function MyHistoryClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [watched, setWatched] = useState<LiveVodHistoryItem[]>([])
  const [favorites, setFavorites] = useState<LiveVodFavorite[]>([])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/my/history")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    const syncHistory = () => setWatched(listLiveVodHistory())
    const syncFavorites = () => setFavorites(listLiveVodFavorites())
    const syncAll = () => {
      syncHistory()
      syncFavorites()
    }
    syncAll()
    window.addEventListener(LIVE_VOD_HISTORY_CHANGED_EVENT, syncHistory)
    window.addEventListener("finsight:live-vod-favorites-changed", syncFavorites)
    window.addEventListener("storage", syncAll)
    return () => {
      window.removeEventListener(LIVE_VOD_HISTORY_CHANGED_EVENT, syncHistory)
      window.removeEventListener("finsight:live-vod-favorites-changed", syncFavorites)
      window.removeEventListener("storage", syncAll)
    }
  }, [])

  if (!ready || !user) {
    return (
      <div className="mx-auto max-w-[960px] px-4 py-10 md:px-6">
        <p className="text-sm text-gray-500">로그인 확인 중…</p>
      </div>
    )
  }

  return (
    <div className="finsight-live-vod-page">
      <div className="mx-auto max-w-[1240px] px-4 py-6 md:px-6 md:py-8">
        <div className="flv-toolbar flv-history-toolbar">
          <div>
            <h1>나의 LIVE/VOD 히스토리</h1>
            <p className="flv-fav-desc">
              최근에 본 YouTube VOD와 즐겨찾기한 영상을 갤러리로 모아 둡니다. 썸네일을 누르면 다시 시청할 수 있습니다.
            </p>
          </div>
          <div className="flv-history-links">
            <Link href="/live-vod" className="flv-back-link">
              LIVE/VOD 목록
            </Link>
            <Link href="/my/favorites" className="flv-back-link">
              즐겨찾기만 보기
            </Link>
          </div>
        </div>

        <section className="flv-relate flv-history-section">
          <div className="flv-history-heading">
            <h3>최근 시청</h3>
            {watched.length > 0 ? (
              <button
                type="button"
                className="flv-hist-clear"
                onClick={() => {
                  if (window.confirm("최근 시청 기록을 모두 삭제할까요?")) {
                    clearLiveVodHistory()
                  }
                }}
              >
                전체 삭제
              </button>
            ) : null}
          </div>
          {watched.length === 0 ? (
            <p className="flv-hist-empty">
              아직 시청 기록이 없습니다. LIVE/VOD에서 썸네일을 눌러 영상을 보면 여기에 남습니다.
            </p>
          ) : (
            <ul>
              {watched.map((it) => (
                <HistoryGalleryCard
                  key={`w-${it.videoId}`}
                  videoId={it.videoId}
                  title={it.title}
                  channelTitle={it.channelTitle}
                  thumbnailUrl={it.thumbnailUrl}
                  tab="HISTORY"
                  meta={formatWatchedAt(it.watchedAt)}
                  removeLabel="삭제"
                  onRemove={() => removeLiveVodHistory(it.videoId)}
                />
              ))}
            </ul>
          )}
        </section>

        <section className="flv-relate flv-history-section">
          <div className="flv-history-heading">
            <h3>즐겨찾기</h3>
          </div>
          {favorites.length === 0 ? (
            <p className="flv-hist-empty">
              저장한 영상이 없습니다. 시청 화면에서 북마크 아이콘을 눌러 추가해 보세요.
            </p>
          ) : (
            <ul>
              {favorites.map((it) => (
                <HistoryGalleryCard
                  key={`f-${it.videoId}`}
                  videoId={it.videoId}
                  title={it.title}
                  channelTitle={it.channelTitle}
                  thumbnailUrl={it.thumbnailUrl}
                  tab="FAVORITES"
                  meta={formatWatchedAt(it.savedAt)}
                  removeLabel="해제"
                  onRemove={() => removeLiveVodFavorite(it.videoId)}
                />
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
