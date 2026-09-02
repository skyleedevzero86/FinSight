export type LiveVodItem = {
  videoId: string
  title: string
  thumbnailUrl: string
  watchUrl: string
  embedUrl: string
  channelTitle: string | null
  favoriteCount: number
  commentCount: number
}

export type LiveVodSection = {
  heading: string
  items: LiveVodItem[]
}

export type LiveVodFeed = {
  title: string
  tab: string
  featuredVideoId: string | null
  featuredTitle: string | null
  featuredThumbnailUrl: string | null
  embedUrl: string | null
  sections: LiveVodSection[]
}

export function toPrivacyEmbedUrl(
  videoId: string | null | undefined,
  embedUrl?: string | null,
): string {
  const id =
    (typeof videoId === "string" && videoId.trim()) ||
    (typeof embedUrl === "string"
      ? embedUrl.match(/(?:embed\/|v=|youtu\.be\/|live\/)([A-Za-z0-9_-]{11})/)?.[1]
      : null)
  if (!id) return ""
  const params = new URLSearchParams({
    autoplay: "0",
    rel: "0",
    modestbranding: "1",
    playsinline: "1",
  })
  return `https://www.youtube-nocookie.com/embed/${id}?${params.toString()}`
}

export const YOUTUBE_EMBED_ALLOW =
  "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen; compute-pressure"

export function liveVodWatchHref(
  item: Pick<LiveVodItem, "videoId">,
  tab?: string,
): string {
  const params = new URLSearchParams()
  if (tab && tab !== "ALL") params.set("tab", tab)
  const qs = params.toString()
  return `/live-vod/watch/${encodeURIComponent(item.videoId)}${qs ? `?${qs}` : ""}`
}

const META_HINT_KEY = "finsight.liveVod.metaHint.v1"
const PLACEHOLDER_TITLE = "VOD 상세"

export type LiveVodMetaHint = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
}

export function stashLiveVodMetaHint(
  item: Pick<LiveVodItem, "videoId" | "title" | "channelTitle" | "thumbnailUrl">,
): void {
  if (typeof window === "undefined" || !item.videoId) return
  const title = (item.title || "").trim()
  if (!title || title === PLACEHOLDER_TITLE) return
  try {
    const payload: LiveVodMetaHint = {
      videoId: item.videoId,
      title,
      channelTitle: item.channelTitle ?? null,
      thumbnailUrl:
        item.thumbnailUrl || `https://i.ytimg.com/vi/${item.videoId}/hqdefault.jpg`,
    }
    window.sessionStorage.setItem(META_HINT_KEY, JSON.stringify(payload))
  } catch {
    void 0
  }
}

export function readLiveVodMetaHint(videoId: string): LiveVodMetaHint | null {
  if (typeof window === "undefined" || !videoId) return null
  try {
    const raw = window.sessionStorage.getItem(META_HINT_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as LiveVodMetaHint
    if (!parsed || parsed.videoId !== videoId) return null
    const title = (parsed.title || "").trim()
    if (!title || title === PLACEHOLDER_TITLE) return null
    return {
      videoId,
      title,
      channelTitle: parsed.channelTitle ?? null,
      thumbnailUrl:
        parsed.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    }
  } catch {
    return null
  }
}

export type LiveVodMeta = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  embedUrl: string
  watchUrl: string
}

