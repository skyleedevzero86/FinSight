"use client"

import Link from "next/link"
import { Suspense, useEffect, useMemo, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import FcbBoardFrame from "@/components/community/FcbBoardFrame"
import { liveVodWatchHref, stashLiveVodMetaHint } from "@/lib/liveVod"
import {
  listLiveVodFavorites,
  removeLiveVodFavorite,
  type LiveVodFavorite,
} from "@/lib/liveVodFavorites"
import { toggleLiveVodFavoriteApi } from "@/lib/liveVodEngagement"
import { formatAuthor, formatBoardDate, type BoardListItem } from "@/lib/boardApi"
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
  SITE_FAVORITES_CHANGED_EVENT,
  type SiteFavoriteCategory,
  type SiteFavoriteItem,
} from "@/lib/siteFavorites"

const PAGE_SIZE = 20

type FavoriteTab = SiteFavoriteCategory

const FAVORITE_TABS: { key: FavoriteTab; label: string }[] = [
  { key: "NEWS", label: "뉴스" },
  { key: "ECONOMY_PICK", label: "경제Pick" },
  { key: "LIVE_VOD", label: "실시간VOD" },
  { key: "COMMUNITY", label: "커뮤니티" },
]

type FavoriteRow = {
  key: string
  href: string
  title: string
  author: string
  date: string
  hits: string
  onOpen?: () => void
  onRemove: () => void | Promise<void>
}

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

function matchesQuery(type: string, keyword: string, title: string, extra: string): boolean {
  const query = keyword.trim().toLowerCase()
  if (!query) return true
  const titleHit = title.toLowerCase().includes(query)
  const extraHit = extra.toLowerCase().includes(query)
  if (type === "subject") return titleHit
  if (type === "content") return extraHit
  return titleHit || extraHit
}

