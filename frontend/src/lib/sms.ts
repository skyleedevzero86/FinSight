import { authHeadersJson, readAccessToken } from "@/lib/finsightToken"

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

export type SmsSettings = {
  enabled: boolean
  newsAlertEnabled: boolean
  otpEnabled: boolean
  accountRecoveryEnabled: boolean
  systemAlertEnabled: boolean
  notificationEnabled: boolean
  defaultMessageType: string
  defaultFromNumber: string | null
  solapiEnabled: boolean
}

export type SmsSendLog = {
  id: number
  purpose: string
  purposeLabel: string
  messageType: string
  toPhone: string
  fromPhone: string | null
  contentPreview: string | null
  status: string
  externalMessageId: string | null
  errorMessage: string | null
  createdAt: string | null
}

export type SmsStats = {
  totalSent: number
  totalFailed: number
  totalSkipped: number
  sentLast7Days: number
  failedLast7Days: number
  byPurpose: Record<string, number>
  daily: { date: string; sent: number; failed: number; skipped: number }[]
}

export type SmsBalance = {
  balanceText: string
  simulation: boolean
}

function parseSettings(payload: unknown): SmsSettings | null {
  const root = asRecord(payload)
  const data = asRecord(root?.data) ?? root
  if (!data) return null
  return {
    enabled: Boolean(data.enabled),
    newsAlertEnabled: Boolean(data.newsAlertEnabled),
    otpEnabled: Boolean(data.otpEnabled),
    accountRecoveryEnabled: Boolean(data.accountRecoveryEnabled),
    systemAlertEnabled: Boolean(data.systemAlertEnabled),
    notificationEnabled: Boolean(data.notificationEnabled),
    defaultMessageType: typeof data.defaultMessageType === "string" ? data.defaultMessageType : "SMS",
    defaultFromNumber: typeof data.defaultFromNumber === "string" ? data.defaultFromNumber : null,
    solapiEnabled: Boolean(data.solapiEnabled),
  }
}

export async function fetchSmsSettings(): Promise<
  { ok: true; settings: SmsSettings } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/admin/sms/settings", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "설정을 불러오지 못했습니다.") }
  const settings = parseSettings(payload)
  if (!settings) return { ok: false, message: "설정 형식이 올바르지 않습니다." }
  return { ok: true, settings }
}

export async function updateSmsSettings(
  settings: Omit<SmsSettings, "solapiEnabled">
): Promise<{ ok: true; settings: SmsSettings } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/admin/sms/settings", {
    method: "PUT",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify(settings),
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "설정 저장에 실패했습니다.") }
  const parsed = parseSettings(payload)
  if (!parsed) return { ok: false, message: "설정 형식이 올바르지 않습니다." }
  return { ok: true, settings: parsed }
}

