import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 90_000

/**
 * Forwards a media reaction request for the specified video to FinSight.
 *
 * @param ctx - Route context containing the video identifier.
 * @returns The response from FinSight.
 */
export async function POST(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/reaction`,
    { timeoutMs: MEDIA_PROXY_TIMEOUT_MS },
  )
}
