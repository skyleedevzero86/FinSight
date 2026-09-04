import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(req, `/api/v1/popup/items${q ? `?${q}` : ""}`)
}

export async function POST(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/popup/items")
}