export async function sendAdminSms(input: {
  toPhone?: string
  userEmail?: string
  message: string
  messageType?: string
  subject?: string
  imageId?: string
}): Promise<{ ok: true; message: string } | { ok: false; message: string }> {
  const res = await fetch("/api/v1/admin/sms/send", {
    method: "POST",
    headers: { ...authHeadersJson(), "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "SMS 발송에 실패했습니다.") }
  return { ok: true, message: readMessage(payload, "SMS를 발송했습니다.") }
}

export async function fetchSmsBalance(): Promise<
  { ok: true; balance: SmsBalance } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/admin/sms/balance", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "잔액 조회에 실패했습니다.") }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  return {
    ok: true,
    balance: {
      balanceText: typeof data?.balanceText === "string" ? data.balanceText : "-",
      simulation: Boolean(data?.simulation),
    },
  }
}

export async function fetchSmsLogs(params: {
  page?: number
  size?: number
  status?: string
  purpose?: string
}): Promise<
  | { ok: true; content: SmsSendLog[]; totalPages: number; totalElements: number }
  | { ok: false; message: string }
> {
  const qs = new URLSearchParams()
  qs.set("page", String(params.page ?? 0))
  qs.set("size", String(params.size ?? 20))
  if (params.status) qs.set("status", params.status)
  if (params.purpose) qs.set("purpose", params.purpose)
  const res = await fetch(`/api/v1/admin/sms/logs?${qs}`, {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "이력을 불러오지 못했습니다.") }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  const contentRaw = Array.isArray(data?.content) ? data.content : []
  const content: SmsSendLog[] = contentRaw
    .map((row) => {
      const o = asRecord(row)
      if (!o || typeof o.id !== "number") return null
      return {
        id: o.id,
        purpose: typeof o.purpose === "string" ? o.purpose : "",
        purposeLabel: typeof o.purposeLabel === "string" ? o.purposeLabel : "",
        messageType: typeof o.messageType === "string" ? o.messageType : "SMS",
        toPhone: typeof o.toPhone === "string" ? o.toPhone : "",
        fromPhone: typeof o.fromPhone === "string" ? o.fromPhone : null,
        contentPreview: typeof o.contentPreview === "string" ? o.contentPreview : null,
        status: typeof o.status === "string" ? o.status : "",
        externalMessageId: typeof o.externalMessageId === "string" ? o.externalMessageId : null,
        errorMessage: typeof o.errorMessage === "string" ? o.errorMessage : null,
        createdAt: typeof o.createdAt === "string" ? o.createdAt : null,
      }
    })
    .filter((x): x is SmsSendLog => Boolean(x))
  return {
    ok: true,
    content,
    totalPages: typeof data?.totalPages === "number" ? data.totalPages : 1,
    totalElements: typeof data?.totalElements === "number" ? data.totalElements : content.length,
  }
}

export async function fetchSmsStats(): Promise<
  { ok: true; stats: SmsStats } | { ok: false; message: string }
> {
  const res = await fetch("/api/v1/admin/sms/stats", {
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "통계를 불러오지 못했습니다.") }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  if (!data) return { ok: false, message: "통계 형식이 올바르지 않습니다." }
  const byPurposeRaw = asRecord(data.byPurpose) ?? {}
  const byPurpose: Record<string, number> = {}
  for (const [k, v] of Object.entries(byPurposeRaw)) {
    if (typeof v === "number") byPurpose[k] = v
  }
  const dailyRaw = Array.isArray(data.daily) ? data.daily : []
  return {
    ok: true,
    stats: {
      totalSent: typeof data.totalSent === "number" ? data.totalSent : 0,
      totalFailed: typeof data.totalFailed === "number" ? data.totalFailed : 0,
      totalSkipped: typeof data.totalSkipped === "number" ? data.totalSkipped : 0,
      sentLast7Days: typeof data.sentLast7Days === "number" ? data.sentLast7Days : 0,
      failedLast7Days: typeof data.failedLast7Days === "number" ? data.failedLast7Days : 0,
      byPurpose,
      daily: dailyRaw
        .map((row) => {
          const o = asRecord(row)
          if (!o) return null
          return {
            date: typeof o.date === "string" ? o.date : "",
            sent: typeof o.sent === "number" ? o.sent : 0,
            failed: typeof o.failed === "number" ? o.failed : 0,
            skipped: typeof o.skipped === "number" ? o.skipped : 0,
          }
        })
        .filter((x): x is SmsStats["daily"][number] => Boolean(x)),
    },
  }
}

export async function uploadSmsImage(
  file: File
): Promise<{ ok: true; imageId: string } | { ok: false; message: string }> {
  const token = readAccessToken()
  const form = new FormData()
  form.append("file", file)
  const headers: HeadersInit = {}
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch("/api/v1/admin/sms/upload-image", {
    method: "POST",
    headers,
    body: form,
  })
  const payload = await readJson(res)
  if (!res.ok) return { ok: false, message: readMessage(payload, "이미지 업로드에 실패했습니다.") }
  const root = asRecord(payload)
  const data = asRecord(root?.data)
  const imageId = typeof data?.imageId === "string" ? data.imageId : null
  if (!imageId) return { ok: false, message: "이미지 ID를 받지 못했습니다." }
  return { ok: true, imageId }
}

export function formatSmsTime(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}
