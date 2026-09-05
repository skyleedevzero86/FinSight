import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(
    req,
    `/api/v1/admin/comments/maintenance/hide-over-reported${q ? `?${q}` : ""}`,
  )
}
