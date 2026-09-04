import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(req, `/api/v1/inbox${q ? `?${q}` : ""}`)
}

export async function DELETE(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/inbox")
}
