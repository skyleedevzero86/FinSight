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
    "FINSIGHT_API_BASE_URL가 설정되지 않았습니다. .env.local에 백엔드 주소를 넣어 주세요.",
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
  if (process.env.NODE_ENV === "development") {
    console.error("백엔드 서버 연결에 실패했습니다.", err)
  }
  return jsonResponse(
    503,
    "백엔드 서버에 연결할 수 없습니다. 서버 상태를 확인한 뒤 잠시 후 다시 시도해 주세요.",
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
  options?: { timeoutMs?: number },
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

    let upstream: Response
    try {
      upstream = await fetch(target, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          ...clientForwardHeaders(req),
          ...(req.headers.get("authorization")
            ? { Authorization: req.headers.get("authorization") as string }
            : {}),
          ...(req.headers.get("cookie")
            ? { Cookie: req.headers.get("cookie") as string }
            : {}),
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
  init?: { body?: BodyInit | null },
): Promise<Response> {
  try {
    const base = getFinSightBaseUrl()
    if (!base) return finSightUnavailableResponse()

    const method = req.method
    const headers: Record<string, string> = {
      Accept: req.headers.get("accept") ?? "application/json",
      ...clientForwardHeaders(req),
    }
    const auth = req.headers.get("authorization")
    if (auth) headers.Authorization = auth
    const cookie = req.headers.get("cookie")
    if (cookie) headers.Cookie = cookie
    const contentType = req.headers.get("content-type")
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
    const timeoutMs = getProxyTimeoutMs()
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

    let upstream: Response
    try {
      const body =
        init?.body !== undefined
          ? init.body
          : method !== "GET" && method !== "HEAD"
            ? await req.text()
            : undefined
      upstream = await fetch(target, {
        method,
        headers,
        body: body === "" ? undefined : body,
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
    const auth = req.headers.get("authorization")
    if (auth) headers.Authorization = auth
    const cookie = req.headers.get("cookie")
    if (cookie) headers.Cookie = cookie
    const contentType = req.headers.get("content-type")
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

