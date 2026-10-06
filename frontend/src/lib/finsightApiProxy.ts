import { NextResponse } from "next/server"

const DEFAULT_PROXY_TIMEOUT_MS = 45_000
const DEFAULT_API_BASE_URL = "http://localhost:8080"
const ACCESS_COOKIE_MAX_AGE_SEC = 60 * 60
const REFRESH_COOKIE_MAX_AGE_SEC = 60 * 60 * 24 * 30

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

function readCookieValue(cookieHeader: string, name: string): string | null {
  const parts = cookieHeader.split(";")
  for (const part of parts) {
    const trimmed = part.trim()
    if (!trimmed.startsWith(`${name}=`)) continue
    const raw = trimmed.slice(name.length + 1)
    if (!raw) return null
    try {
      return decodeURIComponent(raw)
    } catch {
      return raw
    }
  }
  return null
}

function applyCredentialHeaders(
  req: Request,
  headers: Record<string, string>,
  forwardCredentials: boolean,
): void {
  if (!forwardCredentials) return

  const auth = req.headers.get("authorization") ?? req.headers.get("Authorization")
  const cookie = req.headers.get("cookie") ?? req.headers.get("Cookie")
  if (cookie) headers.Cookie = cookie

  if (auth) {
    headers.Authorization = auth
    return
  }
  if (!cookie) return

  const accessToken = readCookieValue(cookie, "accessToken")
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function extractLoginTokens(payload: unknown): {
  accessToken: string | null
  refreshToken: string | null
} {
  const root = asRecord(payload)
  if (root && root.success === false) {
    return { accessToken: null, refreshToken: null }
  }
  const data = asRecord(root?.data) ?? root
  const token = asRecord(data?.token) ?? data
  const accessToken =
    typeof token?.accessToken === "string" && token.accessToken.trim()
      ? token.accessToken.trim()
      : null
  const refreshToken =
    typeof token?.refreshToken === "string" && token.refreshToken.trim()
      ? token.refreshToken.trim()
      : null
  return { accessToken, refreshToken }
}

function stripLoginTokens(payload: unknown): unknown {
  const root = asRecord(payload)
  if (!root) return payload
  const data = asRecord(root.data)
  if (!data) return payload
  const token = asRecord(data.token)
  if (token) {
    return {
      ...root,
      data: {
        ...data,
        token: {
          ...token,
          accessToken: null,
          refreshToken: null,
        },
      },
    }
  }
  if ("accessToken" in data || "refreshToken" in data) {
    return {
      ...root,
      data: {
        ...data,
        accessToken: null,
        refreshToken: null,
      },
    }
  }
  return payload
}

function extractTokensFromSetCookie(upstream: Response): {
  accessToken: string | null
  refreshToken: string | null
} {
  const cookies: string[] = []
  const setCookieAccessor = (
    upstream.headers as Headers & { getSetCookie?: () => string[] }
  ).getSetCookie
  if (typeof setCookieAccessor === "function") {
    cookies.push(...setCookieAccessor.call(upstream.headers))
  } else {
    const single = upstream.headers.get("set-cookie")
    if (single) cookies.push(single)
  }

  let accessToken: string | null = null
  let refreshToken: string | null = null
  for (const raw of cookies) {
    const first = raw.split(";")[0] ?? ""
    const eq = first.indexOf("=")
    if (eq < 0) continue
    const name = first.slice(0, eq).trim()
    let value = first.slice(eq + 1).trim()
    if (value.startsWith('"') && value.endsWith('"') && value.length >= 2) {
      value = value.slice(1, -1)
    }
    try {
      value = decodeURIComponent(value)
    } catch {
      void 0
    }
    if (!value) continue
    if (name === "accessToken") accessToken = value
    if (name === "refreshToken") refreshToken = value
  }
  return { accessToken, refreshToken }
}

function mergeTokens(
  fromBody: { accessToken: string | null; refreshToken: string | null },
  fromCookie: { accessToken: string | null; refreshToken: string | null },
) {
  return {
    accessToken: fromBody.accessToken ?? fromCookie.accessToken,
    refreshToken: fromBody.refreshToken ?? fromCookie.refreshToken,
  }
}

function applyAuthCookies(
  response: NextResponse,
  tokens: { accessToken: string | null; refreshToken: string | null },
) {
  if (tokens.accessToken) {
    response.cookies.set("accessToken", tokens.accessToken, {
      httpOnly: true,
      path: "/",
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      maxAge: ACCESS_COOKIE_MAX_AGE_SEC,
    })
    response.cookies.set("finsight_auth", "1", {
      httpOnly: false,
      path: "/",
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      maxAge: ACCESS_COOKIE_MAX_AGE_SEC,
    })
  }
  if (tokens.refreshToken) {
    response.cookies.set("refreshToken", tokens.refreshToken, {
      httpOnly: true,
      path: "/",
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      maxAge: REFRESH_COOKIE_MAX_AGE_SEC,
    })
  }
}

export async function proxyAuthLoginToFinSight(
  req: Request,
  backendPath: string,
  options?: { timeoutMs?: number; forwardCredentials?: boolean },
): Promise<Response> {
  try {
    const base = getFinSightBaseUrl()
    if (!base) return finSightUnavailableResponse()

    let body: string
    try {
      body = JSON.stringify(await req.json().catch(() => ({})))
    } catch {
      body = "{}"
    }

    const target = `${base}${backendPath.startsWith("/") ? "" : "/"}${backendPath}`
    const controller = new AbortController()
    const timeoutMs = options?.timeoutMs ?? getProxyTimeoutMs()
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
    const outboundHeaders: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "application/json",
      Connection: "close",
      ...clientForwardHeaders(req),
    }
    applyCredentialHeaders(req, outboundHeaders, options?.forwardCredentials === true)

    let upstream: Response
    try {
      upstream = await fetch(target, {
        method: "POST",
        headers: outboundHeaders,
        body,
        signal: controller.signal,
        cache: "no-store",
      })
    } catch (err) {
      return upstreamFailureResponse(err, isAbortError(err))
    } finally {
      clearTimeout(timeoutId)
    }

    const payload: unknown = await upstream.json().catch(() => null)
    const root = asRecord(payload)
    const loginSucceeded =
      upstream.ok && root?.success !== false
    const tokens = loginSucceeded
      ? mergeTokens(extractLoginTokens(payload), extractTokensFromSetCookie(upstream))
      : { accessToken: null, refreshToken: null }
    const safePayload = stripLoginTokens(payload)
    const response = NextResponse.json(safePayload ?? { message: "응답을 해석하지 못했습니다." }, {
      status: upstream.status,
    })
    response.headers.set("Cache-Control", "no-store")
    if (loginSucceeded && tokens.accessToken) {
      applyAuthCookies(response, tokens)
    }
    return response
  } catch (err) {
    if (process.env.NODE_ENV === "development") {
      console.error("인증 프록시 처리 중 오류가 발생했습니다.", err)
    }
    return jsonResponse(500, "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
  }
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
    const outboundHeaders: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...clientForwardHeaders(req),
    }
    applyCredentialHeaders(req, outboundHeaders, forwardCredentials)

    let upstream: Response
    try {
      upstream = await fetch(target, {
        method: "POST",
        headers: outboundHeaders,
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
    forwardCredentials?: boolean
    cache?: RequestCache
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
    applyCredentialHeaders(req, headers, forwardCredentials)
    const contentType = req.headers.get("content-type") ?? req.headers.get("Content-Type")
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

      if (
        method !== "GET" &&
        method !== "HEAD" &&
        method !== "DELETE" &&
        body !== undefined
      ) {
        const ct = (contentType ?? "").toLowerCase()
        const bodyText =
          typeof body === "string"
            ? body.trim()
            : typeof init?.body === "string"
              ? init.body.trim()
              : ""
        const looksLikeJson =
          bodyText.startsWith("{") || bodyText.startsWith("[")
        if (
          !ct ||
          ct.startsWith("text/plain") ||
          (looksLikeJson && !ct.includes("application/json") && !ct.includes("multipart/"))
        ) {
          headers["Content-Type"] = "application/json"
        } else {
          headers["Content-Type"] = contentType!
        }
      } else if (
        contentType &&
        method !== "GET" &&
        method !== "HEAD" &&
        method !== "DELETE"
      ) {
        headers["Content-Type"] = contentType
      }

      upstream = await fetch(target, {
        method,
        headers,
        body,
        signal: controller.signal,
        cache: init?.cache ?? "no-store",
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
    applyCredentialHeaders(req, headers, true)
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

