export const FINSIGHT_ACCESS_TOKEN_KEY = "finsight_access_token"
export const FINSIGHT_AUTH_PROVIDER_KEY = "finsight_auth_provider"
export const FINSIGHT_AUTH_CHANGED_EVENT = "finsight-auth-changed"
export const FINSIGHT_FORCE_PASSWORD_KEY = "finsight_force_password"
export const FINSIGHT_AUTH_HINT_COOKIE = "finsight_auth"

export function emitAuthChanged() {
  if (typeof window === "undefined") return
  window.dispatchEvent(new Event(FINSIGHT_AUTH_CHANGED_EVENT))
}

export type AuthProvider = "WEB" | "KAKAO" | "NAVER" | "GOOGLE"

const consumedOAuthCodes = new Set<string>()

export function consumeOAuthCode(code: string): boolean {
  if (!code || consumedOAuthCodes.has(code)) return false
  consumedOAuthCodes.add(code)
  return true
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null
  const prefix = `${name}=`
  const hit = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
  if (!hit) return null
  return decodeURIComponent(hit.slice(prefix.length))
}

export function hasAuthSession(): boolean {
  return readCookie(FINSIGHT_AUTH_HINT_COOKIE) === "1"
}

export function readAccessToken(): string | null {
  return hasAuthSession() ? "cookie" : null
}

export function isAccessTokenUsable(token: string | null | undefined): boolean {
  return Boolean(token)
}

export function readUsableAccessToken(): string | null {
  return hasAuthSession() ? "cookie" : null
}

export function readAuthProvider(): AuthProvider | null {
  if (typeof window === "undefined") return null
  try {
    const value =
      sessionStorage.getItem(FINSIGHT_AUTH_PROVIDER_KEY) ||
      localStorage.getItem(FINSIGHT_AUTH_PROVIDER_KEY)
    if (value === "WEB" || value === "KAKAO" || value === "NAVER" || value === "GOOGLE") {
      return value
    }
    return null
  } catch {
    return null
  }
}

export function storeAuthSession(options: {
  accessToken?: string | null
  authProvider: AuthProvider
  remember?: boolean
}) {
  const { authProvider, remember } = options
  try {
    localStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    if (remember) {
      localStorage.setItem(FINSIGHT_AUTH_PROVIDER_KEY, authProvider)
      sessionStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
    } else {
      sessionStorage.setItem(FINSIGHT_AUTH_PROVIDER_KEY, authProvider)
      localStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
    }
  } catch {
    void 0
  }
  emitAuthChanged()
}

export function clearAuthSession(options?: { emit?: boolean }) {
  try {
    localStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    localStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
    sessionStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
  } catch {
    void 0
  }
  if (options?.emit !== false) {
    emitAuthChanged()
  }
}

export function authHeadersJson(): HeadersInit {
  return {
    Accept: "application/json",
    "Content-Type": "application/json",
  }
}

export function authHeaders(): HeadersInit {
  return {}
}
