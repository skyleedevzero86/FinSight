"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  INBOX_CATEGORIES,
  broadcastInbox,
  fetchAdminInboxPage,
  fetchInboxSettings,
  formatInboxTime,
  updateInboxSettings,
  type InboxCategory,
  type InboxItem,
  type InboxSettings,
} from "@/lib/inbox"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

export default function AdminNotificationsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<InboxItem[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const [category, setCategory] = useState<InboxCategory>("ADMIN")
  const [title, setTitle] = useState("")
  const [body, setBody] = useState("")
  const [linkUrl, setLinkUrl] = useState("")
  const [actorName, setActorName] = useState("FinSight")
  const [target, setTarget] = useState<"all" | "admins">("all")

  const [settings, setSettings] = useState<InboxSettings>({
    youtubeEnabled: true,
    newsEnabled: true,
    commentEnabled: true,
    qnaEnabled: true,
  })

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchAdminInboxPage({ page, size: 20 })
    setLoading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setRows(result.page.content)
    setTotalPages(Math.max(1, result.page.totalPages))
  }, [page])

  const loadSettings = useCallback(async () => {
    const s = await fetchInboxSettings()
    if (s) setSettings(s)
  }, [])

  useEffect(() => {
    if (!ready) return
    if (!allowed) {
      router.replace("/")
      return
    }
    void load()
    void loadSettings()
  }, [ready, allowed, router, load, loadSettings])

  async function onBroadcast(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) {
      setError("알림 문구를 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await broadcastInbox({
      category,
      title: title.trim(),
      body: body.trim() || undefined,
      linkUrl: linkUrl.trim() || undefined,
      actorName: actorName.trim() || "FinSight",
      allUsers: target === "all",
      adminsOnly: target === "admins",
    })
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(`${result.createdCount}건의 알림을 등록했습니다.`)
    setTitle("")
    setBody("")
    void load()
  }

  async function onSaveSettings() {
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await updateInboxSettings(settings)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setSettings(result.settings)
    setMessage("내 알림 수신 설정을 저장했습니다.")
  }

  if (!ready || !allowed) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-16 text-sm text-gray-500">권한 확인 중…</div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="text-2xl font-semibold text-gray-900">알림 관리</h1>
      <p className="mt-2 text-sm text-gray-500">
        인앱 알림을 등록하면 대상 사용자 헤더의 알림함에 표시됩니다. 카테고리는 유튜브·뉴스·댓글·QnA·관심종목·관리이며,
        관심종목은 회원가입 시 선택한 watchlist와 연동됩니다.
      </p>

      {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}
      {message ? <p className="mt-4 text-sm text-teal-700">{message}</p> : null}

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">알림 등록</h2>
        <form className="mt-4 grid gap-3" onSubmit={(e) => void onBroadcast(e)}>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">카테고리</span>
            <select
              className={inputClass}
              value={category}
              onChange={(e) => setCategory(e.target.value as InboxCategory)}
            >
              {INBOX_CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">대상</span>
            <select
              className={inputClass}
              value={target}
              onChange={(e) => setTarget(e.target.value as "all" | "admins")}
            >
              <option value="all">전체 사용자</option>
              <option value="admins">관리자만</option>
            </select>
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">표시 이름</span>
            <input
              className={inputClass}
              value={actorName}
              onChange={(e) => setActorName(e.target.value)}
              placeholder="FinSight"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">알림 문구</span>
            <input
              className={inputClass}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="새 공지가 등록되었습니다."
              required
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">부가 설명 (선택)</span>
            <textarea
              className={inputClass}
              rows={2}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-gray-600">이동 URL (선택)</span>
            <input
              className={inputClass}
              value={linkUrl}
              onChange={(e) => setLinkUrl(e.target.value)}
              placeholder="/admin/moderation"
            />
          </label>
          <div className="pt-1">
            <button type="submit" className={primaryButtonClass} disabled={saving}>
              {saving ? "등록 중…" : "알림 등록"}
            </button>
          </div>
        </form>
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-lg font-medium text-gray-900">내 알림 수신 설정</h2>
        <p className="mt-1 text-sm text-gray-500">
          유튜브·뉴스·댓글·QnA 수신 여부입니다. 관심종목은 마이페이지 watchlist를 따릅니다.
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {(
            [
              ["youtubeEnabled", "유튜브"],
              ["newsEnabled", "뉴스"],
              ["commentEnabled", "댓글"],
              ["qnaEnabled", "QnA"],
            ] as const
          ).map(([key, label]) => (
            <label key={key} className="flex items-center gap-2 text-sm text-gray-800">
              <input
                type="checkbox"
                checked={settings[key]}
                onChange={(e) =>
                  setSettings((prev) => ({ ...prev, [key]: e.target.checked }))
                }
              />
              {label}
            </label>
          ))}
        </div>
        <button
          type="button"
          className={`${buttonClass} mt-4`}
          disabled={saving}
          onClick={() => void onSaveSettings()}
        >
          설정 저장
        </button>
      </section>

      <section className="mt-8 rounded-xl border border-gray-200 bg-white p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-medium text-gray-900">최근 발송 알림</h2>
          <button type="button" className={buttonClass} onClick={() => void load()} disabled={loading}>
            새로고침
          </button>
        </div>
        {loading ? (
          <p className="mt-6 text-sm text-gray-400">불러오는 중…</p>
        ) : rows.length === 0 ? (
          <p className="mt-6 text-sm text-gray-400">등록된 알림이 없습니다.</p>
        ) : (
          <ul className="mt-4 divide-y divide-gray-100">
            {rows.map((row) => (
              <li key={row.id} className="py-3 text-sm">
                <div className="flex flex-wrap items-baseline gap-2">
                  <span className="rounded bg-teal-50 px-2 py-0.5 text-xs text-teal-700">
                    {INBOX_CATEGORIES.find((c) => c.value === row.category)?.label ?? row.category}
                  </span>
                  <span className="text-gray-800">{row.title}</span>
                  <span className="text-xs text-gray-400">{formatInboxTime(row.createdAt)}</span>
                </div>
                {row.body ? <p className="mt-1 text-xs text-gray-500">{row.body}</p> : null}
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
