import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const u = new URL(req.url)
  const q = u.searchParams.toString()
  return mirrorRequestToFinSight(req, `/api/v1/boards${q ? `?${q}` : ""}`)
}

export async function POST(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/boards")
}
