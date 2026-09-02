import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 90_000

/**
 * Proxies a request for a video's live or VOD comments while preserving its query parameters.
 *
 * @param ctx - Route context containing the video identifier
 * @returns The proxied FinSight response
 */
export async function GET(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  const path =
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments` + (qs ? `?${qs}` : "")
  return mirrorRequestToFinSight(req, path, { timeoutMs: MEDIA_PROXY_TIMEOUT_MS })
}

export async function POST(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`,
    { timeoutMs: MEDIA_PROXY_TIMEOUT_MS },
  )
}
