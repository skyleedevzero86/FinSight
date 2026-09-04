"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  createMainimgItem,
  deleteMainimgItem,
  fetchAdminMainimgItems,
  resolveMainimgUrl,
  updateMainimgItem,
  type MainimgItem,
} from "@/lib/mainimg"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type FormState = {
  domainId: string
  imageName: string
  image: string
  imageFile: string
  description: string
  reflectYn: "Y" | "N"
}

const emptyForm: FormState = {
  domainId: "",
  imageName: "",
  image: "",
  imageFile: "",
  description: "",
  reflectYn: "Y",
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

export default function AdminMainimgClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<MainimgItem[]>([])
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
    const result = await fetchAdminMainimgItems({ page, size: 20, reflectOnly: false })
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

  function startEdit(item: MainimgItem) {
    setEditingId(item.id)
    setForm({
      domainId: item.domainId ?? "",
      imageName: item.imageName,
      image: item.image ?? "",
      imageFile: item.imageFile ?? "",
      description: item.description ?? "",
      reflectYn: item.reflectYn === "N" ? "N" : "Y",
    })
    setMessage(null)
    setError(null)
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.imageName.trim()) {
      setError("이미지명을 입력해 주세요.")
      return
    }
    if (!form.image.trim() && !form.imageFile.trim()) {
      setError("이미지 URL 또는 파일 경로를 입력해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const payload = {
      domainId: form.domainId.trim() || undefined,
      imageName: form.imageName.trim(),
      image: form.image.trim() || undefined,
      imageFile: form.imageFile.trim() || undefined,
      description: form.description.trim() || undefined,
      reflectYn: form.reflectYn,
    }
    const result = editingId
      ? await updateMainimgItem(editingId, payload)
      : await createMainimgItem(payload)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(editingId ? "메인이미지를 수정했습니다." : "메인이미지를 등록했습니다.")
    startCreate()
    await load()
  }

  async function onDelete(item: MainimgItem) {
    if (!window.confirm(`「${item.imageName}」 항목을 삭제할까요?`)) return
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await deleteMainimgItem(item.id)
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    if (editingId === item.id) startCreate()
    setMessage("메인이미지를 삭제했습니다.")
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
          <h1 className="text-2xl font-semibold text-gray-900">메인이미지 관리</h1>
          <p className="mt-1 text-sm text-gray-500">
            홈 히어로 슬라이더에 노출할 이미지를 등록·수정합니다. 반영(Y)인 항목만 메인에 표시됩니다.
          </p>
        </div>
        <button type="button" className={buttonClass} onClick={startCreate}>
          새 항목
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
            <div className="px-4 py-12 text-center text-sm text-gray-500">등록된 메인이미지가 없습니다.</div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {rows.map((item) => {
                const url = resolveMainimgUrl(item)
                return (
                  <li key={item.id} className="flex gap-3 px-4 py-3">
                    <div className="h-16 w-28 shrink-0 overflow-hidden rounded bg-gray-100">
                      {url ? (
                        <img src={url} alt={item.imageName} className="h-full w-full object-cover" />
                      ) : (
                        <div className="flex h-full items-center justify-center text-[11px] text-gray-400">
                          없음
                        </div>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="truncate font-medium text-gray-900">{item.imageName}</span>
                        <span
                          className={
                            item.reflectYn === "Y"
                              ? "rounded bg-emerald-50 px-1.5 py-0.5 text-[11px] text-emerald-700"
                              : "rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600"
                          }
                        >
                          {item.reflectYn === "Y" ? "반영" : "숨김"}
                        </span>
                      </div>
                      <p className="mt-0.5 truncate text-xs text-gray-500">
                        {item.description || item.id}
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
                )
              })}
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
            {editingId ? `수정 · ${editingId}` : "새 메인이미지 등록"}
          </h2>
          <form className="space-y-3" onSubmit={(e) => void onSubmit(e)}>
            <label className="block text-xs text-gray-600">
              이미지명 *
              <input
                className={`${inputClass} mt-1`}
                value={form.imageName}
                onChange={(e) => setForm((f) => ({ ...f, imageName: e.target.value }))}
                maxLength={200}
                required
              />
            </label>
            <label className="block text-xs text-gray-600">
              이미지 URL
              <input
                className={`${inputClass} mt-1`}
                value={form.image}
                onChange={(e) => setForm((f) => ({ ...f, image: e.target.value }))}
                placeholder="https://..."
                maxLength={500}
              />
            </label>
            <label className="block text-xs text-gray-600">
              이미지 파일 경로
              <input
                className={`${inputClass} mt-1`}
                value={form.imageFile}
                onChange={(e) => setForm((f) => ({ ...f, imageFile: e.target.value }))}
                placeholder="/uploads/... 또는 URL"
                maxLength={500}
              />
            </label>
            <label className="block text-xs text-gray-600">
              설명 (슬라이더 부제)
              <textarea
                className={`${inputClass} mt-1 min-h-[72px]`}
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
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
            <label className="block text-xs text-gray-600">
              화면 반영
              <select
                className={`${inputClass} mt-1`}
                value={form.reflectYn}
                onChange={(e) =>
                  setForm((f) => ({ ...f, reflectYn: e.target.value === "N" ? "N" : "Y" }))
                }
              >
                <option value="Y">Y · 메인에 표시</option>
                <option value="N">N · 숨김</option>
              </select>
            </label>
            {(form.image || form.imageFile) && (
              <div className="overflow-hidden rounded border border-gray-200 bg-gray-50">
                <img
                  src={form.image || form.imageFile}
                  alt="미리보기"
                  className="h-40 w-full object-cover"
                />
              </div>
            )}
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
