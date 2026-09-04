import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/inbox/settings")
}

export async function PUT(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/inbox/settings")
}
