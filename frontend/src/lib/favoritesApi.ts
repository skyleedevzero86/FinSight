import { authHeadersJson } from "@/lib/finsightToken"
import {
  unscrapBoard as unscrapBoardApi,
  unwrapApiData,
  type BoardListItem,
  type BoardTypeCode,
} from "@/lib/boardApi"

export type LiveVodMyFavoriteItem = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  savedAt: string | null
}

export type LiveVodMyFavoritePage = {
  items: LiveVodMyFavoriteItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

export function boardHref(boardType: BoardTypeCode | string, id: number): string {
  const t = String(boardType || "").toUpperCase()
  if (t === "NOTICE") return `/community/notice/${id}`
  if (t === "QNA") return `/community/qna/${id}`
  if (t === "FREE") return `/community/free/${id}`
  return `/community/qna/${id}`
}

export async function fetchMyLiveVodFavorites(
  page = 0,
  size = 15,
): Promise<LiveVodMyFavoritePage> {
  const res = await fetch(`/api/v1/media/live-vod/my-favorites?page=${page}&size=${size}`, {
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  const json: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    const root = asRecord(json)
    throw new Error(typeof root?.message === "string" ? root.message : "즐겨찾기를 불러오지 못했습니다.")
  }
  const data = asRecord(unwrapApiData(json) ?? json)
  const itemsRaw = Array.isArray(data?.items) ? data.items : []
  const items: LiveVodMyFavoriteItem[] = itemsRaw
    .map((row) => {
      const o = asRecord(row)
      if (!o || typeof o.videoId !== "string" || !o.videoId) return null
      return {
        videoId: o.videoId,
        title: typeof o.title === "string" ? o.title : "VOD",
        channelTitle: typeof o.channelTitle === "string" ? o.channelTitle : null,
        thumbnailUrl:
          typeof o.thumbnailUrl === "string" && o.thumbnailUrl
            ? o.thumbnailUrl
            : `https://i.ytimg.com/vi/${o.videoId}/hqdefault.jpg`,
        savedAt: typeof o.savedAt === "string" ? o.savedAt : null,
      }
    })
    .filter((v): v is LiveVodMyFavoriteItem => v != null)

  return {
    items,
    page: Number(data?.page) || page,
    size: Number(data?.size) || size,
    totalElements: Number(data?.totalElements) || items.length,
    totalPages: Math.max(1, Number(data?.totalPages) || 1),
    hasNext: data?.hasNext === true,
    hasPrevious: data?.hasPrevious === true,
  }
}

export async function fetchMyBoardScraps(page = 0, size = 15): Promise<{
  items: BoardListItem[]
  hasNext: boolean
}> {
  const res = await fetch(`/api/v1/boards/my-scraps?page=${page}&size=${size}`, {
    headers: { Accept: "application/json", ...authHeadersJson() },
    cache: "no-store",
  })
  const json: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    const root = asRecord(json)
    throw new Error(typeof root?.message === "string" ? root.message : "스크랩을 불러오지 못했습니다.")
  }
  const data = unwrapApiData<BoardListItem[]>(json)
  const items = Array.isArray(data) ? data : []
  return { items, hasNext: items.length >= size }
}

export async function unscrapBoard(boardId: number): Promise<boolean> {
  const result = await unscrapBoardApi(boardId)
  return result.ok
}
