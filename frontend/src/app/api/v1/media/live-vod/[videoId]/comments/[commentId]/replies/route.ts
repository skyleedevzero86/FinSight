import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string; commentId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 90_000

/**
 * Proxies requests for replies to a comment on live or VOD media.
 *
 * @returns The proxied FinSight response.
 */
export async function GET(req: Request, ctx: Ctx) {
  const { videoId, commentId } = await ctx.params
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  const path =
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments/${encodeURIComponent(commentId)}/replies` +
    (qs ? `?${qs}` : "")
  return mirrorRequestToFinSight(req, path, { timeoutMs: MEDIA_PROXY_TIMEOUT_MS })
}
