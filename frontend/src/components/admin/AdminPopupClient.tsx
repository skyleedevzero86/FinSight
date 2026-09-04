"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  createPopupItem,
  deletePopupItem,
  fetchAdminPopupItems,
  updatePopupItem,
  type PopupItem,
} from "@/lib/popup"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type FormState = {
  domainId: string
  title: string
  imgPath: string
  fileUrl: string
  linkTarget: string
  noticeBegin: string
  noticeEnd: string
  widthSize: string
  verticalSize: string
  stopTodayHide: "Y" | "N"
  noticeActive: "Y" | "N"
}

const emptyForm: FormState = {
  domainId: "",
  title: "",
  imgPath: "",
  fileUrl: "",
  linkTarget: "_blank",
  noticeBegin: "",
  noticeEnd: "",
  widthSize: "420",
  verticalSize: "",
  stopTodayHide: "Y",
  noticeActive: "Y",
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

function toOptionalNumber(raw: string): number | undefined {
  const trimmed = raw.trim()
  if (!trimmed) return undefined
  const n = Number(trimmed)
  return Number.isFinite(n) ? n : undefined
}

export default function AdminPopupClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<PopupItem[]>([])
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
    const result = await fetchAdminPopupItems({ page, size: 20, activeOnly: false })
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
  }, [page])

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

  function startEdit(item: PopupItem) {
    setEditingId(item.id)
    setForm({
      domainId: item.domainId ?? "",
      title: item.title,
      imgPath: item.imgPath ?? "",
      fileUrl: item.fileUrl ?? "",
      linkTarget: item.linkTarget || "_blank",
      noticeBegin: item.noticeBegin ?? "",
      noticeEnd: item.noticeEnd ?? "",
      widthSize: item.widthSize != null ? String(item.widthSize) : "420",
      verticalSize: item.verticalSize != null ? String(item.verticalSize) : "",
      stopTodayHide: item.stopTodayHide === "Y" ? "Y" : "N",
      noticeActive: item.noticeActive === "N" ? "N" : "Y",
    })
    setMessage(null)
    setError(null)
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) {
      setError("제목을 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const payload = {
      domainId: form.domainId.trim() || undefined,
      title: form.title.trim(),
      imgPath: form.imgPath.trim() || undefined,
      fileUrl: form.fileUrl.trim() || undefined,
      linkTarget: form.linkTarget.trim() || undefined,
      noticeBegin: form.noticeBegin.trim() || undefined,
      noticeEnd: form.noticeEnd.trim() || undefined,
      widthSize: toOptionalNumber(form.widthSize),
      verticalSize: toOptionalNumber(form.verticalSize),
      stopTodayHide: form.stopTodayHide,
      noticeActive: form.noticeActive,
    }
    const result = editingId
      ? await updatePopupItem(editingId, payload)
      : await createPopupItem(payload)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(editingId ? "팝업을 수정했습니다." : "팝업을 등록했습니다.")
    startCreate()
    await load()
  }

  async function onDelete(item: PopupItem) {
    if (!window.confirm(`「${item.title}」 팝업을 삭제할까요?`)) return
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await deletePopupItem(item.id)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    if (editingId === item.id) startCreate()
    setMessage("팝업을 삭제했습니다.")
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
          <h1 className="text-2xl font-semibold text-gray-900">팝업 관리</h1>
          <p className="mt-1 text-sm text-gray-500">
            활성(Y)이고 노출 기간 안인 팝업이 사이트 전역에 표시됩니다. 「오늘 하루 보지 않기」는 브라우저에
            저장됩니다.
          </p>
        </div>
        <button type="button" className={buttonClass} onClick={startCreate}>
          새 팝업
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
          <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
            <h2 className="text-sm font-semibold text-gray-800">목록 ({totalElements})</h2>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void load()}>
              새로고침
            </button>
          </div>
          {loading ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">불러오는 중…</div>
          ) : rows.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-gray-500">등록된 팝업이 없습니다.</div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {rows.map((item) => (
                <li key={item.id} className="flex gap-3 px-4 py-3">
                  <div className="h-16 w-24 shrink-0 overflow-hidden rounded bg-gray-100">
                    {item.imgPath ? (
                      <img src={item.imgPath} alt={item.title} className="h-full w-full object-cover" />
                    ) : (
                      <div className="flex h-full items-center justify-center text-[11px] text-gray-400">
                        이미지 없음
                      </div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="truncate font-medium text-gray-900">{item.title}</span>
                      <span
                        className={
                          item.noticeActive === "Y"
                            ? "rounded bg-emerald-50 px-1.5 py-0.5 text-[11px] text-emerald-700"
                            : "rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600"
                        }
                      >
                        {item.noticeActive === "Y" ? "활성" : "비활성"}
                      </span>
                    </div>
                    <p className="mt-0.5 truncate text-xs text-gray-500">
                      {(item.noticeBegin || "-") + " ~ " + (item.noticeEnd || "-")}
                    </p>
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
            {editingId ? `수정 · ${editingId}` : "새 팝업 등록"}
          </h2>
          <form className="space-y-3" onSubmit={(e) => void onSubmit(e)}>
            <label className="block text-xs text-gray-600">
              제목 *
              <input
                className={`${inputClass} mt-1`}
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                maxLength={200}
                required
              />
            </label>
            <label className="block text-xs text-gray-600">
              이미지 URL
              <input
                className={`${inputClass} mt-1`}
                value={form.imgPath}
                onChange={(e) => setForm((f) => ({ ...f, imgPath: e.target.value }))}
                placeholder="https://..."
                maxLength={500}
              />
            </label>
            <label className="block text-xs text-gray-600">
              클릭 링크 URL
              <input
                className={`${inputClass} mt-1`}
                value={form.fileUrl}
                onChange={(e) => setForm((f) => ({ ...f, fileUrl: e.target.value }))}
                placeholder="https://..."
                maxLength={500}
              />
            </label>
            <label className="block text-xs text-gray-600">
              링크 타겟
              <select
                className={`${inputClass} mt-1`}
                value={form.linkTarget}
                onChange={(e) => setForm((f) => ({ ...f, linkTarget: e.target.value }))}
              >
                <option value="_blank">새 창 (_blank)</option>
                <option value="_self">현재 창 (_self)</option>
              </select>
            </label>
            <div className="grid grid-cols-2 gap-3">
              <label className="block text-xs text-gray-600">
                노출 시작 (yyyy-MM-dd)
                <input
                  className={`${inputClass} mt-1`}
                  value={form.noticeBegin}
                  onChange={(e) => setForm((f) => ({ ...f, noticeBegin: e.target.value }))}
                  placeholder="2026-09-01"
                  maxLength={20}
                />
              </label>
              <label className="block text-xs text-gray-600">
                노출 종료 (yyyy-MM-dd)
                <input
                  className={`${inputClass} mt-1`}
                  value={form.noticeEnd}
                  onChange={(e) => setForm((f) => ({ ...f, noticeEnd: e.target.value }))}
                  placeholder="2026-12-31"
                  maxLength={20}
                />
              </label>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <label className="block text-xs text-gray-600">
                가로 크기(px)
                <input
                  className={`${inputClass} mt-1`}
                  value={form.widthSize}
                  onChange={(e) => setForm((f) => ({ ...f, widthSize: e.target.value }))}
                />
              </label>
              <label className="block text-xs text-gray-600">
                세로 크기(px)
                <input
                  className={`${inputClass} mt-1`}
                  value={form.verticalSize}
                  onChange={(e) => setForm((f) => ({ ...f, verticalSize: e.target.value }))}
                />
              </label>
            </div>
            <label className="block text-xs text-gray-600">
              도메인 ID (선택)
              <input
                className={`${inputClass} mt-1`}
                value={form.domainId}
                onChange={(e) => setForm((f) => ({ ...f, domainId: e.target.value }))}
                maxLength={32}
              />
            </label>
            <label className="block text-xs text-gray-600">
              게시 활성
              <select
                className={`${inputClass} mt-1`}
                value={form.noticeActive}
                onChange={(e) =>
                  setForm((f) => ({ ...f, noticeActive: e.target.value === "N" ? "N" : "Y" }))
                }
              >
                <option value="Y">Y · 사이트에 표시</option>
                <option value="N">N · 숨김</option>
              </select>
            </label>
            <label className="block text-xs text-gray-600">
              오늘 하루 보지 않기
              <select
                className={`${inputClass} mt-1`}
                value={form.stopTodayHide}
                onChange={(e) =>
                  setForm((f) => ({ ...f, stopTodayHide: e.target.value === "Y" ? "Y" : "N" }))
                }
              >
                <option value="Y">Y · 버튼 표시</option>
                <option value="N">N · 버튼 숨김</option>
              </select>
            </label>
            {form.imgPath ? (
              <div className="overflow-hidden rounded border border-gray-200 bg-gray-50">
                <img src={form.imgPath} alt="미리보기" className="h-40 w-full object-cover" />
              </div>
            ) : null}
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
