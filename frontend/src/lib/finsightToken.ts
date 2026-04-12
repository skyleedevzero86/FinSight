export const FINSIGHT_ACCESS_TOKEN_KEY = "finsight_access_token"

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

export function authHeadersJson(): HeadersInit {
  const t = readAccessToken()
  if (!t) return { Accept: "application/json" }
  return { Accept: "application/json", Authorization: `Bearer ${t}` }
}
