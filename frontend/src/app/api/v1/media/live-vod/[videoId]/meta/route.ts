import { getFinSightBaseUrl } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

type MetaPayload = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  embedUrl: string
  watchUrl: string
}

const PLACEHOLDER_TITLE = "VOD 상세"

function fallbackMeta(videoId: string): MetaPayload {
  return {
    videoId,
    title: PLACEHOLDER_TITLE,
    channelTitle: null,
    thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    embedUrl: `https://www.youtube-nocookie.com/embed/${videoId}`,
    watchUrl: `https://www.youtube.com/watch?v=${videoId}`,
  }
}

function isUsableTitle(title: string | null | undefined): boolean {
  const trimmed = (title || "").trim()
  return trimmed.length > 0 && trimmed !== PLACEHOLDER_TITLE
}

function okMeta(data: MetaPayload) {
  return Response.json(
    {
      success: true,
      data,
      message: "영상 정보를 조회했습니다.",
    },
    { status: 200, headers: { "Content-Type": "application/json" } },
  )
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

async function fetchOEmbed(videoId: string): Promise<MetaPayload | null> {
  try {
    const watchUrl = `https://www.youtube.com/watch?v=${videoId}`
    const url = `https://www.youtube.com/oembed?format=json&url=${encodeURIComponent(watchUrl)}`
    const res = await fetch(url, {
      headers: { Accept: "application/json" },
      cache: "force-cache",
      next: { revalidate: 3600 },
    })
    if (!res.ok) return null
    const data = asRecord(await res.json())
    if (!data) return null
    const title = typeof data.title === "string" && data.title ? data.title : PLACEHOLDER_TITLE
    const channelTitle =
      typeof data.author_name === "string" && data.author_name ? data.author_name : null
    const thumbnailUrl =
      typeof data.thumbnail_url === "string" && data.thumbnail_url
        ? data.thumbnail_url
        : `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
    return {
      videoId,
      title,
      channelTitle,
      thumbnailUrl,
      embedUrl: `https://www.youtube-nocookie.com/embed/${videoId}`,
      watchUrl,
    }
  } catch {
    return null
  }
}

function parseBackendMeta(videoId: string, payload: unknown): MetaPayload | null {
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  if (!data) return null
  const id = typeof data.videoId === "string" && data.videoId ? data.videoId : videoId
  const title = typeof data.title === "string" && data.title ? data.title : ""
  if (!isUsableTitle(title)) return null
  return {
    videoId: id,
    title,
    channelTitle: typeof data.channelTitle === "string" ? data.channelTitle : null,
    thumbnailUrl:
      typeof data.thumbnailUrl === "string" && data.thumbnailUrl
        ? data.thumbnailUrl
        : `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
    embedUrl:
      typeof data.embedUrl === "string" && data.embedUrl
        ? data.embedUrl
        : `https://www.youtube-nocookie.com/embed/${id}`,
    watchUrl:
      typeof data.watchUrl === "string" && data.watchUrl
        ? data.watchUrl
        : `https://www.youtube.com/watch?v=${id}`,
  }
}

export async function GET(_req: Request, ctx: Ctx) {
  const { videoId: rawId } = await ctx.params
  const videoId = (rawId || "").trim()
  if (!videoId || !/^[A-Za-z0-9_-]{6,32}$/.test(videoId)) {
    return okMeta(fallbackMeta(videoId || "unknown"))
  }

  const oembed = await fetchOEmbed(videoId)
  if (oembed && isUsableTitle(oembed.title)) {
    return okMeta(oembed)
  }

  const base = getFinSightBaseUrl()
  if (base) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 8_000)
      try {
        const upstream = await fetch(
          `${base}/api/v1/media/live-vod/${encodeURIComponent(videoId)}/meta`,
          {
            headers: { Accept: "application/json" },
            signal: controller.signal,
            cache: "no-store",
          },
        )
        if (upstream.ok) {
          const payload = await upstream.json().catch(() => null)
          const parsed = parseBackendMeta(videoId, payload)
          if (parsed) return okMeta(parsed)
        }
      } finally {
        clearTimeout(timeoutId)
      }
    } catch {
      void 0
    }
  }

  if (oembed) return okMeta(oembed)
  return okMeta(fallbackMeta(videoId))
}
