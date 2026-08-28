import { authHeadersJson } from "@/lib/finsightToken"
import type { AuthProvider } from "@/lib/finsightToken"

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}

function readMessage(payload: unknown, fallback: string): string {
  const root = asRecord(payload)
  if (!root) return fallback
  if (typeof root.message === "string" && root.message) return root.message
  return fallback
}

async function readJson(res: Response): Promise<unknown> {
  try {
    return await res.json()
  } catch {
    return null
  }
}

export type UserStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED" | "WITHDRAWN"
export type UserRole = "USER" | "MANAGER" | "ADMIN"
export type RevealField = "username" | "email" | "phone"

export type AdminUser = {
  id: number
  username: string
  nickname: string
  email: string
  phoneNumber: string | null
  usernameMasked: boolean
  emailMasked: boolean
  phoneMasked: boolean
  authProvider: AuthProvider
  status: UserStatus
  role: UserRole
  lastLoginAt: string | null
  createDate: string | null
}

export type AdminUserPage = {
  content: AdminUser[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export const USER_STATUS_LABEL: Record<UserStatus, string> = {
  PENDING: "승인 대기",
  APPROVED: "정상",
  REJECTED: "거부",
  SUSPENDED: "정지",
  WITHDRAWN: "탈퇴",
}

export const USER_ROLE_LABEL: Record<UserRole, string> = {
  USER: "일반",
  MANAGER: "매니저",
  ADMIN: "관리자",
}

function parseProvider(value: unknown): AuthProvider {
  if (value === "KAKAO" || value === "NAVER" || value === "GOOGLE" || value === "WEB") return value
  return "WEB"
}

function parseUser(raw: unknown): AdminUser | null {
  const o = asRecord(raw)
  if (!o) return null
  const id = typeof o.id === "number" ? o.id : Number(o.id)
  if (!Number.isFinite(id)) return null
  const status = o.status
  const role = o.role
  return {
    id,
    username: typeof o.username === "string" ? o.username : "",
    nickname: typeof o.nickname === "string" ? o.nickname : "",
    email: typeof o.email === "string" ? o.email : "",
    phoneNumber: typeof o.phoneNumber === "string" && o.phoneNumber ? o.phoneNumber : null,
    usernameMasked: o.usernameMasked !== false,
    emailMasked: o.emailMasked !== false,
    phoneMasked: o.phoneMasked !== false,
    authProvider: parseProvider(o.authProvider),
    status:
      status === "PENDING" ||
      status === "APPROVED" ||
      status === "REJECTED" ||
      status === "SUSPENDED" ||
      status === "WITHDRAWN"
        ? status
        : "APPROVED",
    role: role === "ADMIN" || role === "MANAGER" || role === "USER" ? role : "USER",
    lastLoginAt: typeof o.lastLoginAt === "string" ? o.lastLoginAt : null,
    createDate: typeof o.createDate === "string" ? o.createDate : null,
  }
}

export async function fetchAdminUsers(options: {
  page: number
  size: number
  status: UserStatus | ""
  keyword: string
  reveal: RevealField[]
}): Promise<{ ok: true; data: AdminUserPage } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", String(options.page))
  params.set("size", String(options.size))
  if (options.status) params.set("status", options.status)
  if (options.keyword.trim()) params.set("keyword", options.keyword.trim())
  if (options.reveal.length) params.set("reveal", options.reveal.join(","))
  const res = await fetch(`/api/v1/admin/users?${params.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "사용자 목록을 불러오지 못했습니다.") }
  }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  if (!data) {
    return { ok: false, message: "사용자 목록 형식이 올바르지 않습니다." }
  }
  const contentRaw = Array.isArray(data.content)
    ? data.content
    : Array.isArray(data.items)
      ? data.items
      : []
  const content = contentRaw
    .map(parseUser)
    .filter((v): v is AdminUser => v !== null)
  const totalElements =
    typeof data.totalElements === "number"
      ? data.totalElements
      : typeof data.total === "number"
        ? data.total
        : content.length
  const totalPages =
    typeof data.totalPages === "number"
      ? data.totalPages
      : Math.max(1, Math.ceil(totalElements / Math.max(options.size, 1)))
  return {
    ok: true,
    data: {
      content,
      page: typeof data.page === "number" ? data.page : typeof data.number === "number" ? data.number : 0,
      size: typeof data.size === "number" ? data.size : options.size,
      totalElements,
      totalPages,
      hasNext: data.hasNext === true || data.last === false,
      hasPrevious: data.hasPrevious === true || data.first === false,
    },
  }
}

async function postAction(
  path: string,
  fallback: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(path, {
    method: "POST",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: "{}",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, fallback) }
  return { ok: true }
}

export function suspendAdminUser(userId: number) {
  return postAction(`/api/v1/admin/users/${userId}/suspend`, "계정을 정지하지 못했습니다.")
}

export function unlockAdminUser(userId: number) {
  return postAction(`/api/v1/admin/users/${userId}/unlock`, "정지를 해제하지 못했습니다.")
}

export function restoreAdminUser(userId: number) {
  return postAction(`/api/v1/admin/users/${userId}/restore`, "계정을 복구하지 못했습니다.")
}

export function approveAdminUser(userId: number) {
  return postAction(`/api/v1/admin/users/${userId}/approve`, "사용자를 승인하지 못했습니다.")
}

export function rejectAdminUser(userId: number) {
  return postAction(`/api/v1/admin/users/${userId}/reject`, "사용자를 거부하지 못했습니다.")
}

export async function deleteAdminUser(
  userId: number,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/users/${userId}`, {
    method: "DELETE",
    headers: authHeadersJson(),
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "사용자를 삭제하지 못했습니다.") }
  return { ok: true }
}

export async function resetAdminUserPassword(
  userId: number,
  newPassword: string,
  newPasswordConfirm: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/users/${userId}/password`, {
    method: "PUT",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify({ newPassword, newPasswordConfirm }),
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "비밀번호를 변경하지 못했습니다.") }
  return { ok: true }
}

export async function withdrawSelf(): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/users/withdraw", {
    method: "POST",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: "{}",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "탈퇴하지 못했습니다.") }
  return { ok: true }
}

export function canManageUsers(role: string | undefined): boolean {
  if (!role) return false
  const normalized = role.startsWith("ROLE_") ? role.slice(5) : role
  return normalized === "ADMIN" || normalized === "MANAGER"
}
