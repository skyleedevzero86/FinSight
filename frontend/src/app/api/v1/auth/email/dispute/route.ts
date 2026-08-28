import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  const url = new URL(req.url)
  return mirrorRequestToFinSight(req, `/api/v1/auth/email/dispute${url.search}`)
}
