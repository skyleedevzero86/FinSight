import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 20_000

/**
 * Retrieves engagement data for a live or VOD media item.
 *
 * @param ctx - Request context containing the media item's `videoId`
 * @returns The proxied engagement response
 */
export async function GET(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/engagement`,
    { timeoutMs: MEDIA_PROXY_TIMEOUT_MS },
  )
}
