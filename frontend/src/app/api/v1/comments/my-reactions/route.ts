import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const qs = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/comments/my-reactions${qs ? `?${qs}` : ""}`,
  )
}
