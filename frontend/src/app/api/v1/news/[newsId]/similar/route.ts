import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request, context: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await context.params
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(req, `/api/v1/news/${newsId}/similar${q ? `?${q}` : ""}`)
}
