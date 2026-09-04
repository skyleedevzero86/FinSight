"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  createUlinkItem,
  deleteUlinkItem,
  fetchAdminUlinkItems,
  updateUlinkItem,
  ULINK_SECTIONS,
  type UlinkItem,
} from "@/lib/ulink"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type FormState = {
  domainId: string
  sectionCode: string
  linkGroup: string
  linkName: string
  linkUrl: string
  linkTarget: string
  description: string
}

const emptyForm: FormState = {
  domainId: "",
  sectionCode: "FOOTER_SERVICE",
  linkGroup: "",
  linkName: "",
  linkUrl: "",
  linkTarget: "_self",
  description: "",
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

function sectionLabel(code: string | null): string {
  const found = ULINK_SECTIONS.find((s) => s.value === code)
  return found?.label ?? code ?? "-"
}

export default function AdminUlinkClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [page, setPage] = useState(0)
  const [sectionFilter, setSectionFilter] = useState("")
  const [rows, setRows] = useState<UlinkItem[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchAdminUlinkItems({
      page,
      size: 20,
      sectionCode: sectionFilter || undefined,
    })
    setLoading(false)
    if (!result.ok) {
      setRows([])
      setTotalElements(0)
      setTotalPages(1)
      setError(result.message)
      return
    }
    setRows(result.data.content)
    setTotalPages(Math.max(1, result.data.totalPages))
    setTotalElements(result.data.totalElements)
  }, [page, sectionFilter])

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace("/login")
      return
    }
    if (!canManageUsers(user.role)) {
      router.replace("/")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!allowed) return
    void load()
  }, [allowed, load])

  function startCreate() {
    setEditingId(null)
    setForm(emptyForm)
    setMessage(null)
    setError(null)
  }

  function startEdit(item: UlinkItem) {
    setEditingId(item.id)
    setForm({
      domainId: item.domainId ?? "",
      sectionCode: item.sectionCode || "FOOTER_SERVICE",
      linkGroup: item.linkGroup ?? "",
      linkName: item.linkName,
      linkUrl: item.linkUrl,
      linkTarget: item.linkTarget || "_self",
      description: item.description ?? "",
    })
    setMessage(null)
    setError(null)
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.linkName.trim() || !form.linkUrl.trim()) {
      setError("표시명과 URL을 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const payload = {
      domainId: form.domainId.trim() || undefined,
      sectionCode: form.sectionCode.trim() || undefined,
      linkGroup: form.linkGroup.trim() || undefined,
      linkName: form.linkName.trim(),
      linkUrl: form.linkUrl.trim(),
      linkTarget: form.linkTarget.trim() || undefined,
      description: form.description.trim() || undefined,
    }
    const result = editingId
      ? await updateUlinkItem(editingId, payload)
      : await createUlinkItem(payload)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(editingId ? "통합링크를 수정했습니다." : "통합링크를 등록했습니다.")
    startCreate()
    await load()
  }

  async function onDelete(item: UlinkItem) {
    if (!window.confirm(`「${item.linkName}」 링크를 삭제할까요?`)) return
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await deleteUlinkItem(item.id)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    if (editingId === item.id) startCreate()
    setMessage("통합링크를 삭제했습니다.")
    await load()
  }

  if (!ready || !allowed) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-16 text-center text-gray-500">
        권한을 확인하는 중…
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">통합링크 관리</h1>
          <p className="mt-1 text-sm text-gray-500">
            푸터 서비스·정책·소셜 링크를 등록합니다. CMS에 항목이 없으면 기본 링크가 표시됩니다.
            소셜은 description에 FACEBOOK / INSTAGRAM / YOUTUBE / TWITTER 를 넣으세요.
          </p>
        </div>
        <button type="button" className={buttonClass} onClick={startCreate}>
          새 링크
        </button>
      </div>

      {error ? (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      ) : null}
      {message ? (
        <div className="mb-4 rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {message}
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="rounded border border-gray-200 bg-white">
          <div className="flex flex-wrap items-center gap-2 border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-800">목록 ({totalElements})</h2>
            <select
              className={`${inputClass} ml-auto w-auto`}
              value={sectionFilter}
              onChange={(e) => {
                setPage(0)
                setSectionFilter(e.target.value)
              }}
            >
              <option value="">전체 구역</option>
              {ULINK_SECTIONS.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void load()}>
              새로고침
            </button>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : rows.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">등록된 링크가 없습니다.</div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {rows.map((item) => (
                <li key={item.id} className="px-4 py-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600">
                      {sectionLabel(item.sectionCode)}
                    </span>
                    <span className="font-medium text-gray-900">{item.linkName}</span>
                  </div>
                  <p className="mt-0.5 truncate text-xs text-gray-500">{item.linkUrl}</p>
                  <p className="mt-0.5 text-[11px] text-gray-400">
                    {formatDate(item.updatedAt || item.createdAt)}
                  </p>
                  <div className="mt-2 flex gap-2">
                    <button type="button" className={buttonClass} onClick={() => startEdit(item)}>
                      수정
                    </button>
                    <button
                      type="button"
                      className={buttonClass}
                      disabled={saving}
                      onClick={() => void onDelete(item)}
                    >
                      삭제
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3 text-sm">
            <button
              type="button"
              className={buttonClass}
              disabled={page <= 0 || loading}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              이전
            </button>
            <span className="text-gray-500">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className={buttonClass}
              disabled={page + 1 >= totalPages || loading}
              onClick={() => setPage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </section>

        <section className="rounded border border-gray-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-gray-800">
            {editingId ? `수정 · ${editingId}` : "새 통합링크 등록"}
          </h2>
          <form className="space-y-3" onSubmit={(e) => void onSubmit(e)}>
            <label className="block text-xs text-gray-600">
              구역 *
              <select
                className={`${inputClass} mt-1`}
                value={form.sectionCode}
                onChange={(e) => setForm((f) => ({ ...f, sectionCode: e.target.value }))}
              >
                {ULINK_SECTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs text-gray-600">
              표시명 *
              <input
                className={`${inputClass} mt-1`}
                value={form.linkName}
                onChange={(e) => setForm((f) => ({ ...f, linkName: e.target.value }))}
                maxLength={200}
                required
              />
            </label>
            <label className="block text-xs text-gray-600">
              URL *
              <input
                className={`${inputClass} mt-1`}
                value={form.linkUrl}
                onChange={(e) => setForm((f) => ({ ...f, linkUrl: e.target.value }))}
                placeholder="/news 또는 https://..."
                maxLength={500}
                required
              />
            </label>
            <label className="block text-xs text-gray-600">
              링크 타겟
              <select
                className={`${inputClass} mt-1`}
                value={form.linkTarget}
                onChange={(e) => setForm((f) => ({ ...f, linkTarget: e.target.value }))}
              >
                <option value="_self">현재 창 (_self)</option>
                <option value="_blank">새 창 (_blank)</option>
              </select>
            </label>
            <label className="block text-xs text-gray-600">
              그룹명 (선택)
              <input
                className={`${inputClass} mt-1`}
                value={form.linkGroup}
                onChange={(e) => setForm((f) => ({ ...f, linkGroup: e.target.value }))}
                maxLength={100}
              />
            </label>
            <label className="block text-xs text-gray-600">
              설명 / 소셜 아이콘 키
              <input
                className={`${inputClass} mt-1`}
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="FACEBOOK | INSTAGRAM | YOUTUBE | TWITTER"
                maxLength={1000}
              />
            </label>
            <label className="block text-xs text-gray-600">
              도메인 ID (선택)
              <input
                className={`${inputClass} mt-1`}
                value={form.domainId}
                onChange={(e) => setForm((f) => ({ ...f, domainId: e.target.value }))}
                maxLength={32}
              />
            </label>
            <div className="flex gap-2 pt-1">
              <button type="submit" className={primaryButtonClass} disabled={saving}>
                {editingId ? "수정 저장" : "등록"}
              </button>
              {editingId ? (
                <button type="button" className={buttonClass} onClick={startCreate}>
                  취소
                </button>
              ) : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}