function savedDate(iso: string | null | undefined): string {
  if (!iso) return "-"
  return formatBoardDate(iso) || "-"
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
  const [liveTotalElements, setLiveTotalElements] = useState(0)
  const [liveFromServer, setLiveFromServer] = useState(false)
  const [communityItems, setCommunityItems] = useState<BoardListItem[]>([])
  const [communityTotal, setCommunityTotal] = useState(0)
  const [communityTotalPages, setCommunityTotalPages] = useState(1)
  const [draftType, setDraftType] = useState("")
  const [draftValue, setDraftValue] = useState("")
  const [appliedType, setAppliedType] = useState("")
  const [appliedValue, setAppliedValue] = useState("")

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/myinfo/favorites")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    setPage(0)
    setError(null)
    setDraftType("")
    setDraftValue("")
    setAppliedType("")
    setAppliedValue("")
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
    if (tab !== "LIVE_VOD" && tab !== "COMMUNITY") {
      setLoading(false)
      return
    }
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
          setLiveTotalElements(data.totalElements)
          setLiveFromServer(true)
          return
        }
        const data = await fetchMyBoardScraps(page, PAGE_SIZE)
        if (cancelled) return
        setCommunityItems(data.items)
        setCommunityTotal(data.totalElements)
        setCommunityTotalPages(Math.max(1, data.totalPages))
      } catch (err) {
        if (cancelled) return
        if (tab === "LIVE_VOD") {
          setLiveItems([])
          setLiveTotalPages(1)
          setLiveTotalElements(0)
          setLiveFromServer(false)
        } else {
          setCommunityItems([])
          setCommunityTotal(0)
          setCommunityTotalPages(1)
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

  const siteFiltered = useMemo(() => {
    return siteItems.filter(
      (it) =>
        it.category === tab &&
        matchesQuery(appliedType, appliedValue, it.title, it.subtitle ?? ""),
    )
  }, [siteItems, tab, appliedType, appliedValue])

  const localLiveFiltered = useMemo(() => {
    return localLiveItems.filter((it) =>
      matchesQuery(appliedType, appliedValue, it.title, it.channelTitle ?? ""),
    )
  }, [localLiveItems, appliedType, appliedValue])

  const rows: FavoriteRow[] = useMemo(() => {
    if (tab === "NEWS" || tab === "ECONOMY_PICK") {
      const start = page * PAGE_SIZE
      return siteFiltered.slice(start, start + PAGE_SIZE).map((it) => ({
        key: it.key,
        href: it.href,
        title: it.title,
        author: it.subtitle || "-",
        date: savedDate(it.savedAt),
        hits: "-",
        onRemove: () => removeSiteFavorite(it.key),
      }))
    }
    if (tab === "LIVE_VOD" && !liveFromServer) {
      const start = page * PAGE_SIZE
      return localLiveFiltered.slice(start, start + PAGE_SIZE).map((it) => ({
        key: `local-live-${it.videoId}`,
        href: liveVodWatchHref({ videoId: it.videoId }, "FAVORITES"),
        title: it.title,
        author: it.channelTitle || "-",
        date: savedDate(it.savedAt),
        hits: "-",
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
    if (tab === "LIVE_VOD") {
      return liveItems
        .filter((it) => matchesQuery(appliedType, appliedValue, it.title, it.channelTitle ?? ""))
        .map((it) => ({
          key: `live-${it.videoId}`,
          href: liveVodWatchHref({ videoId: it.videoId }, "FAVORITES"),
          title: it.title,
          author: it.channelTitle || "-",
          date: savedDate(it.savedAt),
          hits: "-",
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
              console.warn("실시간 VOD 즐겨찾기 해제에 실패했습니다.", it.videoId)
            }
            removeLiveVodFavorite(it.videoId)
            setLiveItems((prev) => prev.filter((row) => row.videoId !== it.videoId))
            setLiveTotalElements((prev) => Math.max(0, prev - 1))
          },
        }))
    }
    return communityItems
      .filter((it) =>
        matchesQuery(appliedType, appliedValue, it.title, `${it.authorEmail} ${it.boardType}`),
      )
      .map((it) => ({
        key: `board-${it.id}`,
        href: boardHref(it.boardType, it.id),
        title: it.title,
        author: formatAuthor(it.authorEmail) || "-",
        date: savedDate(it.createdAt),
        hits: String(it.viewCount ?? 0),
        onRemove: async () => {
          const ok = await unscrapBoard(it.id)
          if (!ok) {
            setError("즐겨찾기를 해제하지 못했습니다.")
            return
          }
          setCommunityItems((prev) => prev.filter((row) => row.id !== it.id))
          setCommunityTotal((prev) => Math.max(0, prev - 1))
        },
      }))
  }, [
    tab,
    page,
    siteFiltered,
    localLiveFiltered,
    liveFromServer,
    liveItems,
    communityItems,
    appliedType,
    appliedValue,
  ])

  const siteTotalPages = Math.max(1, Math.ceil(siteFiltered.length / PAGE_SIZE))
  const localLiveTotalPages = Math.max(1, Math.ceil(localLiveFiltered.length / PAGE_SIZE))
  const totalPages =
    tab === "NEWS" || tab === "ECONOMY_PICK"
      ? siteTotalPages
      : tab === "LIVE_VOD"
        ? liveFromServer
          ? liveTotalPages
          : localLiveTotalPages
        : communityTotalPages
  const totalCount =
    tab === "NEWS" || tab === "ECONOMY_PICK"
      ? siteFiltered.length
      : tab === "LIVE_VOD"
        ? liveFromServer
          ? liveTotalElements
          : localLiveFiltered.length
        : communityTotal

  function selectTab(next: FavoriteTab) {
    const params = new URLSearchParams(searchParams.toString())
    params.set("tab", next)
    router.replace(`/myinfo/favorites?${params.toString()}`)
  }

  if (!ready || !user) {
    return <p className="px-6 py-10 text-sm text-gray-500">로그인 확인 중…</p>
  }

  const rowNo = (index: number) => page * PAGE_SIZE + index + 1

  return (
    <FcbBoardFrame
      boardId="bbs_my_favorites"
      heading="나의 즐겨찾기"
      description="관심있는 목록입니다. 게시물을 누르면 다시 볼 수가 있습니다."
      caption="나의 즐겨찾기"
      totalCount={totalCount}
      currentPage={page + 1}
      totalPages={totalPages}
      onPage={(next) => setPage(Math.max(0, next - 1))}
      search={{
        type: draftType,
        value: draftValue,
        onTypeChange: setDraftType,
        onValueChange: setDraftValue,
        onSubmit: () => {
          setAppliedType(draftType)
          setAppliedValue(draftValue.trim())
          setPage(0)
        },
      }}
      beforeList={
        <div className="bbs_cate tablist fcb-tablist">
          <ul className="tablist_3d fcb-tablist-3d">
            {FAVORITE_TABS.map((item) => (
              <li key={item.key} className={tab === item.key ? "on fcb-on" : undefined}>
                <button type="button" onClick={() => selectTab(item.key)}>
                  {item.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      }
    >
      <thead>
        <tr>
          <th className="td_num">번호</th>
          <th className="td_subject" style={{ textAlign: "center" }}>
            제목
          </th>
          <th className="td_name">작성자</th>
          <th className="td_file">첨부</th>
          <th className="td_date">작성일</th>
          <th className="td_hit">조회수</th>
          <th className="td_action">관리</th>
        </tr>
      </thead>
      <tbody>
        {loading ? (
          <tr>
            <td className="td_subject" colSpan={7}>
              불러오는 중…
            </td>
          </tr>
        ) : error && rows.length === 0 ? (
          <tr>
            <td className="td_subject" colSpan={7}>
              {error}
            </td>
          </tr>
        ) : rows.length === 0 ? (
          <tr>
            <td className="td_subject" colSpan={7}>
              {emptyMessage(tab)}
            </td>
          </tr>
        ) : (
          rows.map((row, index) => (
            <tr key={row.key}>
              <td className="td_num">{rowNo(index)}</td>
              <td className="td_subject">
                <Link href={row.href} onClick={row.onOpen}>
                  {row.title}
                </Link>
              </td>
              <td className="td_name">{row.author}</td>
              <td className="td_file" />
              <td className="td_date">{row.date}</td>
              <td className="td_hit">{row.hits}</td>
              <td className="td_action">
                <button
                  type="button"
                  className="rounded border border-gray-300 bg-white px-2 py-1 text-xs text-gray-800 hover:bg-gray-50"
                  onClick={() => {
                    void row.onRemove()
                  }}
                >
                  삭제
                </button>
              </td>
            </tr>
          ))
        )}
      </tbody>
    </FcbBoardFrame>
  )
}

export default function MyFavoritesClient() {
  return (
    <Suspense fallback={<p className="px-6 py-10 text-sm text-gray-500">불러오는 중…</p>}>
      <FavoritesBody />
    </Suspense>
  )
}
