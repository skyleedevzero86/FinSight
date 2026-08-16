import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function GET(req: Request) {
  const url = new URL(req.url)
  return mirrorRequestToFinSight(req, `/api/v1/auth/email/challenge${url.search}`)
}
