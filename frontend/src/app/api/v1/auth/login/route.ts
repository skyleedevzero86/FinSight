import { proxyJsonToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return proxyJsonToFinSight(req, "/api/v1/auth/login", { timeoutMs: 90_000 })
}
