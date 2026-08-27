import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ videoId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`,
  )
}

export async function POST(req: Request, ctx: Ctx) {
  const { videoId } = await ctx.params
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/${encodeURIComponent(videoId)}/comments`,
  )
}
