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

/**
 * Displays the authenticated user's saved LIVE/VOD videos.
 *
 * Unauthenticated users are redirected to the login page. The list stays synchronized
 * with favorite changes and browser storage updates.
 *
 * @returns The favorites page content.
 */
export default function MyFavoritesClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [items, setItems] = useState<LiveVodFavorite[]>([])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/my/favorites")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    const sync = () => setItems(listLiveVodFavorites())
    sync()
    window.addEventListener("finsight:live-vod-favorites-changed", sync)
    window.addEventListener("storage", sync)
    return () => {
      window.removeEventListener("finsight:live-vod-favorites-changed", sync)
      window.removeEventListener("storage", sync)
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
            <h1>나의 즐겨찾기</h1>
            <p className="flv-fav-desc">저장한 LIVE/VOD 영상 목록입니다. 썸네일을 누르면 다시 시청할 수 있습니다.</p>
          </div>
          <Link href="/my/history" className="flv-back-link">
            시청 기록 보기
          </Link>
        </div>

        {items.length === 0 ? (
          <p className="text-sm text-gray-500">
            아직 저장한 영상이 없습니다. LIVE/VOD 상세에서 별 아이콘을 눌러 추가해 보세요.
          </p>
        ) : (
          <section className="flv-relate flv-favorites">
            <ul>
              {items.map((it) => (
                <li key={it.videoId}>
                  <Link
                    href={liveVodWatchHref({ videoId: it.videoId }, "FAVORITES")}
                    className="flv-thumb-link"
                    onClick={() =>
                      stashLiveVodMetaHint({
                        videoId: it.videoId,
                        title: it.title,
                        channelTitle: it.channelTitle,
                        thumbnailUrl: it.thumbnailUrl,
                      })
                    }
                  >
                    <div className="flv-thumb-wrap">
                      <img src={it.thumbnailUrl} alt="" />
                    </div>
                    <div className="flv-vod-title">{it.title}</div>
                    {it.channelTitle ? (
                      <div className="flv-hist-channel">{it.channelTitle}</div>
                    ) : null}
                  </Link>
                  <button
                    type="button"
                    className="flv-fav-remove"
                    onClick={() => removeLiveVodFavorite(it.videoId)}
                  >
                    삭제
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </div>
  )
}
