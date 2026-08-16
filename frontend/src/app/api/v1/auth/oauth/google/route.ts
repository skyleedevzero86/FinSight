import { proxyJsonToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return proxyJsonToFinSight(req, "/api/v1/auth/oauth/google")
}
