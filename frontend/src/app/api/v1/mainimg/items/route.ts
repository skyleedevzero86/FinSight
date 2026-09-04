import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

function isPublicCmsGet(req: Request): boolean {
  if (req.method !== "GET" && req.method !== "HEAD") return false
  const auth = req.headers.get("authorization") ?? req.headers.get("Authorization")
  return !auth
}

export async function GET(req: Request) {
  const q = new URL(req.url).searchParams.toString()
  return mirrorRequestToFinSight(req, `/api/v1/mainimg/items${q ? `?${q}` : ""}`, {
    forwardCredentials: !isPublicCmsGet(req),
  })
}

export async function POST(req: Request) {
  return mirrorRequestToFinSight(req, "/api/v1/mainimg/items")
}
