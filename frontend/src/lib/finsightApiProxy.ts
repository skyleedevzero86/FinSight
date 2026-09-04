const DEFAULT_PROXY_TIMEOUT_MS = 30_000
const DEFAULT_API_BASE_URL = "http://localhost:8080"

export function getFinSightBaseUrl(): string | null {
  const base = process.env.FINSIGHT_API_BASE_URL?.replace(/\/$/, "")
  if (base) return base
  if (process.env.NODE_ENV === "development") return DEFAULT_API_BASE_URL
  return null
}

function jsonResponse(status: number, message: string) {
  return new Response(JSON.stringify({ message }), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

function copyUpstreamHeaders(upstream: Response): Headers {
  const headers = new Headers()
  const contentType = upstream.headers.get("content-type") ?? "application/json"
  headers.set("Content-Type", contentType)

  const cacheControl = upstream.headers.get("cache-control")
  if (cacheControl) headers.set("Cache-Control", cacheControl)

  const location = upstream.headers.get("location")
  if (location) headers.set("Location", location)

  const setCookieAccessor = (
    upstream.headers as Headers & { getSetCookie?: () => string[] }
  ).getSetCookie
  if (typeof setCookieAccessor === "function") {
    const setCookies = setCookieAccessor.call(upstream.headers)
    for (const cookie of setCookies) headers.append("Set-Cookie", cookie)
  } else {
    const setCookie = upstream.headers.get("set-cookie")
    if (setCookie) headers.append("Set-Cookie", setCookie)
  }

  return headers
}

export function finSightUnavailableResponse() {
  return jsonResponse(
    503,
    "백엔드 주소가 설정되지 않았습니다. 환경 설정을 확인해 주세요.",
  )
}

function getProxyTimeoutMs(): number {
  const raw = process.env.FINSIGHT_API_PROXY_TIMEOUT_MS
  if (raw === undefined || raw === "") return DEFAULT_PROXY_TIMEOUT_MS
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? Math.min(n, 120_000) : DEFAULT_PROXY_TIMEOUT_MS
}

function isAbortError(err: unknown): boolean {
  if (!(err instanceof Error)) return false
  if (err.name === "AbortError") return true
  return err.message.includes("aborted") || err.message.includes("AbortError")
}

function upstreamFailureResponse(err: unknown, aborted: boolean) {
  if (aborted) {
    return jsonResponse(
      504,
      "백엔드 응답이 너무 느려 요청이 중단되었습니다. 잠시 후 다시 시도해 주세요.",
    )
  }
  const message = err instanceof Error ? err.message : String(err ?? "")
  const connectionClosed =
    /ECONNRESET|ECONNREFUSED|ERR_CONNECTION_CLOSED|socket hang up|fetch failed/i.test(
      message,
    )
  if (process.env.NODE_ENV === "development") {
    console.error("백엔드 서버 연결에 실패했습니다.", err)
  }
  return jsonResponse(
    connectionClosed ? 503 : 503,
    connectionClosed
      ? "백엔드 연결이 끊겼습니다. 서버를 재시작한 뒤 새로고침해 주세요."
      : "백엔드 서버에 연결할 수 없습니다. 서버 상태를 확인한 뒤 잠시 후 다시 시도해 주세요.",
  )
}

function clientForwardHeaders(req: Request): Record<string, string> {
  const headers: Record<string, string> = {}
  const forwarded = req.headers.get("x-forwarded-for")
  const realIp = req.headers.get("x-real-ip")
  if (forwarded) headers["X-Forwarded-For"] = forwarded
  else if (realIp) headers["X-Forwarded-For"] = realIp
  if (realIp) headers["X-Real-IP"] = realIp
  return headers
}

export async function proxyJsonToFinSight(
  req: Request,
  backendPath: string,
  options?: { timeoutMs?: number; forwardCredentials?: boolean },
): Promise<Response> {
  try {
    const base = getFinSightBaseUrl()
    if (!base) return finSightUnavailableResponse()

    let body: string
    try {
      const j = await req.json()
      body = JSON.stringify(j)
    } catch {
      return jsonResponse(400, "잘못된 JSON 요청입니다.")
    }

    const target = `${base}${backendPath.startsWith("/") ? "" : "/"}${backendPath}`
    const controller = new AbortController()
    const timeoutMs = options?.timeoutMs ?? getProxyTimeoutMs()
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
    const forwardCredentials = options?.forwardCredentials !== false
    const authHeader = req.headers.get("authorization")
    const cookieHeader = req.headers.get("cookie")

    let upstream: Response
    try {
      upstream = await fetch(target, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          ...clientForwardHeaders(req),
          ...(forwardCredentials && authHeader ? { Authorization: authHeader } : {}),
          ...(forwardCredentials && cookieHeader ? { Cookie: cookieHeader } : {}),
        },
        body,
        signal: controller.signal,
      })
    } catch (err) {
      const aborted = isAbortError(err)
      return upstreamFailureResponse(err, aborted)
    } finally {
      clearTimeout(timeoutId)
    }

    let text: string
    try {
      text = await upstream.text()
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.error("백엔드 응답 본문을 읽는 중 오류가 발생했습니다.", err)
      }
      return jsonResponse(
        503,
        "백엔드 응답을 받는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
      )
    }

    return new Response(text, {
      status: upstream.status,
      headers: copyUpstreamHeaders(upstream),
    })
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.error("백엔드 요청 처리 중 오류가 발생했습니다.", err)
    }
    return jsonResponse(
      500,
      "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
    )
  }
}

