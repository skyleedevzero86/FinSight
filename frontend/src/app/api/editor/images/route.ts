import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/editor/images${qs ? `?${qs}` : ""}`,
  )
}
