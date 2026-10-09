import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(
  req: Request,
  ctx: { params: Promise<{ newsId: string }> },
) {
  const { newsId } = await ctx.params
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/comments/news/${encodeURIComponent(newsId)}${qs ? `?${qs}` : ""}`,
  )
}
