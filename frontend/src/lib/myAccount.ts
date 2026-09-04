import { authHeaders, authHeadersJson, storeAuthSession, readAuthProvider } from "@/lib/finsightToken"
import type { TargetCategory } from "@/lib/registration"

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readMessage(payload: unknown, fallback: string): string {
  const root = asRecord(payload)
  if (!root) return fallback
  if (typeof root.message === "string" && root.message) return root.message
  if (Array.isArray(root.errors) && root.errors[0]) return String(root.errors[0])
  return fallback
}

async function readJson(res: Response): Promise<unknown> {
  try {
    return await res.json()
  } catch {
    return null
  }
}

function unwrapData(payload: unknown): Record<string, unknown> | null {
  const root = asRecord(payload)
  if (!root) return null
  return asRecord(root.data) ?? root
}

export type PasswordStatus = {
  changeRequired: boolean
  changeRecommended: boolean
  daysUntilExpiry: number | null
  statusMessage: string
}

export type UserProfile = {
  email: string
  nickname: string
  profileImageUrl: string | null
  watchlist: TargetCategory[]
}

export async function fetchUserProfile(): Promise<UserProfile | null> {
  const res = await fetch("/api/v1/users/profile", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  if (!res.ok) return null
  const data = unwrapData(await readJson(res))
  if (!data) return null
  const watchlistRaw = data.watchlist
  const watchlist = Array.isArray(watchlistRaw)
    ? (watchlistRaw.filter((v) => typeof v === "string") as TargetCategory[])
    : []
  return {
    email: typeof data.email === "string" ? data.email : "",
    nickname: typeof data.nickname === "string" ? data.nickname : "",
    profileImageUrl:
      typeof data.profileImageUrl === "string" && data.profileImageUrl
        ? data.profileImageUrl
        : null,
    watchlist,
  }
}

export async function fetchWatchlist(): Promise<TargetCategory[]> {
  const res = await fetch("/api/v1/users/watchlist", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  if (!res.ok) return []
  const payload = await readJson(res)
  return parseCategoryList(payload)
}

function parseCategoryList(payload: unknown): TargetCategory[] {
  const root = asRecord(payload)
  if (!root) return []
  const candidates = [root.data, root.categories, root]
  for (const candidate of candidates) {
    if (!Array.isArray(candidate)) continue
    return candidate.filter((v): v is TargetCategory => typeof v === "string")
  }
  return []
}

export async function updateWatchlist(
  categories: TargetCategory[],
): Promise<
  | { ok: true; categories: TargetCategory[] }
  | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/users/watchlist", {
    method: "PUT",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify({ categories }),
  })
  const payload = await readJson(res)
  if (!res.ok || (asRecord(payload)?.success === false)) {
    return { ok: false, message: readMessage(payload, "관심 카테고리를 저장하지 못했습니다.") }
  }
  const saved = parseCategoryList(payload)
  return { ok: true, categories: saved.length ? saved : categories }
}

export async function fetchPasswordStatus(): Promise<PasswordStatus | null> {
  const res = await fetch("/api/v1/users/password/status", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  if (!res.ok) return null
  const data = unwrapData(await readJson(res))
  if (!data) return null
  const required = data.passwordChangeRequired === true || data.changeRequired === true
    || data.isChangeRequired === true
  const recommended = data.passwordChangeRecommended === true || data.changeRecommended === true
    || data.isChangeRecommended === true
  const daysRaw = data.daysUntilExpiry
  return {
    changeRequired: required,
    changeRecommended: recommended,
    daysUntilExpiry: typeof daysRaw === "number" ? daysRaw : null,
    statusMessage: typeof data.statusMessage === "string" ? data.statusMessage : "",
  }
}

export async function updateProfile(body: {
  nickname?: string
  email?: string
}): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/users/profile", {
    method: "PUT",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "프로필을 저장하지 못했습니다.") }
  }
  const data = unwrapData(payload)
  const tokenWrap = data ? asRecord(data.token) : null
  const accessToken =
    tokenWrap && typeof tokenWrap.accessToken === "string" ? tokenWrap.accessToken : null
  if (accessToken) {
    storeAuthSession({
      accessToken,
      authProvider: readAuthProvider() ?? "WEB",
      remember: Boolean(
        typeof window !== "undefined" && localStorage.getItem("finsight_access_token"),
      ),
    })
  }
  return { ok: true }
}

export async function changePassword(body: {
  oldPassword: string
  newPassword: string
  newPasswordConfirm: string
}): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/users/password/change", {
    method: "POST",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "비밀번호를 변경하지 못했습니다.") }
  }
  return { ok: true }
}

export async function uploadProfileImage(
  file: File,
): Promise<{ ok: true; url?: string } | { ok: false; message: string }> {
  const form = new FormData()
  form.append("image", file)
  const res = await fetch("/api/v1/users/profile/image", {
    method: "POST",
    headers: authHeaders(),
    body: form,
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "프로필 사진을 업로드하지 못했습니다.") }
  }
  const data = unwrapData(payload)
  return {
    ok: true,
    url: data && typeof data.profileImageUrl === "string" ? data.profileImageUrl : undefined,
  }
}
