import { proxyAuthLoginToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return proxyAuthLoginToFinSight(req, "/api/v1/auth/login", {
    timeoutMs: 90_000,
  })
}
