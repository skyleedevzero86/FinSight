export function getFinSightBaseUrl(): string | null {
  const base = process.env.FINSIGHT_API_BASE_URL?.replace(/\/$/, "")
  return base || null
}

export function finSightUnavailableResponse() {
  return new Response(
    JSON.stringify({
      message:
        "FINSIGHT_API_BASE_URL가 설정되지 않았습니다. .env.local에 백엔드 주소를 넣어 주세요.",
    }),
    { status: 503, headers: { "Content-Type": "application/json" } },
  )
}

export async function proxyJsonToFinSight(
  req: Request,
  backendPath: string,
): Promise<Response> {
  const base = getFinSightBaseUrl()
  if (!base) return finSightUnavailableResponse()

  let body: string
  try {
    const j = await req.json()
    body = JSON.stringify(j)
  } catch {
    return new Response(JSON.stringify({ message: "잘못된 JSON 요청입니다." }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    })
  }

  const target = `${base}${backendPath.startsWith("/") ? "" : "/"}${backendPath}`
  const upstream = await fetch(target, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body,
  })

  const text = await upstream.text()
  const ct = upstream.headers.get("content-type") ?? "application/json"
  return new Response(text, {
    status: upstream.status,
    headers: { "Content-Type": ct },
  })
}
