import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

type Ctx = { params: Promise<{ newsId: string }> }

export async function GET(req: Request, ctx: Ctx) {
  const { newsId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/news/${encodeURIComponent(newsId)}/reactions`)
}

export async function POST(req: Request, ctx: Ctx) {
  const { newsId } = await ctx.params
  return mirrorRequestToFinSight(req, `/api/v1/news/${encodeURIComponent(newsId)}/reactions`)
}
