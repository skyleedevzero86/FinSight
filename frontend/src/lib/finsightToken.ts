export const FINSIGHT_ACCESS_TOKEN_KEY = "finsight_access_token"
export const FINSIGHT_AUTH_PROVIDER_KEY = "finsight_auth_provider"
export const FINSIGHT_AUTH_CHANGED_EVENT = "finsight-auth-changed"
export const FINSIGHT_FORCE_PASSWORD_KEY = "finsight_force_password"

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

export function readAccessToken(): string | null {
  if (typeof window === "undefined") return null
  try {
    return (
      localStorage.getItem(FINSIGHT_ACCESS_TOKEN_KEY) ||
      sessionStorage.getItem(FINSIGHT_ACCESS_TOKEN_KEY)
    )
  } catch {
    return null
  }
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
  accessToken: string
  authProvider: AuthProvider
  remember?: boolean
}) {
  const { accessToken, authProvider, remember } = options
  if (remember) {
    localStorage.setItem(FINSIGHT_ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(FINSIGHT_AUTH_PROVIDER_KEY, authProvider)
    sessionStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
  } else {
    sessionStorage.setItem(FINSIGHT_ACCESS_TOKEN_KEY, accessToken)
    sessionStorage.setItem(FINSIGHT_AUTH_PROVIDER_KEY, authProvider)
    localStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
    localStorage.removeItem(FINSIGHT_AUTH_PROVIDER_KEY)
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
  const t = readAccessToken()
  if (!t) return { Accept: "application/json" }
  return { Accept: "application/json", Authorization: `Bearer ${t}` }
}

export function authHeaders(): HeadersInit {
  const t = readAccessToken()
  if (!t) return {}
  return { Authorization: `Bearer ${t}` }
}
