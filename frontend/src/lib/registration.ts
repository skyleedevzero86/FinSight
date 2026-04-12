export const WATCHLIST_CATEGORIES = [
  { value: "GENERAL", label: "일반" },
  { value: "SPY", label: "S&P 500 ETF" },
  { value: "QQQ", label: "나스닥 100 ETF" },
  { value: "BTC", label: "비트코인 (BTC)" },
  { value: "AAPL", label: "애플" },
  { value: "MSFT", label: "마이크로소프트" },
  { value: "NVDA", label: "엔비디아" },
  { value: "GOOGL", label: "구글 (알파벳)" },
  { value: "META", label: "메타" },
  { value: "TSLA", label: "테슬라" },
  { value: "BITCOIN", label: "비트코인 (BITCOIN)" },
  { value: "NONE", label: "없음 · 미분류" },
] as const

export type TargetCategory = (typeof WATCHLIST_CATEGORIES)[number]["value"]

export type UserRegistrationRequest = {
  username: string
  email: string
  password: string
  nickname?: string
  watchlist?: TargetCategory[]
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export type FieldErrors = Partial<
  Record<"username" | "email" | "password" | "passwordConfirm" | "nickname", string>
>

export function validateUsername(v: string): string | null {
  const t = v.trim()
  if (!t) return "사용자명은 필수입니다."
  if (t.length < 3 || t.length > 50) return "사용자명은 3~50자 사이여야 합니다."
  return null
}

export function validateEmail(v: string): string | null {
  const t = v.trim()
  if (!t) return "이메일은 필수입니다."
  if (!EMAIL_RE.test(t)) return "올바른 이메일 형식이 아닙니다."
  return null
}

export function validatePassword(v: string): string | null {
  if (!v) return "비밀번호는 필수입니다."
  if (v.length < 8) return "비밀번호는 최소 8자 이상이어야 합니다."
  if (!/[A-Za-z]/.test(v)) return "비밀번호에 영문을 포함해 주세요."
  if (!/\d/.test(v)) return "비밀번호에 숫자를 포함해 주세요."
  if (!/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>\/?]/.test(v)) {
    return "비밀번호에 특수문자를 포함해 주세요."
  }
  return null
}

export function validateNickname(v: string): string | null {
  if (!v.trim()) return null
  if (v.length > 50) return "닉네임은 50자를 초과할 수 없습니다."
  return null
}

export function validateSignupForm(values: {
  username: string
  email: string
  password: string
  passwordConfirm: string
  nickname: string
}): FieldErrors {
  const errors: FieldErrors = {}
  const u = validateUsername(values.username)
  if (u) errors.username = u
  const e = validateEmail(values.email)
  if (e) errors.email = e
  const p = validatePassword(values.password)
  if (p) errors.password = p
  if (values.password !== values.passwordConfirm) {
    errors.passwordConfirm = "비밀번호가 일치하지 않습니다."
  }
  const n = validateNickname(values.nickname)
  if (n) errors.nickname = n
  return errors
}

function readApiErrorMessage(payload: unknown, fallback: string): string {
  if (payload && typeof payload === "object") {
    const o = payload as Record<string, unknown>
    if (typeof o.message === "string" && o.message) return o.message
    if (Array.isArray(o.messages) && o.messages.length) {
      const first = o.messages[0]
      if (typeof first === "string") return first
    }
    const errors = o.errors
    if (Array.isArray(errors)) {
      const parts = errors
        .map((item) => {
          if (typeof item === "string") return item
          if (item && typeof item === "object") {
            const e = item as Record<string, unknown>
            if (typeof e.defaultMessage === "string") return e.defaultMessage
            if (typeof e.message === "string") return e.message
          }
          return null
        })
        .filter(Boolean) as string[]
      if (parts.length) return parts.join(" ")
    }
    if (errors && typeof errors === "object" && !Array.isArray(errors)) {
      const parts = Object.values(errors as Record<string, unknown>)
        .flatMap((v) => {
          if (typeof v === "string") return [v]
          if (Array.isArray(v) && v.every((x) => typeof x === "string"))
            return v as string[]
          return []
        })
        .filter(Boolean)
      if (parts.length) return parts.join(" ")
    }
  }
  return fallback
}

export async function postRegister(
  body: UserRegistrationRequest,
): Promise<{ ok: true } | { ok: false; status: number; message: string }> {
  const payload: UserRegistrationRequest = {
    username: body.username.trim(),
    email: body.email.trim(),
    password: body.password,
  }
  const nick = body.nickname?.trim()
  if (nick) payload.nickname = nick
  if (body.watchlist?.length) payload.watchlist = body.watchlist

  const res = await fetch("/api/v1/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(payload),
  })

  if (res.ok) return { ok: true }

  let data: unknown
  try {
    data = await res.json()
  } catch {
    data = null
  }
  const message = readApiErrorMessage(
    data,
    res.status === 503
      ? "서버 연결이 설정되지 않았습니다. 관리자에게 문의하세요."
      : "가입에 실패했습니다. 입력 내용을 확인해 주세요.",
  )
  return { ok: false, status: res.status, message }
}
