import { authHeadersJson } from "@/lib/finsightToken"

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

export type EmailStatus =
  | "PENDING"
  | "SENT"
  | "DELIVERED"
  | "OPENED"
  | "CLICKED"
  | "BOUNCED"
  | "FAILED"
  | "SPAM"

export type EmailMailPurpose =
  | "VERIFICATION_SIGNUP"
  | "VERIFICATION_FIND_EMAIL"
  | "VERIFICATION_FIND_PASSWORD"
  | "NEWS_ALERT"
  | "SYSTEM_NOTIFICATION"
  | "WELCOME"
  | "PASSWORD_CHANGE_REMINDER"
  | "ACCOUNT_RECOVERY_OTP"
  | "PASSWORD_RESET_CONFIRMATION"
  | "OTHER"

export type EmailActorType = "ANONYMOUS" | "USER" | "SYSTEM" | "ADMIN"

export type AdminEmailLog = {
  id: number
  recipient: string
  subject: string
  templateType: string | null
  purpose: EmailMailPurpose
  purposeLabel: string
  status: EmailStatus
  statusLabel: string
  fromAddress: string | null
  userId: number | null
  actorType: EmailActorType
  actorTypeLabel: string
  actorUserId: number | null
  requestIp: string | null
  requestLocation: string | null
  userAgent: string | null
  bodyPreview: string | null
  errorMessage: string | null
  relatedRef: string | null
  sentAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AdminEmailLogPage = {
  content: AdminEmailLog[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export const EMAIL_STATUS_LABEL: Record<EmailStatus, string> = {
  PENDING: "전송 대기",
  SENT: "전송 완료",
  DELIVERED: "전달 완료",
  OPENED: "열람",
  CLICKED: "클릭",
  BOUNCED: "반송",
  FAILED: "전송 실패",
  SPAM: "스팸 신고",
}

export const EMAIL_PURPOSE_OPTIONS: { value: EmailMailPurpose | ""; label: string }[] = [
  { value: "", label: "전체 용도" },
  { value: "VERIFICATION_SIGNUP", label: "회원가입 인증" },
  { value: "VERIFICATION_FIND_EMAIL", label: "이메일 찾기" },
  { value: "VERIFICATION_FIND_PASSWORD", label: "비밀번호 찾기" },
  { value: "ACCOUNT_RECOVERY_OTP", label: "계정 복구 OTP" },
  { value: "PASSWORD_RESET_CONFIRMATION", label: "비밀번호 재설정 확인" },
  { value: "PASSWORD_CHANGE_REMINDER", label: "비밀번호 변경 안내" },
  { value: "NEWS_ALERT", label: "뉴스 알림" },
  { value: "SYSTEM_NOTIFICATION", label: "시스템 알림" },
  { value: "WELCOME", label: "환영 메일" },
  { value: "OTHER", label: "기타" },
]

export const EMAIL_ACTOR_OPTIONS: { value: EmailActorType | ""; label: string }[] = [
  { value: "", label: "전체 주체" },
  { value: "ANONYMOUS", label: "비로그인" },
  { value: "USER", label: "로그인 사용자" },
  { value: "SYSTEM", label: "시스템" },
  { value: "ADMIN", label: "관리자" },
]

function parseLog(raw: unknown): AdminEmailLog | null {
  const o = asRecord(raw)
  if (!o || typeof o.id !== "number") return null
  const status = typeof o.status === "string" ? (o.status as EmailStatus) : "SENT"
  const purpose = typeof o.purpose === "string" ? (o.purpose as EmailMailPurpose) : "OTHER"
  const actorType = typeof o.actorType === "string" ? (o.actorType as EmailActorType) : "SYSTEM"
  return {
    id: o.id,
    recipient: typeof o.recipient === "string" ? o.recipient : "",
    subject: typeof o.subject === "string" ? o.subject : "",
    templateType: typeof o.templateType === "string" ? o.templateType : null,
    purpose,
    purposeLabel: typeof o.purposeLabel === "string" ? o.purposeLabel : purpose,
    status,
    statusLabel:
      typeof o.statusLabel === "string"
        ? o.statusLabel
        : EMAIL_STATUS_LABEL[status] ?? status,
    fromAddress: typeof o.fromAddress === "string" ? o.fromAddress : null,
    userId: typeof o.userId === "number" ? o.userId : null,
    actorType,
    actorTypeLabel: typeof o.actorTypeLabel === "string" ? o.actorTypeLabel : actorType,
    actorUserId: typeof o.actorUserId === "number" ? o.actorUserId : null,
    requestIp: typeof o.requestIp === "string" ? o.requestIp : null,
    requestLocation: typeof o.requestLocation === "string" ? o.requestLocation : null,
    userAgent: typeof o.userAgent === "string" ? o.userAgent : null,
    bodyPreview: typeof o.bodyPreview === "string" ? o.bodyPreview : null,
    errorMessage: typeof o.errorMessage === "string" ? o.errorMessage : null,
    relatedRef: typeof o.relatedRef === "string" ? o.relatedRef : null,
    sentAt: typeof o.sentAt === "string" ? o.sentAt : null,
    createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
    updatedAt: typeof o.updatedAt === "string" ? o.updatedAt : null,
  }
}

export async function fetchAdminEmailLogs(options: {
  page: number
  size: number
  keyword: string
  status: EmailStatus | ""
  purpose: EmailMailPurpose | ""
  actorType: EmailActorType | ""
  requestIp: string
}): Promise<{ ok: true; data: AdminEmailLogPage } | { ok: false; message: string }> {
  const params = new URLSearchParams()
  params.set("page", String(options.page))
  params.set("size", String(options.size))
  if (options.keyword.trim()) params.set("keyword", options.keyword.trim())
  if (options.status) params.set("status", options.status)
  if (options.purpose) params.set("purpose", options.purpose)
  if (options.actorType) params.set("actorType", options.actorType)
  if (options.requestIp.trim()) params.set("requestIp", options.requestIp.trim())

  const res = await fetch(`/api/v1/admin/email-logs?${params.toString()}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메일 발송 이력을 불러오지 못했습니다.") }
  }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  if (!data) return { ok: false, message: "메일 발송 이력 형식이 올바르지 않습니다." }
  const contentRaw = data.content
  const content = Array.isArray(contentRaw)
    ? contentRaw.map(parseLog).filter((v): v is AdminEmailLog => v !== null)
    : []
  return {
    ok: true,
    data: {
      content,
      page: typeof data.page === "number" ? data.page : 0,
      size: typeof data.size === "number" ? data.size : options.size,
      totalElements: typeof data.totalElements === "number" ? data.totalElements : content.length,
      totalPages: typeof data.totalPages === "number" ? data.totalPages : 1,
      hasNext: data.hasNext === true,
      hasPrevious: data.hasPrevious === true,
    },
  }
}

export async function fetchAdminEmailLog(
  id: number,
): Promise<{ ok: true; data: AdminEmailLog } | { ok: false; message: string }> {
  const res = await fetch(`/api/v1/admin/email-logs/${id}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) {
    return { ok: false, message: readMessage(payload, "메일 발송 이력을 불러오지 못했습니다.") }
  }
  const root = asRecord(payload)
  const parsed = parseLog(root?.data)
  if (!parsed) return { ok: false, message: "메일 발송 이력 형식이 올바르지 않습니다." }
  return { ok: true, data: parsed }
}
