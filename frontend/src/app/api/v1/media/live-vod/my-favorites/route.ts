import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const url = new URL(req.url)
  const q = url.searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/media/live-vod/my-favorites${q ? `?${q}` : ""}`,
    { timeoutMs: 20_000 },
  )
}
