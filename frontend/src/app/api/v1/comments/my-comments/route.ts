import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/comments/my-comments${qs ? `?${qs}` : ""}`,
  )
}
