import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/users/withdraw")
}
