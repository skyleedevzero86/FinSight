export type EmailVerificationPurpose =
  | "SIGNUP"
  | "FIND_EMAIL"
  | "FIND_PASSWORD"

type ApiEnvelope<T> = {
  success?: boolean
  message?: string
  data?: T
}

export type EmailVerificationIssue = {
  challengeToken: string
  maskedEmail: string
  purpose: EmailVerificationPurpose
  purposeLabel: string
  expiresInSeconds: number
}

export type EmailVerificationChallenge = {
  maskedEmail: string
  purpose: EmailVerificationPurpose
  purposeLabel: string
  status: string
  expiresInSeconds: number
  expired: boolean
}

export type EmailVerificationConfirm = {
  verified: boolean
  purpose: EmailVerificationPurpose
  purposeLabel: string
  maskedEmail: string
  email?: string | null
  username?: string | null
  maskedUsername?: string | null
  redirectTo?: string
  canResetPassword?: boolean
}

export type EmailVerificationDispute = {
  disputed: boolean
  accountSuspended: boolean
  purpose?: EmailVerificationPurpose
  purposeLabel?: string
  message: string
}

const SIGNUP_DRAFT_KEY = "finsight.signup.draft"
const VERIFIED_KEY = "finsight.email.verified"
const PENDING_KEY = "finsight.email.pending"

function readMessage(data: unknown): string | undefined {
  if (data && typeof data === "object" && "message" in data) {
    const m = (data as { message: unknown }).message
    if (typeof m === "string" && m) return m
  }
  return undefined
}

async function readJson(res: Response): Promise<unknown> {
  try {
    return await res.json()
  } catch {
    return null
  }
}

export async function requestEmailVerification(
  email: string,
  purpose: EmailVerificationPurpose,
): Promise<
  | { ok: true; data: EmailVerificationIssue }
  | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/auth/email/verify-request", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ email: email.trim(), purpose }),
  })
  const payload = (await readJson(res)) as ApiEnvelope<EmailVerificationIssue> | null
  if (!res.ok || !payload?.data?.challengeToken) {
    return {
      ok: false,
      message:
        readMessage(payload) ??
        (res.status === 503
          ? "백엔드 연결(FINSIGHT_API_BASE_URL)을 확인해 주세요."
          : "이메일 인증 요청에 실패했습니다."),
    }
  }
  return { ok: true, data: payload.data }
}

export async function fetchEmailChallenge(
  token: string,
): Promise<
  | { ok: true; data: EmailVerificationChallenge }
  | { ok: false; message: string }
> {
  const res = await fetch(
    `/api/v1/auth/email/challenge?token=${encodeURIComponent(token)}`,
    { headers: { Accept: "application/json" } },
  )
  const payload = (await readJson(res)) as ApiEnvelope<EmailVerificationChallenge> | null
  if (!res.ok || !payload?.data) {
    return {
      ok: false,
      message: readMessage(payload) ?? "유효하지 않은 인증 링크입니다.",
    }
  }
  return { ok: true, data: payload.data }
}

export async function confirmEmailVerification(
  token: string,
  code: string,
): Promise<
  | { ok: true; data: EmailVerificationConfirm }
  | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/auth/email/verify", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ token, code }),
  })
  const payload = (await readJson(res)) as ApiEnvelope<EmailVerificationConfirm> | null
  if (!res.ok || !payload?.data?.verified) {
    return {
      ok: false,
      message: readMessage(payload) ?? "인증 코드가 올바르지 않습니다.",
    }
  }
  return { ok: true, data: payload.data }
}

export async function resetPasswordAfterVerification(
  token: string,
  password: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/auth/email/reset-password", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({ token, password }),
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return {
      ok: false,
      message: readMessage(payload) ?? "비밀번호 변경에 실패했습니다.",
    }
  }
  return { ok: true }
}

export async function disputeEmailVerification(
  token: string,
): Promise<
  | { ok: true; data: EmailVerificationDispute }
  | { ok: false; message: string }
> {
  const res = await fetch(
    `/api/v1/auth/email/dispute?token=${encodeURIComponent(token)}`,
    {
      method: "POST",
      headers: { Accept: "application/json" },
    },
  )
  const payload = (await readJson(res)) as ApiEnvelope<EmailVerificationDispute> | null
  if (!res.ok || !payload?.data?.disputed) {
    return {
      ok: false,
      message: readMessage(payload) ?? "요청 처리에 실패했습니다.",
    }
  }
  return { ok: true, data: payload.data }
}

export type SignupDraft = {
  username: string
  email: string
  nickname: string
}

export function saveSignupDraft(draft: SignupDraft) {
  sessionStorage.setItem(SIGNUP_DRAFT_KEY, JSON.stringify(draft))
}

export function loadSignupDraft(): SignupDraft | null {
  try {
    const raw = sessionStorage.getItem(SIGNUP_DRAFT_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as SignupDraft
    if (!parsed?.email) return null
    return parsed
  } catch {
    return null
  }
}

export function savePendingVerification(email: string, purpose: EmailVerificationPurpose) {
  sessionStorage.setItem(
    PENDING_KEY,
    JSON.stringify({ email: email.trim().toLowerCase(), purpose }),
  )
}

export function loadPendingVerification(): {
  email: string
  purpose: EmailVerificationPurpose
} | null {
  try {
    const raw = sessionStorage.getItem(PENDING_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as { email?: string; purpose?: EmailVerificationPurpose }
    if (!parsed.email || !parsed.purpose) return null
    return { email: parsed.email, purpose: parsed.purpose }
  } catch {
    return null
  }
}

export function markEmailVerified(email: string, purpose: EmailVerificationPurpose) {
  sessionStorage.setItem(
    VERIFIED_KEY,
    JSON.stringify({ email: email.trim().toLowerCase(), purpose, at: Date.now() }),
  )
}

export function isEmailMarkedVerified(email: string, purpose: EmailVerificationPurpose) {
  try {
    const raw = sessionStorage.getItem(VERIFIED_KEY)
    if (!raw) return false
    const parsed = JSON.parse(raw) as {
      email?: string
      purpose?: string
      at?: number
    }
    if (parsed.purpose !== purpose) return false
    if ((parsed.email ?? "").toLowerCase() !== email.trim().toLowerCase()) return false
    if (!parsed.at || Date.now() - parsed.at > 24 * 60 * 60 * 1000) return false
    return true
  } catch {
    return false
  }
}
