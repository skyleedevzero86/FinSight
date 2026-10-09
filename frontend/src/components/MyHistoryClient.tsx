"use client"

import { useEffect, useMemo, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import FcbBoardFrame from "@/components/community/FcbBoardFrame"
import { FcbTabList } from "@/components/community/FcbManageShell"
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

const PAGE_SIZE = 20

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

function openHistory(item: BrowseHistoryItem) {
  if (item.kind !== "LIVE_VOD") return
  const matched = item.key.match(/^live-vod:(.+)$/)
  if (!matched?.[1]) return
  stashLiveVodMetaHint({
    videoId: matched[1],
    title: item.title,
    channelTitle: item.subtitle,
    thumbnailUrl: item.thumbnailUrl,
  })
}

export default function MyHistoryClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [items, setItems] = useState<BrowseHistoryItem[]>([])
  const [page, setPage] = useState(0)
  const [sortMode, setSortMode] = useState<HistorySortMode>("DATE")
  const [searchInput, setSearchInput] = useState("")
  const [searchQuery, setSearchQuery] = useState("")
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
    const ordered =
      sortMode === "DATE"
        ? [...items].sort((a, b) => (a.viewedAt < b.viewedAt ? 1 : -1))
        : [...items].sort((a, b) => {
            const sa = popularityScores[a.key] ?? 0
            const sb = popularityScores[b.key] ?? 0
            if (sa !== sb) return sb - sa
            return a.viewedAt < b.viewedAt ? 1 : -1
          })
    const query = searchQuery.trim().toLowerCase()
    if (!query) return ordered
    return ordered.filter((item) => {
      const title = item.title.toLowerCase()
      const subtitle = (item.subtitle ?? "").toLowerCase()
      return title.includes(query) || subtitle.includes(query)
    })
  }, [items, sortMode, popularityScores, searchQuery])

  const totalPages = Math.max(1, Math.ceil(sortedItems.length / PAGE_SIZE))

  useEffect(() => {
    setPage((prev) => Math.min(prev, totalPages - 1))
  }, [totalPages])

  useEffect(() => {
    setPage(0)
  }, [sortMode, searchQuery])

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
    <FcbBoardFrame
      boardId="bbs_my_history"
      heading="나의 시청 기록"
      description="최근에 본 게시물입니다. 제목을 누르면 다시 볼 수 있습니다."
      caption="시청 기록"
      totalCount={sortedItems.length}
      currentPage={page + 1}
      totalPages={totalPages}
      onPage={(next) => setPage(next - 1)}
      search={{
        type: "",
        value: searchInput,
        typeOptions: [],
        onTypeChange: () => undefined,
        onValueChange: setSearchInput,
        onSubmit: () => setSearchQuery(searchInput.trim()),
      }}
      beforeList={
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <FcbTabList
            label="시청 기록 정렬"
            activeKey={sortMode}
            onSelect={(key) => selectSort(key as HistorySortMode)}
            items={[
              { key: "DATE", label: "날짜순" },
              { key: "POPULAR", label: "인기순" },
            ]}
          />
          {items.length > 0 ? (
            <button
              type="button"
              className="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-800 hover:bg-gray-50"
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
      }
    >
      <thead>
        <tr>
          <th className="td_num">번호</th>
          <th className="td_subject" style={{ textAlign: "center" }}>
            제목
          </th>
          <th className="td_name">채널</th>
          <th className="td_date">시청일</th>
          <th className="td_action">관리</th>
        </tr>
      </thead>
      <tbody>
        {items.length === 0 ? (
          <tr>
            <td className="td_subject" colSpan={5}>
              아직 기록이 없습니다. 상세 페이지를 보면 여기에 남습니다.
            </td>
          </tr>
        ) : sortedItems.length === 0 ? (
          <tr>
            <td className="td_subject" colSpan={5}>
              검색 결과가 없습니다.
            </td>
          </tr>
        ) : (
          pageItems.map((item, index) => (
            <tr key={item.key}>
              <td className="td_num">{page * PAGE_SIZE + index + 1}</td>
              <td className="td_subject">
                <Link href={item.href} onClick={() => openHistory(item)}>
                  {item.title}
                </Link>
              </td>
              <td className="td_name">{item.subtitle || "-"}</td>
              <td className="td_date">{formatViewedAt(item.viewedAt) || "-"}</td>
              <td className="td_action">
                <button
                  type="button"
                  className="rounded border border-gray-300 bg-white px-2 py-1 text-xs text-gray-800 hover:bg-gray-50"
                  onClick={() => {
                    removeBrowseHistory(item.key)
                    if (item.kind === "LIVE_VOD") {
                      const matched = item.key.match(/^live-vod:(.+)$/)
                      if (matched?.[1]) removeLiveVodHistory(matched[1])
                    }
                  }}
                >
                  삭제
                </button>
              </td>
            </tr>
          ))
        )}
        {sortMode === "POPULAR" && popularityLoading ? (
          <tr>
            <td className="td_subject" colSpan={5}>
              인기순으로 정렬하는 중…
            </td>
          </tr>
        ) : null}
      </tbody>
    </FcbBoardFrame>
  )
}
