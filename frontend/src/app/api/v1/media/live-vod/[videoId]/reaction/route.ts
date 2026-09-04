import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

const MEDIA_PROXY_TIMEOUT_MS = 90_000

export async function POST(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/reaction`,
    { timeoutMs: MEDIA_PROXY_TIMEOUT_MS },
  )
}
