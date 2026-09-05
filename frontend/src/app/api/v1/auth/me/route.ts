import { mirrorRequestToFinSight } from "@/lib/finsightApiProxy"

function sessionProbeResponse(options?: {
  unavailable?: boolean
  sessionInvalid?: boolean
}) {
  return Response.json(
    {
      success: true,
      data: null,
      message: options?.unavailable
        ? "인증 서버에 잠시 연결할 수 없습니다"
        : options?.sessionInvalid
          ? "세션이 만료되었습니다"
          : "로그인이 필요합니다",
      ...(options?.unavailable ? { unavailable: true } : {}),
      ...(options?.sessionInvalid ? { sessionInvalid: true } : {}),
    },
    {
      status: 200,
      headers: { "Cache-Control": "no-store" },
    },
  )
}

export async function GET(req: Request) {
  const auth = req.headers.get("authorization")
  if (!auth || !auth.toLowerCase().startsWith("bearer ")) {
    return sessionProbeResponse()
  }

  const upstream = await mirrorRequestToFinSight(req, "/api/v1/auth/me")
  if (upstream.status === 401 || upstream.status === 403) {
    return sessionProbeResponse({ sessionInvalid: true })
  }
  if (upstream.status === 502 || upstream.status === 503 || upstream.status === 504) {
    return sessionProbeResponse({ unavailable: true })
  }
  return upstream
}
