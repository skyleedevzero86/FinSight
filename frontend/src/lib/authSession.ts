import {
  authHeadersJson,
  clearAuthSession,
  readAccessToken,
  type AuthProvider,
} from "@/lib/finsightToken"

export type AuthUser = {
  id: number
  email: string
  nickname: string
  role: string
  authProvider: AuthProvider
  profileImageUrl: string | null
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function parseAuthProvider(value: unknown): AuthProvider {
  if (value === "WEB" || value === "KAKAO" || value === "NAVER" || value === "GOOGLE") {
    return value
  }
  return "WEB"
}

export function parseAuthUser(payload: unknown): AuthUser | null {
  const root = asRecord(payload)
  if (!root) return null
  const data = asRecord(root.data) ?? root
  const email = typeof data.email === "string" ? data.email : ""
  const nicknameRaw = typeof data.nickname === "string" ? data.nickname.trim() : ""
  const idRaw = data.id
  const id = typeof idRaw === "number" ? idRaw : Number(idRaw)
  if (!email && !nicknameRaw) return null
  return {
    id: Number.isFinite(id) ? id : 0,
    email,
    nickname: nicknameRaw || email.split("@")[0] || "회원",
    role: typeof data.role === "string" ? data.role : "USER",
    authProvider: parseAuthProvider(data.authProvider),
    profileImageUrl:
      typeof data.profileImageUrl === "string" && data.profileImageUrl
        ? data.profileImageUrl
        : null,
  }
}

export function authProviderLabel(provider: AuthProvider): string {
  if (provider === "KAKAO") return "카카오"
  if (provider === "NAVER") return "네이버"
  if (provider === "GOOGLE") return "구글"
  return "이메일"
}

export async function fetchCurrentUser(): Promise<AuthUser | null> {
  if (!readAccessToken()) return null
  try {
    const res = await fetch("/api/v1/auth/me", {
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (res.status === 401 || res.status === 403) {
      clearAuthSession({ emit: false })
      return null
    }
    if (!res.ok) return null
    const payload: unknown = await res.json().catch(() => null)
    return parseAuthUser(payload)
  } catch {
    return null
  }
}
