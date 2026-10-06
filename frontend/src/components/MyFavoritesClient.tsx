"use client"

import Link from "next/link"
import { Suspense, useEffect, useMemo, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { liveVodWatchHref, stashLiveVodMetaHint } from "@/lib/liveVod"
import {
  listLiveVodFavorites,
  removeLiveVodFavorite,
  type LiveVodFavorite,
} from "@/lib/liveVodFavorites"
import { toggleLiveVodFavoriteApi } from "@/lib/liveVodEngagement"
import type { BoardListItem } from "@/lib/boardApi"
import {
  boardHref,
  fetchMyBoardScraps,
  fetchMyLiveVodFavorites,
  unscrapBoard,
  type LiveVodMyFavoriteItem,
} from "@/lib/favoritesApi"
import {
  listSiteFavorites,
  removeSiteFavorite,
  SITE_FAVORITE_PLACEHOLDER,
  SITE_FAVORITES_CHANGED_EVENT,
  type SiteFavoriteCategory,
  type SiteFavoriteItem,
} from "@/lib/siteFavorites"

const PAGE_SIZE = 15

type FavoriteTab = SiteFavoriteCategory

const FAVORITE_TABS: { key: FavoriteTab; label: string }[] = [
  { key: "NEWS", label: "뉴스" },
  { key: "ECONOMY_PICK", label: "경제Pick" },
  { key: "LIVE_VOD", label: "실시간VOD" },
  { key: "COMMUNITY", label: "커뮤니티" },
]

function parseTab(raw: string | null): FavoriteTab {
  const v = (raw || "").trim().toUpperCase()
  if (v === "NEWS" || v === "ECONOMY_PICK" || v === "LIVE_VOD" || v === "COMMUNITY") return v
  return "LIVE_VOD"
}

function emptyMessage(tab: FavoriteTab): string {
  if (tab === "NEWS") return "저장한 뉴스가 없습니다."
  if (tab === "ECONOMY_PICK") return "저장한 경제Pick이 없습니다."
  if (tab === "COMMUNITY") return "저장한 커뮤니티 게시물이 없습니다."
  return "저장한 실시간 VOD가 없습니다. 상세에서 별 아이콘을 눌러 추가해 보세요."
}

type GalleryItem = {
  key: string
  href: string
  title: string
  subtitle: string | null
  thumbnailUrl: string
  onOpen?: () => void
  onRemove: () => void | Promise<void>
}

function FavoriteGalleryCard({ item }: { item: GalleryItem }) {
  return (
    <li>
      <Link href={item.href} className="flv-thumb-link" onClick={item.onOpen}>
        <div className="flv-thumb-wrap">
          <img src={item.thumbnailUrl} alt="" />
        </div>
        <div className="flv-vod-title">{item.title}</div>
        {item.subtitle ? <div className="flv-hist-channel">{item.subtitle}</div> : null}
      </Link>
      <button
        type="button"
        className="flv-fav-remove"
        onClick={() => {
          void item.onRemove()
        }}
      >
        삭제
      </button>
    </li>
  )
}

function FavoritesBody() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { user, ready } = useAuthSession()
  const tab = parseTab(searchParams.get("tab"))
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [siteItems, setSiteItems] = useState<SiteFavoriteItem[]>([])
  const [localLiveItems, setLocalLiveItems] = useState<LiveVodFavorite[]>([])
  const [liveItems, setLiveItems] = useState<LiveVodMyFavoriteItem[]>([])
  const [liveTotalPages, setLiveTotalPages] = useState(1)
  const [liveFromServer, setLiveFromServer] = useState(false)
  const [communityItems, setCommunityItems] = useState<BoardListItem[]>([])
  const [communityHasNext, setCommunityHasNext] = useState(false)

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/myinfo/favorites")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    setPage(0)
  }, [tab])

  useEffect(() => {
    const syncSite = () => setSiteItems(listSiteFavorites())
    const syncLocalLive = () => setLocalLiveItems(listLiveVodFavorites())
    syncSite()
    syncLocalLive()
    window.addEventListener(SITE_FAVORITES_CHANGED_EVENT, syncSite)
    window.addEventListener("finsight:live-vod-favorites-changed", syncLocalLive)
    window.addEventListener("storage", syncSite)
    window.addEventListener("storage", syncLocalLive)
    return () => {
      window.removeEventListener(SITE_FAVORITES_CHANGED_EVENT, syncSite)
      window.removeEventListener("finsight:live-vod-favorites-changed", syncLocalLive)
      window.removeEventListener("storage", syncSite)
      window.removeEventListener("storage", syncLocalLive)
    }
  }, [])

  useEffect(() => {
    if (!ready || !user) return
    if (tab !== "LIVE_VOD" && tab !== "COMMUNITY") return
    let cancelled = false
    setLoading(true)
    setError(null)
    const run = async () => {
      try {
        if (tab === "LIVE_VOD") {
          const data = await fetchMyLiveVodFavorites(page, PAGE_SIZE)
          if (cancelled) return
          setLiveItems(data.items)
          setLiveTotalPages(Math.max(1, data.totalPages))
          setLiveFromServer(true)
          return
        }
        const data = await fetchMyBoardScraps(page, PAGE_SIZE)
        if (cancelled) return
        setCommunityItems(data.items)
        setCommunityHasNext(data.hasNext)
      } catch (err) {
        if (cancelled) return
        if (tab === "LIVE_VOD") {
          setLiveItems([])
          setLiveTotalPages(1)
          setLiveFromServer(false)
        } else {
          setCommunityItems([])
          setCommunityHasNext(false)
        }
        setError(err instanceof Error ? err.message : "목록을 불러오지 못했습니다.")
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [ready, user, tab, page])

  const sitePageItems = useMemo(() => {
    const filtered = siteItems.filter((it) => it.category === tab)
    const start = page * PAGE_SIZE
    return filtered.slice(start, start + PAGE_SIZE)
  }, [siteItems, tab, page])

  const siteTotalPages = useMemo(() => {
    const filtered = siteItems.filter((it) => it.category === tab)
    return Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  }, [siteItems, tab])

  const localLivePageItems = useMemo(() => {
    const start = page * PAGE_SIZE
    return localLiveItems.slice(start, start + PAGE_SIZE)
  }, [localLiveItems, page])

  const localLiveTotalPages = Math.max(1, Math.ceil(localLiveItems.length / PAGE_SIZE))

  const galleryItems: GalleryItem[] = useMemo(() => {
    if (tab === "NEWS" || tab === "ECONOMY_PICK") {
      return sitePageItems.map((it) => ({
        key: it.key,
        href: it.href,
        title: it.title,
        subtitle: it.subtitle,
        thumbnailUrl: it.thumbnailUrl || SITE_FAVORITE_PLACEHOLDER,
        onRemove: () => removeSiteFavorite(it.key),
      }))
    }
    if (tab === "LIVE_VOD") {
      if (liveFromServer) {
        return liveItems.map((it) => ({
          key: `live-${it.videoId}`,
          href: liveVodWatchHref({ videoId: it.videoId }, "FAVORITES"),
          title: it.title,
          subtitle: it.channelTitle,
          thumbnailUrl: it.thumbnailUrl,
          onOpen: () =>
            stashLiveVodMetaHint({
              videoId: it.videoId,
              title: it.title,
              channelTitle: it.channelTitle,
              thumbnailUrl: it.thumbnailUrl,
            }),
          onRemove: async () => {
            try {
              await toggleLiveVodFavoriteApi(it.videoId)
            } catch {
            }
            removeLiveVodFavorite(it.videoId)
            setLiveItems((prev) => prev.filter((row) => row.videoId !== it.videoId))
          },
        }))
      }
      return localLivePageItems.map((it) => ({
        key: `local-live-${it.videoId}`,
        href: liveVodWatchHref({ videoId: it.videoId }, "FAVORITES"),
        title: it.title,
        subtitle: it.channelTitle,
        thumbnailUrl: it.thumbnailUrl,
        onOpen: () =>
          stashLiveVodMetaHint({
            videoId: it.videoId,
            title: it.title,
            channelTitle: it.channelTitle,
            thumbnailUrl: it.thumbnailUrl,
          }),
        onRemove: () => removeLiveVodFavorite(it.videoId),
      }))
    }
    return communityItems.map((it) => ({
      key: `board-${it.id}`,
      href: boardHref(it.boardType, it.id),
      title: it.title,
      subtitle: it.boardType,
      thumbnailUrl: SITE_FAVORITE_PLACEHOLDER,
      onRemove: async () => {
        const ok = await unscrapBoard(it.id)
        if (ok) setCommunityItems((prev) => prev.filter((row) => row.id !== it.id))
      },
    }))
  }, [tab, sitePageItems, liveFromServer, liveItems, localLivePageItems, communityItems])

  const totalPages =
    tab === "NEWS" || tab === "ECONOMY_PICK"
      ? siteTotalPages
      : tab === "LIVE_VOD"
        ? liveFromServer
          ? liveTotalPages
          : localLiveTotalPages
        : Math.max(1, page + (communityHasNext ? 2 : 1))

  function selectTab(next: FavoriteTab) {
    const params = new URLSearchParams(searchParams.toString())
    params.set("tab", next)
    router.replace(`/myinfo/favorites?${params.toString()}`)
  }

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
            <p className="flv-fav-desc">
              관심있는 목록입니다. 게시물을 누르면 다시 볼 수가 있습니다.
            </p>
          </div>
        </div>

        <div className="flv-fav-tabs" role="tablist" aria-label="즐겨찾기 분류">
          {FAVORITE_TABS.map((item) => (
            <button
              key={item.key}
              type="button"
              role="tab"
              aria-selected={tab === item.key}
              className={`flv-fav-tab${tab === item.key ? " is-active" : ""}`}
              onClick={() => selectTab(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {loading ? <p className="flv-hist-empty">불러오는 중…</p> : null}
        {error && galleryItems.length === 0 ? <p className="flv-hist-empty">{error}</p> : null}

        {!loading && galleryItems.length === 0 ? (
          <p className="flv-hist-empty">{emptyMessage(tab)}</p>
        ) : !loading ? (
          <section className="flv-relate flv-favorites">
            <ul>
              {galleryItems.map((it) => (
                <FavoriteGalleryCard key={it.key} item={it} />
              ))}
            </ul>
            {totalPages > 1 || communityHasNext || page > 0 ? (
              <div className="flv-reply-pager" role="navigation" aria-label="즐겨찾기 페이지">
                <button
                  type="button"
                  className="flv-reply-page-btn"
                  disabled={page <= 0}
                  aria-label="이전 페이지"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  ‹
                </button>
                <span className="flv-reply-page-indicator">
                  {page + 1}
                  {tab === "COMMUNITY" ? "" : ` / ${totalPages}`}
                </span>
                <button
                  type="button"
                  className="flv-reply-page-btn"
                  disabled={
                    tab === "COMMUNITY" ? !communityHasNext : page + 1 >= totalPages
                  }
                  aria-label="다음 페이지"
                  onClick={() => setPage((p) => p + 1)}
                >
                  ›
                </button>
              </div>
            ) : null}
          </section>
        ) : null}
      </div>
    </div>
  )
}

export default function MyFavoritesClient() {
  return (
    <Suspense
      fallback={
        <div className="mx-auto max-w-[960px] px-4 py-10 md:px-6">
          <p className="text-sm text-gray-500">불러오는 중…</p>
        </div>
      }
    >
      <FavoritesBody />
    </Suspense>
  )
}
