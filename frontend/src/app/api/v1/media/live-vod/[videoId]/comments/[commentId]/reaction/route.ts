import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string; commentId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 90_000

/**
 * Proxies a request to react to a live VOD video comment.
 *
 * @param req - The incoming reaction request
 * @param ctx - The route context containing the video and comment identifiers
 * @returns The proxied FinSight response
 */
export async function POST(req: Request, ctx: Ctx) {
  const { videoId, commentId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments/${encodeURIComponent(commentId)}/reaction`,
    { timeoutMs: MEDIA_PROXY_TIMEOUT_MS },
  )
}
