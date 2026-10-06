"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { stashLiveVodMetaHint } from "@/lib/liveVod"
import {
  BROWSE_HISTORY_CHANGED_EVENT,
  clearBrowseHistory,
  listBrowseHistory,
  removeBrowseHistory,
  type BrowseHistoryItem,
} from "@/lib/browseHistory"
import { clearLiveVodHistory, removeLiveVodHistory } from "@/lib/liveVodHistory"
import {
  fetchHistoryPopularityScores,
  toPopularityQueryItems,
  type HistorySortMode,
} from "@/lib/historyPopularity"

const PAGE_SIZE = 15

function formatViewedAt(iso: string): string {
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
  item,
  onRemove,
}: {
  item: BrowseHistoryItem
  onRemove: () => void
}) {
  return (
    <li>
      <Link
        href={item.href}
        className="flv-thumb-link"
        onClick={() => {
          if (item.kind !== "LIVE_VOD") return
          const m = item.key.match(/^live-vod:(.+)$/)
          if (!m?.[1]) return
          stashLiveVodMetaHint({
            videoId: m[1],
            title: item.title,
            channelTitle: item.subtitle,
            thumbnailUrl: item.thumbnailUrl,
          })
        }}
      >
        <div className="flv-thumb-wrap">
          <img src={item.thumbnailUrl} alt="" />
        </div>
        <div className="flv-vod-title">{item.title}</div>
        {item.subtitle ? <div className="flv-hist-channel">{item.subtitle}</div> : null}
        <div className="flv-hist-meta">{formatViewedAt(item.viewedAt)}</div>
      </Link>
      <button type="button" className="flv-fav-remove" onClick={onRemove}>
        삭제
      </button>
    </li>
  )
}

export default function MyHistoryClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [items, setItems] = useState<BrowseHistoryItem[]>([])
  const [page, setPage] = useState(0)
  const [sortMode, setSortMode] = useState<HistorySortMode>("DATE")
  const [popularityScores, setPopularityScores] = useState<Record<string, number>>({})
  const [popularityLoading, setPopularityLoading] = useState(false)

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/myinfo/history")}`)
    }
  }, [ready, user, router])

  useEffect(() => {
    const syncHistory = () => setItems(listBrowseHistory())
    syncHistory()
    window.addEventListener(BROWSE_HISTORY_CHANGED_EVENT, syncHistory)
    window.addEventListener("storage", syncHistory)
    return () => {
      window.removeEventListener(BROWSE_HISTORY_CHANGED_EVENT, syncHistory)
      window.removeEventListener("storage", syncHistory)
    }
  }, [])

  useEffect(() => {
    if (sortMode !== "POPULAR" || items.length === 0) return
    let cancelled = false
    setPopularityLoading(true)
    void fetchHistoryPopularityScores(toPopularityQueryItems(items))
      .then((scores) => {
        if (!cancelled) setPopularityScores(scores)
      })
      .catch(() => {
        if (!cancelled) setPopularityScores({})
      })
      .finally(() => {
        if (!cancelled) setPopularityLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [sortMode, items])

  const sortedItems = useMemo(() => {
    if (sortMode === "DATE") {
      return [...items].sort((a, b) => (a.viewedAt < b.viewedAt ? 1 : -1))
    }
    return [...items].sort((a, b) => {
      const sa = popularityScores[a.key] ?? 0
      const sb = popularityScores[b.key] ?? 0
      if (sa !== sb) return sb - sa
      return a.viewedAt < b.viewedAt ? 1 : -1
    })
  }, [items, sortMode, popularityScores])

  const totalPages = Math.max(1, Math.ceil(sortedItems.length / PAGE_SIZE))

  useEffect(() => {
    setPage((prev) => Math.min(prev, totalPages - 1))
  }, [totalPages])

  useEffect(() => {
    setPage(0)
  }, [sortMode])

  const pageItems = useMemo(() => {
    const start = page * PAGE_SIZE
    return sortedItems.slice(start, start + PAGE_SIZE)
  }, [sortedItems, page])

  function selectSort(mode: HistorySortMode) {
    setSortMode(mode)
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
            <h1>시청 기록</h1>
            <p className="flv-fav-desc">
              최근에 본 게시물을 갤러리로 모아 둡니다. 썸네일을 누르면 다시 시청할 수 있습니다.
            </p>
          </div>
          <div className="flv-history-links" role="group" aria-label="정렬">
            <button
              type="button"
              className={`flv-back-link${sortMode === "DATE" ? " is-active" : ""}`}
              aria-pressed={sortMode === "DATE"}
              onClick={() => selectSort("DATE")}
            >
              날짜순
            </button>
            <button
              type="button"
              className={`flv-back-link${sortMode === "POPULAR" ? " is-active" : ""}`}
              aria-pressed={sortMode === "POPULAR"}
              onClick={() => selectSort("POPULAR")}
            >
              인기순
            </button>
          </div>
        </div>

        <section className="flv-relate flv-history-section">
          <div className="flv-history-heading">
            <h3>최근 본 게시물</h3>
            {items.length > 0 ? (
              <button
                type="button"
                className="flv-hist-clear"
                onClick={() => {
                  if (window.confirm("최근 본 게시물 기록을 모두 삭제할까요?")) {
                    clearBrowseHistory()
                    clearLiveVodHistory()
                    setPage(0)
                  }
                }}
              >
                전체 삭제
              </button>
            ) : null}
          </div>
          {items.length === 0 ? (
            <p className="flv-hist-empty">
              아직 기록이 없습니다. 상세 페이지를 보면 여기에 남습니다.
            </p>
          ) : (
            <>
              {sortMode === "POPULAR" && popularityLoading ? (
                <p className="flv-hist-empty">인기순으로 정렬하는 중…</p>
              ) : null}
              <ul>
                {pageItems.map((it) => (
                  <HistoryGalleryCard
                    key={it.key}
                    item={it}
                    onRemove={() => {
                      removeBrowseHistory(it.key)
                      if (it.kind === "LIVE_VOD") {
                        const m = it.key.match(/^live-vod:(.+)$/)
                        if (m?.[1]) removeLiveVodHistory(m[1])
                      }
                    }}
                  />
                ))}
              </ul>
              {totalPages > 1 ? (
                <div className="flv-reply-pager" role="navigation" aria-label="시청 기록 페이지">
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
                    {page + 1} / {totalPages}
                  </span>
                  <button
                    type="button"
                    className="flv-reply-page-btn"
                    disabled={page + 1 >= totalPages}
                    aria-label="다음 페이지"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  >
                    ›
                  </button>
                </div>
              ) : null}
            </>
          )}
        </section>
      </div>
    </div>
  )
}