export async function fetchLiveVodMeta(videoId: string): Promise<LiveVodMeta> {
  const hint = readLiveVodMetaHint(videoId)
  const fallback: LiveVodMeta = {
    videoId,
    title: hint?.title || PLACEHOLDER_TITLE,
    channelTitle: hint?.channelTitle ?? null,
    thumbnailUrl: hint?.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    embedUrl: `https://www.youtube-nocookie.com/embed/${videoId}`,
    watchUrl: `https://www.youtube.com/watch?v=${videoId}`,
  }
  try {
    const res = await fetch(`/api/v1/media/live-vod/${encodeURIComponent(videoId)}/meta`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    })
    if (!res.ok) return fallback
    const json = (await res.json()) as { success?: boolean; data?: Record<string, unknown> }
    const data = json.data
    if (!data || typeof data !== "object") return fallback
    const title =
      typeof data.title === "string" && data.title.trim() && data.title.trim() !== PLACEHOLDER_TITLE
        ? data.title
        : fallback.title
    return {
      videoId: typeof data.videoId === "string" ? data.videoId : videoId,
      title,
      channelTitle:
        typeof data.channelTitle === "string"
          ? data.channelTitle
          : fallback.channelTitle,
      thumbnailUrl:
        typeof data.thumbnailUrl === "string" && data.thumbnailUrl
          ? data.thumbnailUrl
          : fallback.thumbnailUrl,
      embedUrl:
        typeof data.embedUrl === "string" && data.embedUrl ? data.embedUrl : fallback.embedUrl,
      watchUrl:
        typeof data.watchUrl === "string" && data.watchUrl ? data.watchUrl : fallback.watchUrl,
    }
  } catch {
    return fallback
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function parseItem(raw: unknown): LiveVodItem | null {
  const o = asRecord(raw)
  if (!o || typeof o.videoId !== "string" || !o.videoId) return null
  return {
    videoId: o.videoId,
    title: typeof o.title === "string" ? o.title : "",
    thumbnailUrl:
      typeof o.thumbnailUrl === "string" && o.thumbnailUrl
        ? o.thumbnailUrl
        : `https://i.ytimg.com/vi/${o.videoId}/mqdefault.jpg`,
    watchUrl:
      typeof o.watchUrl === "string" && o.watchUrl
        ? o.watchUrl
        : `https://www.youtube.com/watch?v=${o.videoId}`,
    embedUrl:
      typeof o.embedUrl === "string" && o.embedUrl
        ? o.embedUrl.replace(
            "https://www.youtube.com/embed/",
            "https://www.youtube-nocookie.com/embed/",
          )
        : `https://www.youtube-nocookie.com/embed/${o.videoId}`,
    channelTitle: typeof o.channelTitle === "string" ? o.channelTitle : null,
    favoriteCount: typeof o.favoriteCount === "number" ? o.favoriteCount : Number(o.favoriteCount) || 0,
    commentCount: typeof o.commentCount === "number" ? o.commentCount : Number(o.commentCount) || 0,
  }
}

function parseSection(raw: unknown): LiveVodSection | null {
  const o = asRecord(raw)
  if (!o) return null
  const itemsRaw = Array.isArray(o.items) ? o.items : []
  return {
    heading: typeof o.heading === "string" ? o.heading : "VOD",
    items: itemsRaw.map(parseItem).filter((v): v is LiveVodItem => v !== null),
  }
}

export async function fetchLiveVodFeed(
  tab: string,
): Promise<{ ok: true; data: LiveVodFeed } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  if (tab && tab !== "ALL") params.set("tab", tab)
  const qs = params.toString()

  let res: Response
  try {
    res = await fetch(`/api/v1/media/videos/live-vod${qs ? `?${qs}` : ""}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    })
  } catch {
    return {
      ok: false,
      message: "LIVE/VOD 서버에 연결하지 못했습니다. 백엔드 기동 여부를 확인해 주세요.",
    }
  }

  let payload: unknown = null
  try {
    payload = await res.json()
  } catch {
    payload = null
  }
  if (!res.ok) {
    const root = asRecord(payload)
    const message =
      typeof root?.message === "string" && root.message
        ? root.message
        : res.status === 503 || res.status === 504
          ? "백엔드가 준비되지 않았거나 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
          : "LIVE/VOD 피드를 불러오지 못했습니다."
    return { ok: false, message }
  }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  if (!data) {
    return { ok: false, message: "LIVE/VOD 응답 형식이 올바르지 않습니다." }
  }
  const sectionsRaw = Array.isArray(data.sections) ? data.sections : []
  return {
    ok: true,
    data: {
      title: typeof data.title === "string" ? data.title : "finsight LIVE",
      tab: typeof data.tab === "string" ? data.tab : tab || "ALL",
      featuredVideoId:
        typeof data.featuredVideoId === "string" ? data.featuredVideoId : null,
      featuredTitle:
        typeof data.featuredTitle === "string" ? data.featuredTitle : null,
      featuredThumbnailUrl:
        typeof data.featuredThumbnailUrl === "string"
          ? data.featuredThumbnailUrl
          : null,
      embedUrl:
        typeof data.featuredVideoId === "string"
          ? toPrivacyEmbedUrl(data.featuredVideoId)
          : null,
      sections: sectionsRaw
        .map(parseSection)
        .filter((v): v is LiveVodSection => v !== null),
    },
  }
}
