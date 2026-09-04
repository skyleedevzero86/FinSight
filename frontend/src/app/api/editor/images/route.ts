import { getFinSightBaseUrl, finSightUnavailableResponse } from "@/lib/finsightApiProxy"

export async function POST(req: Request) {
  const base = getFinSightBaseUrl()
  if (!base) return finSightUnavailableResponse()

  const url = new URL(req.url)
  const qs = url.searchParams.toString()
  const target = `${base}/api/editor/images${qs ? `?${qs}` : ""}`

  try {
    const incoming = await req.formData()
    const forward = new FormData()
    for (const [key, value] of incoming.entries()) {
      forward.append(key, value)
    }

    const headers: Record<string, string> = {
      Accept: "application/json",
      Connection: "close",
    }
    const auth = req.headers.get("authorization") ?? req.headers.get("Authorization")
    if (auth) headers.Authorization = auth

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 90_000)
    let upstream: Response
    try {
      upstream = await fetch(target, {
        method: "POST",
        headers,
        body: forward,
        signal: controller.signal,
        cache: "no-store",
      })
    } finally {
      clearTimeout(timeoutId)
    }

    const contentType = upstream.headers.get("content-type") ?? "application/json"
    const body = await upstream.arrayBuffer()
    return new Response(body, {
      status: upstream.status,
      headers: {
        "Content-Type": contentType,
        "Cache-Control": "no-store",
      },
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err ?? "")
    const aborted = /abort/i.test(message)
    return Response.json(
      {
        message: aborted
          ? "이미지 업로드 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
          : "이미지 업로드 중 서버 연결이 끊겼습니다. 프론트/백엔드를 확인한 뒤 다시 시도해 주세요.",
      },
      { status: aborted ? 504 : 503 },
    )
  }
}
