import { proxyAuthLoginToFinSight } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  return proxyAuthLoginToFinSight(req, "/api/v1/auth/refresh", {
    timeoutMs: 30_000,
    forwardCredentials: true,
  })
}