export async function mirrorRequestToFinSight(
  req: Request,
  backendPathAndQuery: string,
  init?: {
    body?: BodyInit | null
    timeoutMs?: number
    /** false면 Authorization/Cookie를 백엔드로 넘기지 않음(공개 GET용) */
    forwardCredentials?: boolean
  },
): Promise<Response> {
  try {
    const base = getFinSightBaseUrl()
    if (!base) return finSightUnavailableResponse()

    const method = req.method
    const forwardCredentials = init?.forwardCredentials !== false
    const headers: Record<string, string> = {
      Accept: req.headers.get("accept") ?? "application/json",
      Connection: "close",
      ...clientForwardHeaders(req),
    }
    if (forwardCredentials) {
      const auth = req.headers.get("authorization") ?? req.headers.get("Authorization")
      if (auth) headers.Authorization = auth
      const cookie = req.headers.get("cookie") ?? req.headers.get("Cookie")
      if (cookie) headers.Cookie = cookie
    }
    const contentType = req.headers.get("content-type") ?? req.headers.get("Content-Type")
    if (
      contentType &&
      method !== "GET" &&
      method !== "HEAD" &&
      method !== "DELETE"
    ) {
      headers["Content-Type"] = contentType
    }

    const target = `${base}${backendPathAndQuery.startsWith("/") ? "" : "/"}${backendPathAndQuery}`
    const controller = new AbortController()
    const timeoutMs = init?.timeoutMs ?? getProxyTimeoutMs()
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

    let upstream: Response
    try {
      let body: BodyInit | undefined
      if (init?.body !== undefined) {
        body = init.body ?? undefined
      } else if (method !== "GET" && method !== "HEAD") {
        const contentTypeHeader = contentType?.toLowerCase() ?? ""
        if (contentTypeHeader.includes("multipart/form-data")) {
          body = await req.arrayBuffer()
        } else {
          const text = await req.text()
          body = text === "" ? undefined : text
        }
      }
      upstream = await fetch(target, {
        method,
        headers,
        body,
        signal: controller.signal,
        cache: "no-store",
      })
    } catch (err) {
      const aborted = isAbortError(err)
      return upstreamFailureResponse(err, aborted)
    } finally {
      clearTimeout(timeoutId)
    }

    let text: string
    try {
      text = await upstream.text()
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.error("백엔드 응답 본문을 읽는 중 오류가 발생했습니다.", err)
      }
      return jsonResponse(
        503,
        "백엔드 응답을 받는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
      )
    }

    return new Response(text, {
      status: upstream.status,
      headers: copyUpstreamHeaders(upstream),
    })
  } catch (err) {
    if (isAbortError(err)) {
      return upstreamFailureResponse(err, true)
    }
    if (process.env.NODE_ENV === "development") {
      console.error("백엔드 요청 처리 중 오류가 발생했습니다.", err)
    }
    return jsonResponse(
      503,
      "백엔드 서버에 연결할 수 없습니다. 서버 상태를 확인한 뒤 잠시 후 다시 시도해 주세요.",
    )
  }
}

export async function mirrorBinaryRequestToFinSight(
  req: Request,
  backendPathAndQuery: string,
): Promise<Response> {
  try {
    const base = getFinSightBaseUrl()
    if (!base) return finSightUnavailableResponse()

    const method = req.method
    const headers: Record<string, string> = {
      Accept: req.headers.get("accept") ?? "*/*",
      ...clientForwardHeaders(req),
    }
    const auth = req.headers.get("authorization") ?? req.headers.get("Authorization")
    if (auth) headers.Authorization = auth
    const cookie = req.headers.get("cookie") ?? req.headers.get("Cookie")
    if (cookie) headers.Cookie = cookie
    const contentType = req.headers.get("content-type") ?? req.headers.get("Content-Type")
    if (
      contentType &&
      method !== "GET" &&
      method !== "HEAD" &&
      method !== "DELETE"
    ) {
      headers["Content-Type"] = contentType
    }

    const target = `${base}${backendPathAndQuery.startsWith("/") ? "" : "/"}${backendPathAndQuery}`
    const controller = new AbortController()
    const timeoutMs = Math.max(getProxyTimeoutMs(), 45_000)
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

    let upstream: Response
    try {
      const body =
        method !== "GET" && method !== "HEAD"
          ? await req.arrayBuffer()
          : undefined
      upstream = await fetch(target, {
        method,
        headers,
        body: body && body.byteLength > 0 ? body : undefined,
        signal: controller.signal,
      })
    } catch (err) {
      const aborted = isAbortError(err)
      return upstreamFailureResponse(err, aborted)
    } finally {
      clearTimeout(timeoutId)
    }

    let buf: ArrayBuffer
    try {
      buf = await upstream.arrayBuffer()
    } catch (err) {
      if (process.env.NODE_ENV === "development") {
        console.error("백엔드 응답 본문을 읽는 중 오류가 발생했습니다.", err)
      }
      return jsonResponse(
        503,
        "백엔드 응답을 받는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
      )
    }

    return new Response(buf, {
      status: upstream.status,
      headers: copyUpstreamHeaders(upstream),
    })
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.error("백엔드 요청 처리 중 오류가 발생했습니다.", err)
    }
    return jsonResponse(
      500,
      "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
    )
  }
}

