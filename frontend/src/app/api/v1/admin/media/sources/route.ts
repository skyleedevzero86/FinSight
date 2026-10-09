import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/admin/media/sources")
}

export async function POST(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/admin/media/sources")
}
