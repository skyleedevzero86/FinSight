import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/users/password/status")
}
