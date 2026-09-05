"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  createUlinkItem,
  deleteUlinkItem,
  fetchAdminUlinkItems,
  isUlinkImageType,
  isUlinkPolicyItem,
  updateUlinkItem,
  uploadUlinkImage,
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
  sectionCode: "FOOTER_TEXT" | "FOOTER_IMAGE"
  linkKind: "SERVICE" | "POLICY"
  linkName: string
  linkUrl: string
  linkTarget: string
  description: string
  imgPath: string
  sortOrder: string
  openYn: "Y" | "N"
}

const emptyForm: FormState = {
  sectionCode: "FOOTER_TEXT",
  linkKind: "SERVICE",
  linkName: "",
  linkUrl: "",
  linkTarget: "_self",
  description: "",
  imgPath: "",
  sortOrder: "",
  openYn: "Y",
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

function digitsOnly(value: string): string {
  return value.replace(/\D/g, "")
}

function sectionLabel(code: string | null): string {
  if (code === "FOOTER_IMAGE" || code === "FOOTER_SOCIAL") return "이미지"
  if (code === "FOOTER_TEXT" || code === "FOOTER_SERVICE" || code === "FOOTER_POLICY") return "텍스트"
  const found = ULINK_SECTIONS.find((s) => s.value === code)
  return found?.label ?? code ?? "-"
}

function toFormSection(code: string | null): "FOOTER_TEXT" | "FOOTER_IMAGE" {
  if (code === "FOOTER_IMAGE" || code === "FOOTER_SOCIAL") return "FOOTER_IMAGE"
  return "FOOTER_TEXT"
}

export default function AdminUlinkClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [page, setPage] = useState(0)
  const [sectionFilter, setSectionFilter] = useState("")
  const [rows, setRows] = useState<UlinkItem[]>([])
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
    if (fileInputRef.current) fileInputRef.current.value = ""
  }

  function startEdit(item: UlinkItem) {
    setEditingId(item.id)
    setForm({
      sectionCode: toFormSection(item.sectionCode),
      linkKind: isUlinkPolicyItem(item) ? "POLICY" : "SERVICE",
      linkName: item.linkName,
      linkUrl: item.linkUrl,
      linkTarget: item.linkTarget || "_self",
      description: item.description ?? "",
      imgPath: item.imgPath ?? "",
      sortOrder: item.sortOrder > 0 ? String(item.sortOrder) : "",
      openYn: item.openYn === "N" ? "N" : "Y",
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
    const result = await uploadUlinkImage(file)
    setUploading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setForm((f) => ({ ...f, imgPath: result.url }))
    setMessage("이미지를 업로드했습니다. 저장을 눌러 반영하세요.")
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.linkName.trim() || !form.linkUrl.trim()) {
      setError("링크 제목과 URL을 입력해 주세요.")
      return
    }
    if (form.sectionCode === "FOOTER_IMAGE" && !form.imgPath.trim()) {
      setError("이미지 유형은 파일을 업로드해 주세요.")
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
    const payload = {
      sectionCode: form.sectionCode,
      linkGroup:
        form.sectionCode === "FOOTER_TEXT" && form.linkKind === "POLICY" ? "POLICY" : undefined,
      linkName: form.linkName.trim(),
      linkUrl: form.linkUrl.trim(),
      linkTarget: form.linkTarget.trim() || undefined,
      description: form.description.trim() || undefined,
      imgPath:
        form.sectionCode === "FOOTER_IMAGE" ? form.imgPath.trim() || undefined : undefined,
      openYn: form.openYn,
      ...(sortOrder != null ? { sortOrder } : {}),
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

  const isImage = form.sectionCode === "FOOTER_IMAGE"

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">통합링크 관리</h1>
          <p className="mt-1 text-sm text-gray-500">
            링크 제목·순번·오픈 여부로 푸터 링크를 관리합니다. 오픈(Y)인 항목만 사이트에 표시됩니다.
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
              <option value="">전체 유형</option>
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
                <li key={item.id} className="flex gap-3 px-4 py-3">
                  <div className="flex h-12 w-10 shrink-0 flex-col items-center justify-center rounded bg-gray-50 text-xs font-semibold text-gray-700">
                    {item.sortOrder > 0 ? item.sortOrder : "-"}
                  </div>
                  {isUlinkImageType(item.sectionCode) && item.imgPath ? (
                    <div className="h-12 w-12 shrink-0 overflow-hidden rounded bg-gray-100">
                      <img
                        src={item.imgPath}
                        alt={item.linkName}
                        className="h-full w-full object-cover"
                      />
                    </div>
                  ) : null}
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600">
                        {sectionLabel(item.sectionCode)}
                      </span>
                      {isUlinkPolicyItem(item) ? (
                        <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">
                          정책
                        </span>
                      ) : null}
                      <span
                        className={
                          item.openYn === "Y"
                            ? "rounded bg-emerald-50 px-1.5 py-0.5 text-[11px] text-emerald-700"
                            : "rounded bg-gray-100 px-1.5 py-0.5 text-[11px] text-gray-600"
                        }
                      >
                        {item.openYn === "Y" ? "오픈" : "닫힘"}
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
                name="ulink-sort-order"
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
              표시 유형 *
              <select
                className={`${inputClass} mt-1`}
                name="ulink-section"
                autoComplete="off"
                value={form.sectionCode}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    sectionCode: e.target.value === "FOOTER_IMAGE" ? "FOOTER_IMAGE" : "FOOTER_TEXT",
                    imgPath: e.target.value === "FOOTER_IMAGE" ? f.imgPath : "",
                  }))
                }
              >
                {ULINK_SECTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs text-gray-600">
              링크 제목 *
              <input
                className={`${inputClass} mt-1`}
                name="ulink-link-name"
                autoComplete="off"
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
                name="ulink-link-url"
                autoComplete="off"
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
                name="ulink-link-target"
                autoComplete="off"
                value={form.linkTarget}
                onChange={(e) => setForm((f) => ({ ...f, linkTarget: e.target.value }))}
              >
                <option value="_self">현재 창 (_self)</option>
                <option value="_blank">새 창 (_blank)</option>
              </select>
            </label>
            <label className="block text-xs text-gray-600">
              오픈 여부
              <select
                className={`${inputClass} mt-1`}
                name="ulink-open-yn"
                autoComplete="off"
                value={form.openYn}
                onChange={(e) =>
                  setForm((f) => ({ ...f, openYn: e.target.value === "N" ? "N" : "Y" }))
                }
              >
                <option value="Y">오픈 · 사이트에 표시</option>
                <option value="N">닫힘 · 숨김</option>
              </select>
            </label>
            {!isImage ? (
              <>
                <label className="block text-xs text-gray-600">
                  링크 구분
                  <select
                    className={`${inputClass} mt-1`}
                    name="ulink-link-kind"
                    autoComplete="off"
                    value={form.linkKind}
                    onChange={(e) =>
                      setForm((f) => ({
                        ...f,
                        linkKind: e.target.value === "POLICY" ? "POLICY" : "SERVICE",
                      }))
                    }
                  >
                    <option value="SERVICE">서비스</option>
                    <option value="POLICY">정책</option>
                  </select>
                </label>
                <label className="block text-xs text-gray-600">
                  설명
                  <input
                    className={`${inputClass} mt-1`}
                    name="ulink-description"
                    autoComplete="off"
                    value={form.description}
                    onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                    maxLength={1000}
                  />
                </label>
              </>
            ) : (
              <div className="block text-xs text-gray-600">
                이미지 파일 *
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  name="ulink-image-file"
                  autoComplete="off"
                  className={`${inputClass} mt-1`}
                  disabled={uploading || saving}
                  onChange={(e) => void onFileChange(e)}
                />
                <p className="mt-1 text-[11px] text-gray-400">
                  {uploading ? "업로드 중…" : "푸터에 표시될 이미지를 업로드하세요."}
                </p>
                {form.imgPath ? (
                  <div className="mt-2 overflow-hidden rounded border border-gray-200 bg-gray-50 p-2">
                    <img
                      src={form.imgPath}
                      alt="미리보기"
                      className="mx-auto h-16 w-16 object-contain"
                    />
                  </div>
                ) : null}
              </div>
            )}
            <div className="flex gap-2 pt-1">
              <button
                type="submit"
                className={primaryButtonClass}
                disabled={saving || uploading}
              >
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
