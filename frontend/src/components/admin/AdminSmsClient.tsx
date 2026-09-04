"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  fetchSmsBalance,
  fetchSmsLogs,
  fetchSmsSettings,
  fetchSmsStats,
  formatSmsTime,
  sendAdminSms,
  updateSmsSettings,
  uploadSmsImage,
  type SmsSendLog,
  type SmsSettings,
  type SmsStats,
} from "@/lib/sms"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

const PURPOSE_LABELS: Record<string, string> = {
  NEWS_ALERT: "뉴스 알림",
  OTP: "OTP",
  ACCOUNT_RECOVERY: "계정 복구",
  SYSTEM: "시스템",
  NOTIFICATION: "일반 알림",
  MANUAL: "수동 발송",
}

export default function AdminSmsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [settings, setSettings] = useState<SmsSettings | null>(null)
  const [stats, setStats] = useState<SmsStats | null>(null)
  const [balanceText, setBalanceText] = useState("-")
  const [simulation, setSimulation] = useState(true)
  const [logs, setLogs] = useState<SmsSendLog[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [targetMode, setTargetMode] = useState<"phone" | "email">("phone")
  const [toPhone, setToPhone] = useState("")
  const [userEmail, setUserEmail] = useState("")
  const [sendMessage, setSendMessage] = useState("")
  const [messageType, setMessageType] = useState("SMS")
  const [subject, setSubject] = useState("")
  const [imageId, setImageId] = useState("")

  const loadAll = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [s, st, b, l] = await Promise.all([
        fetchSmsSettings(),
        fetchSmsStats(),
        fetchSmsBalance(),
        fetchSmsLogs({ page, size: 15 }),
      ])
      const failures: string[] = []
      if (s.ok) setSettings(s.settings)
      else failures.push(s.message)
      if (st.ok) setStats(st.stats)
      else failures.push(st.message)
      if (b.ok) {
        setBalanceText(b.balance.balanceText)
        setSimulation(b.balance.simulation)
      } else failures.push(b.message)
      if (l.ok) {
        setLogs(l.content)
        setTotalPages(Math.max(1, l.totalPages))
      } else failures.push(l.message)
      if (failures.length > 0) setError(failures.join(" "))
    } catch {
      setError("SMS 대시보드 정보를 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    if (!ready) return
    if (!allowed) {
      router.replace("/")
      return
    }
    void loadAll()
  }, [ready, allowed, router, loadAll])

  async function onSaveSettings(e: FormEvent) {
    e.preventDefault()
    if (!settings) return
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      const { solapiEnabled: _, ...body } = settings
      const result = await updateSmsSettings(body)
      if (!result.ok) {
        setError(result.message)
        return
      }
      setSettings(result.settings)
      setMessage("SMS 발송 설정을 저장했습니다.")
    } catch {
      setError("SMS 발송 설정 저장 중 오류가 발생했습니다.")
    } finally {
      setSaving(false)
    }
  }

  async function onSend(e: FormEvent) {
    e.preventDefault()
    if (!sendMessage.trim()) {
      setError("메시지 내용을 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    try {
      const result = await sendAdminSms({
        toPhone: targetMode === "phone" ? toPhone.trim() : undefined,
        userEmail: targetMode === "email" ? userEmail.trim() : undefined,
        message: sendMessage.trim(),
        messageType,
        subject: subject.trim() || undefined,
        imageId: imageId.trim() || undefined,
      })
      if (!result.ok) {
        setError(result.message)
        return
      }
      setMessage(result.message)
      setSendMessage("")
      void loadAll()
    } catch {
      setError("SMS 발송 중 오류가 발생했습니다.")
    } finally {
      setSaving(false)
    }
  }

  async function onUpload(file: File | null) {
    if (!file) return
    setSaving(true)
    setError(null)
    try {
      const result = await uploadSmsImage(file)
      if (!result.ok) {
        setError(result.message)
        return
      }
      setImageId(result.imageId)
      setMessage(`이미지 업로드 완료: ${result.imageId}`)
    } catch {
      setError("이미지 업로드 중 오류가 발생했습니다.")
    } finally {
      setSaving(false)
    }
  }

  if (!ready || !allowed) {
    return <div className="mx-auto max-w-5xl px-4 py-16 text-sm text-gray-500">권한 확인 중…</div>
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-2xl font-semibold text-gray-900">SMS 알림</h1>
      <p className="mt-2 text-sm text-gray-500">
        Solapi SMS 발송 설정·수동 발송·잔액·이력·통계. 마스터 스위치를 켠 뒤 용도별 체크를 켜야 자동/수동
        발송이 동작합니다.
      </p>

      {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}
      {message ? <p className="mt-4 text-sm text-teal-700">{message}</p> : null}

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-gray-900">잔액 · Solapi</h2>
          <button type="button" className={buttonClass} onClick={() => void loadAll()} disabled={loading}>
            새로고침
          </button>
        </div>
        <p className="mt-3 text-sm text-gray-800">{balanceText}</p>
        <p className="mt-1 text-xs text-gray-500">
          {simulation
            ? "현재 시뮬레이션 모드입니다. solapi.enabled=true 및 API 키 설정 후 실발송됩니다."
            : "Solapi 실연동 모드입니다."}
        </p>
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">발송 설정</h2>
        {settings ? (
          <form className="mt-4 grid gap-3" onSubmit={(e) => void onSaveSettings(e)}>
            <label className="flex items-center gap-2 text-sm font-medium text-gray-900">
              <input
                type="checkbox"
                checked={settings.enabled}
                onChange={(e) => setSettings({ ...settings, enabled: e.target.checked })}
              />
              SMS 마스터 스위치 (전체 on/off)
            </label>
            <div className="grid gap-2 sm:grid-cols-2">
              {(
                [
                  ["newsAlertEnabled", "뉴스 알림 자동 SMS"],
                  ["otpEnabled", "OTP SMS"],
                  ["accountRecoveryEnabled", "계정 복구 SMS"],
                  ["systemAlertEnabled", "시스템 알림 SMS"],
                  ["notificationEnabled", "일반 알림 SMS"],
                ] as const
              ).map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 text-sm text-gray-800">
                  <input
                    type="checkbox"
                    checked={settings[key]}
                    disabled={!settings.enabled}
                    onChange={(e) => setSettings({ ...settings, [key]: e.target.checked })}
                  />
                  {label}
                </label>
              ))}
            </div>
            <fieldset className="mt-2">
              <legend className="text-sm text-gray-600">기본 메시지 타입</legend>
              <div className="mt-2 flex flex-wrap gap-4 text-sm">
                {(["SMS", "LMS", "MMS"] as const).map((type) => (
                  <label key={type} className="flex items-center gap-1.5">
                    <input
                      type="radio"
                      name="defaultMessageType"
                      checked={settings.defaultMessageType === type}
                      onChange={() => setSettings({ ...settings, defaultMessageType: type })}
                    />
                    {type}
                  </label>
                ))}
              </div>
            </fieldset>
            <label className="grid gap-1 text-sm">
              <span className="text-gray-600">기본 발신번호 (선택)</span>
              <input
                className={inputClass}
                value={settings.defaultFromNumber ?? ""}
                onChange={(e) =>
                  setSettings({ ...settings, defaultFromNumber: e.target.value || null })
                }
                placeholder="solapi.default-from-number 사용"
              />
            </label>
            <button type="submit" className={primaryButtonClass} disabled={saving}>
              {saving ? "저장 중…" : "설정 저장"}
            </button>
          </form>
        ) : (
          <p className="mt-4 text-sm text-gray-400">설정을 불러오는 중…</p>
        )}
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">수동 발송</h2>
        <form className="mt-4 grid gap-3" onSubmit={(e) => void onSend(e)}>
          <div className="flex gap-4 text-sm">
            <label className="flex items-center gap-1.5">
              <input
                type="radio"
                checked={targetMode === "phone"}
                onChange={() => setTargetMode("phone")}
              />
              전화번호
            </label>
            <label className="flex items-center gap-1.5">
              <input
                type="radio"
                checked={targetMode === "email"}
                onChange={() => setTargetMode("email")}
              />
              사용자 이메일
            </label>
          </div>
          {targetMode === "phone" ? (
            <input
              className={inputClass}
              value={toPhone}
              onChange={(e) => setToPhone(e.target.value)}
              placeholder="01012345678"
              required
            />
          ) : (
            <input
              className={inputClass}
              type="email"
              value={userEmail}
              onChange={(e) => setUserEmail(e.target.value)}
              placeholder="user@example.com"
              required
            />
          )}
          <div className="flex flex-wrap gap-4 text-sm">
            {(["SMS", "LMS", "MMS"] as const).map((type) => (
              <label key={type} className="flex items-center gap-1.5">
                <input
                  type="radio"
                  name="messageType"
                  checked={messageType === type}
                  onChange={() => setMessageType(type)}
                />
                {type}
              </label>
            ))}
          </div>
          {(messageType === "LMS" || messageType === "MMS") && (
            <input
              className={inputClass}
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="제목"
            />
          )}
          {messageType === "MMS" && (
            <div className="grid gap-2">
              <input
                type="file"
                accept="image/*"
                onChange={(e) => void onUpload(e.target.files?.[0] ?? null)}
              />
              <input
                className={inputClass}
                value={imageId}
                onChange={(e) => setImageId(e.target.value)}
                placeholder="이미지 ID"
              />
            </div>
          )}
          <textarea
            className={inputClass}
            rows={4}
            value={sendMessage}
            onChange={(e) => setSendMessage(e.target.value)}
            placeholder="메시지 내용"
            required
          />
          <button type="submit" className={primaryButtonClass} disabled={saving || !settings?.enabled}>
            {saving ? "발송 중…" : "발송"}
          </button>
          {!settings?.enabled ? (
            <p className="text-xs text-amber-600">마스터 스위치를 켠 뒤 발송할 수 있습니다.</p>
          ) : null}
        </form>
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">통계</h2>
        {stats ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <div className="rounded-lg bg-gray-50 p-3 text-sm">
              <p className="text-gray-500">성공</p>
              <p className="text-xl font-semibold text-gray-900">{stats.totalSent}</p>
              <p className="text-xs text-gray-400">7일 {stats.sentLast7Days}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-3 text-sm">
              <p className="text-gray-500">실패</p>
              <p className="text-xl font-semibold text-gray-900">{stats.totalFailed}</p>
              <p className="text-xs text-gray-400">7일 {stats.failedLast7Days}</p>
            </div>
            <div className="rounded-lg bg-gray-50 p-3 text-sm">
              <p className="text-gray-500">스킵</p>
              <p className="text-xl font-semibold text-gray-900">{stats.totalSkipped}</p>
            </div>
            <div className="sm:col-span-3">
              <p className="text-sm text-gray-600">용도별</p>
              <ul className="mt-2 flex flex-wrap gap-2 text-xs">
                {Object.entries(stats.byPurpose).map(([k, v]) => (
                  <li key={k} className="rounded bg-teal-50 px-2 py-1 text-teal-800">
                    {PURPOSE_LABELS[k] ?? k}: {v}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ) : (
          <p className="mt-4 text-sm text-gray-400">통계 없음</p>
        )}
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">발송 이력</h2>
        {logs.length === 0 ? (
          <p className="mt-4 text-sm text-gray-400">이력이 없습니다.</p>
        ) : (
          <ul className="mt-4 divide-y divide-gray-100">
            {logs.map((row) => (
              <li key={row.id} className="py-3 text-sm">
                <div className="flex flex-wrap items-baseline gap-2">
                  <span className="rounded bg-gray-100 px-2 py-0.5 text-xs">{row.status}</span>
                  <span className="rounded bg-teal-50 px-2 py-0.5 text-xs text-teal-700">
                    {row.purposeLabel || row.purpose}
                  </span>
                  <span className="text-gray-800">{row.toPhone}</span>
                  <span className="text-xs text-gray-400">{formatSmsTime(row.createdAt)}</span>
                </div>
                {row.contentPreview ? (
                  <p className="mt-1 line-clamp-2 text-xs text-gray-500">{row.contentPreview}</p>
                ) : null}
                {row.errorMessage ? (
                  <p className="mt-1 text-xs text-red-500">{row.errorMessage}</p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
        <div className="mt-4 flex items-center gap-2">
          <button
            type="button"
            className={buttonClass}
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            이전
          </button>
          <span className="text-xs text-gray-500">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            className={buttonClass}
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </button>
        </div>
      </section>
    </div>
  )
}
