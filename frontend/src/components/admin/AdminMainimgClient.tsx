"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import AdminDateField from "@/components/admin/AdminDateField"
import { canManageUsers } from "@/lib/adminUsers"
import {
  createMainimgItem,
  deleteMainimgItem,
  fetchAdminMainimgItems,
  resolveMainimgUrl,
  updateMainimgItem,
  uploadMainimgFile,
  type MainimgItem,
} from "@/lib/mainimg"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type FormState = {
  sortOrder: string
  imageName: string
  imageFile: string
  description: string
  linkUrl: string
  noticeBegin: string
  noticeEnd: string
  reflectYn: "Y" | "N"
}

const emptyForm: FormState = {
  sortOrder: "",
  imageName: "",
  imageFile: "",
  description: "",
  linkUrl: "",
  noticeBegin: "",
  noticeEnd: "",
  reflectYn: "Y",
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

function digitsOnly(value: string): string {
  return value.replace(/\D/g, "")
}

export default function AdminMainimgClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<MainimgItem[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
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
    if (fileInputRef.current) fileInputRef.current.value = ""
  }

  function startEdit(item: MainimgItem) {
    setEditingId(item.id)
    setForm({
      sortOrder: item.sortOrder > 0 ? String(item.sortOrder) : "",
      imageName: item.imageName,
      imageFile: resolveMainimgUrl(item),
      description: item.description ?? "",
      linkUrl: item.linkUrl ?? "",
      noticeBegin: item.noticeBegin ?? "",
      noticeEnd: item.noticeEnd ?? "",
      reflectYn: item.reflectYn === "N" ? "N" : "Y",
    })
    setMessage(null)
    setError(null)
    if (fileInputRef.current) fileInputRef.current.value = ""
  }

  async function onFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (!file.type.startsWith("image/")) {
      setError("이미지 파일만 업로드할 수 있습니다.")
      return
    }
    setUploading(true)
    setError(null)
    setMessage(null)
    const result = await uploadMainimgFile(file)
    setUploading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setForm((f) => ({
      ...f,
      imageFile: result.url,
    }))
    setMessage("이미지를 업로드했습니다. 저장을 눌러 반영하세요.")
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.imageName.trim()) {
      setError("제목을 입력해 주세요.")
      return
    }
    if (!form.imageFile.trim()) {
      setError("이미지를 업로드해 주세요.")
      return
    }
    const sortDigits = form.sortOrder.trim()
    let sortOrder: number | undefined
    if (sortDigits) {
      const n = Number(sortDigits)
      if (!Number.isInteger(n) || n < 1) {
        setError("순번은 1 이상의 숫자만 입력할 수 있습니다.")
        return
      }
      sortOrder = n
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const uploaded = form.imageFile.trim()
    const payload = {
      imageName: form.imageName.trim(),
      image: uploaded,
      imageFile: uploaded,
      description: form.description.trim() || undefined,
      linkUrl: form.linkUrl.trim() || undefined,
      noticeBegin: form.noticeBegin.trim() || undefined,
      noticeEnd: form.noticeEnd.trim() || undefined,
      reflectYn: form.reflectYn,
      ...(sortOrder != null ? { sortOrder } : {}),
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
    setEditingId(null)
    setForm(emptyForm)
    if (fileInputRef.current) fileInputRef.current.value = ""
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
    if (editingId === item.id) {
      setEditingId(null)
      setForm(emptyForm)
      if (fileInputRef.current) fileInputRef.current.value = ""
    }
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

  const previewUrl = form.imageFile

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">메인이미지 관리</h1>
          <p className="mt-1 text-sm text-gray-500">
            순번·제목·클릭 URL·이미지·설명·노출 기간을 관리합니다. 반영(Y)이고 기간 안인 항목만 메인에
            표시됩니다.
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
                    <div className="flex h-16 w-10 shrink-0 flex-col items-center justify-center rounded bg-gray-50 text-xs font-semibold text-gray-700">
                      {item.sortOrder > 0 ? item.sortOrder : "-"}
                    </div>
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
                        {item.description || item.linkUrl || item.id}
                      </p>
                      <p className="mt-0.5 text-[11px] text-gray-400">
                        {(item.noticeBegin || "-") + " ~ " + (item.noticeEnd || "-")}
                        {" · "}
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
          <form
            key={editingId ?? "create"}
            className="space-y-3"
            autoComplete="off"
            onSubmit={(e) => void onSubmit(e)}
          >
            <div aria-hidden="true" className="pointer-events-none absolute h-0 w-0 overflow-hidden opacity-0">
              <input type="text" name="username" tabIndex={-1} autoComplete="username" defaultValue="" />
              <input
                type="password"
                name="password"
                tabIndex={-1}
                autoComplete="new-password"
                defaultValue=""
              />
            </div>
            <label className="block text-xs text-gray-600">
              순번
              <input
                className={`${inputClass} mt-1`}
                name="mainimg-sort-order"
                autoComplete="off"
                value={form.sortOrder}
                onChange={(e) => setForm((f) => ({ ...f, sortOrder: digitsOnly(e.target.value) }))}
                inputMode="numeric"
                pattern="[0-9]*"
                placeholder="비우면 저장 시 다음 번호"
                maxLength={6}
              />
            </label>
            <label className="block text-xs text-gray-600">
              제목 *
              <input
                className={`${inputClass} mt-1`}
                name="mainimg-title"
                autoComplete="off"
                value={form.imageName}
                onChange={(e) => setForm((f) => ({ ...f, imageName: e.target.value }))}
                maxLength={200}
                required
              />
            </label>
            <label className="block text-xs text-gray-600">
              이미지 클릭 URL
              <input
                className={`${inputClass} mt-1`}
                name="mainimg-link-url"
                autoComplete="off"
                value={form.linkUrl}
                onChange={(e) => setForm((f) => ({ ...f, linkUrl: e.target.value }))}
                placeholder="https://... (비우면 클릭 없음)"
                maxLength={500}
              />
            </label>
            <div className="block text-xs text-gray-600">
              이미지 업로드 *
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                name="mainimg-image-file"
                autoComplete="off"
                className={`${inputClass} mt-1`}
                disabled={uploading || saving}
                onChange={(e) => void onFileChange(e)}
              />
              <p className="mt-1 text-[11px] text-gray-400">
                {uploading
                  ? "업로드 중…"
                  : previewUrl
                    ? "업로드된 이미지가 있습니다. 바꾸려면 파일을 다시 선택하세요."
                    : "이미지를 선택하면 업로드됩니다."}
              </p>
            </div>
            <label className="block text-xs text-gray-600">
              설명
              <textarea
                className={`${inputClass} mt-1 min-h-[72px]`}
                name="mainimg-description"
                autoComplete="off"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                maxLength={1000}
                placeholder="슬라이더 부제·설명"
              />
            </label>
            <div className="grid grid-cols-2 gap-3">
              <AdminDateField
                label="기간 시작"
                value={form.noticeBegin}
                disabled={saving || uploading}
                onChange={(noticeBegin) => setForm((f) => ({ ...f, noticeBegin }))}
              />
              <AdminDateField
                label="기간 종료"
                value={form.noticeEnd}
                disabled={saving || uploading}
                onChange={(noticeEnd) => setForm((f) => ({ ...f, noticeEnd }))}
              />
            </div>
            <label className="block text-xs text-gray-600">
              화면 반영
              <select
                className={`${inputClass} mt-1`}
                name="mainimg-reflect-yn"
                autoComplete="off"
                value={form.reflectYn}
                onChange={(e) =>
                  setForm((f) => ({ ...f, reflectYn: e.target.value === "N" ? "N" : "Y" }))
                }
              >
                <option value="Y">Y · 메인에 표시</option>
                <option value="N">N · 숨김</option>
              </select>
            </label>
            {previewUrl ? (
              <div className="overflow-hidden rounded border border-gray-200 bg-gray-50">
                <img src={previewUrl} alt="미리보기" className="h-40 w-full object-cover" />
              </div>
            ) : null}
            <div className="flex gap-2 pt-1">
              <button type="submit" className={primaryButtonClass} disabled={saving || uploading}>
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
