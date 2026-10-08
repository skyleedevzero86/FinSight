"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import AdminDateField from "@/components/admin/AdminDateField"
import { FcbManageShell, FcbNumberPager } from "@/components/community/FcbManageShell"
import { canManageUsers } from "@/lib/adminUsers"
import {
  POPUP_DEFAULT_HEIGHT,
  POPUP_DEFAULT_WIDTH,
  createPopupItem,
  deletePopupItem,
  fetchAdminPopupItems,
  updatePopupItem,
  uploadPopupImage,
  type PopupItem,
} from "@/lib/popup"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

const buttonClass =
  "rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-800 hover:bg-gray-50 disabled:opacity-50"

const primaryButtonClass =
  "rounded bg-finsight-primary px-4 py-2 text-sm text-white hover:bg-finsight-primary/90 disabled:opacity-50"

type FormState = {
  title: string
  imgPath: string
  fileName: string
  fileUrl: string
  linkTarget: string
  noticeBegin: string
  noticeEnd: string
  stopTodayHide: "Y" | "N"
  noticeActive: "Y" | "N"
}

const emptyForm: FormState = {
  title: "",
  imgPath: "",
  fileName: "",
  fileUrl: "",
  linkTarget: "_blank",
  noticeBegin: "",
  noticeEnd: "",
  stopTodayHide: "Y",
  noticeActive: "Y",
}

export default function AdminPopupClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && canManageUsers(user.role))
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<PopupItem[]>([])
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
    let result = await fetchAdminPopupItems({ page, size: 20, activeOnly: false })
    if (!result.ok && /권한|로그인|인증/i.test(result.message)) {
      await new Promise((resolve) => setTimeout(resolve, 300))
      result = await fetchAdminPopupItems({ page, size: 20, activeOnly: false })
    }
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

  function startEdit(item: PopupItem) {
    setEditingId(item.id)
    setForm({
      title: item.title,
      imgPath: item.imgPath ?? "",
      fileName: item.fileName ?? "",
      fileUrl: item.fileUrl ?? "",
      linkTarget: item.linkTarget || "_blank",
      noticeBegin: item.noticeBegin ?? "",
      noticeEnd: item.noticeEnd ?? "",
      stopTodayHide: item.stopTodayHide === "Y" ? "Y" : "N",
      noticeActive: item.noticeActive === "N" ? "N" : "Y",
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
    const result = await uploadPopupImage(file)
    setUploading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setForm((f) => ({
      ...f,
      imgPath: result.url,
      fileName: result.fileName,
    }))
    setMessage("이미지를 업로드했습니다. 저장을 눌러 반영하세요.")
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) {
      setError("제목을 입력해 주세요.")
      return
    }
    if (!form.imgPath.trim()) {
      setError("팝업 이미지를 업로드해 주세요.")
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const payload = {
      title: form.title.trim(),
      imgPath: form.imgPath.trim() || undefined,
      fileName: form.fileName.trim() || undefined,
      fileUrl: form.fileUrl.trim() || undefined,
      linkTarget: form.linkTarget.trim() || undefined,
      noticeBegin: form.noticeBegin.trim() || undefined,
      noticeEnd: form.noticeEnd.trim() || undefined,
      widthSize: POPUP_DEFAULT_WIDTH,
      verticalSize: POPUP_DEFAULT_HEIGHT,
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

  const previewScale = 0.55
  const previewW = Math.round(POPUP_DEFAULT_WIDTH * previewScale)
  const previewH = Math.round(POPUP_DEFAULT_HEIGHT * previewScale)

  return (
    <FcbManageShell
      title="팝업 관리"
      description={`활성(Y)이고 노출 기간 안인 팝업이 사이트에 표시됩니다. 팝업 크기 ${POPUP_DEFAULT_WIDTH}×${POPUP_DEFAULT_HEIGHT}px · 「오늘 하루 보지 않기」는 브라우저에 저장됩니다.`}
      toolbar={
        <div className="mb-4 flex justify-end">
          <button type="button" className={buttonClass} onClick={startCreate}>
            새 팝업
          </button>
        </div>
      }
    >

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
        <section className="bbs bbs_list bbs_basic">
          <div className="bbs_leadin">
            <div className="bbs_count">
              <span className="list-count">
                전체 <strong>{totalElements}</strong>건
              </span>
              <span className="page-count">
                <b>{page + 1}</b> / {totalPages}page
              </span>
            </div>
            <button type="button" className={buttonClass} disabled={loading} onClick={() => void load()}>
              새로고침
            </button>
          </div>
          <div className="bbs_listing">
            <table className="table">
              <caption>팝업 목록</caption>
              <thead>
                <tr>
                  <th className="td_num">번호</th>
                  <th className="td_file">이미지</th>
                  <th className="td_subject">제목</th>
                  <th className="td_name">상태</th>
                  <th className="td_date">기간</th>
                  <th className="td_action">관리</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td className="td_subject" colSpan={6}>불러오는 중…</td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td className="td_subject" colSpan={6}>등록된 팝업이 없습니다.</td>
                  </tr>
                ) : (
                  rows.map((item, index) => (
                    <tr key={item.id}>
                      <td className="td_num">{page * 20 + index + 1}</td>
                      <td className="td_file">
                        {item.imgPath ? (
                          <img src={item.imgPath} alt="" className="mx-auto h-10 w-16 object-cover" />
                        ) : null}
                      </td>
                      <td className="td_subject">{item.title}</td>
                      <td className="td_name">{item.noticeActive === "Y" ? "활성" : "비활성"}</td>
                      <td className="td_date">{(item.noticeBegin || "-") + " ~ " + (item.noticeEnd || "-")}</td>
                      <td className="td_action">
                        <div className="flex flex-wrap justify-center gap-1">
                          <button type="button" className={buttonClass} onClick={() => startEdit(item)}>
                            수정
                          </button>
                          <button type="button" className={buttonClass} disabled={saving} onClick={() => void onDelete(item)}>
                            삭제
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <FcbNumberPager
            page={page + 1}
            totalPages={totalPages}
            disabled={loading}
            label="팝업 페이지"
            onPage={(next) => setPage(next - 1)}
          />
        </section>

        <section className="rounded border border-gray-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-gray-800">
            {editingId ? `수정 · ${editingId}` : "새 팝업 등록"}
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
              제목 *
              <input
                className={`${inputClass} mt-1`}
                name="popup-title"
                autoComplete="off"
                value={form.title}
                onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                maxLength={200}
                required
              />
            </label>
            <div className="block text-xs text-gray-600">
              이미지 파일 *
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className={`${inputClass} mt-1`}
                disabled={uploading || saving}
                onChange={(e) => void onFileChange(e)}
              />
              <p className="mt-1 text-[11px] text-gray-400">
                {uploading
                  ? "업로드 중…"
                  : form.fileName
                    ? `선택됨: ${form.fileName}`
                    : "이미지 파일을 선택하면 업로드·미리보기가 표시됩니다."}
              </p>
            </div>
            {form.imgPath ? (
              <div className="space-y-1">
                <p className="text-xs text-gray-600">
                  미리보기 ({POPUP_DEFAULT_WIDTH}×{POPUP_DEFAULT_HEIGHT}px ·{" "}
                  {Math.round(previewScale * 100)}% 축소)
                </p>
                <div
                  className="overflow-hidden rounded border border-gray-200 bg-white shadow-sm"
                  style={{ width: previewW, height: previewH }}
                >
                  <div className="flex h-8 items-center border-b border-gray-100 px-2">
                    <span className="truncate text-[11px] font-medium text-gray-800">
                      {form.title || "팝업 제목"}
                    </span>
                  </div>
                  <img
                    src={form.imgPath}
                    alt="팝업 미리보기"
                    className="h-[calc(100%-2rem)] w-full object-cover"
                  />
                </div>
              </div>
            ) : null}
            <label className="block text-xs text-gray-600">
              클릭 링크 URL
              <input
                className={`${inputClass} mt-1`}
                name="popup-link-url"
                autoComplete="off"
                value={form.fileUrl}
                onChange={(e) => setForm((f) => ({ ...f, fileUrl: e.target.value }))}
                placeholder="https://... (비우면 클릭 없음)"
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
              <AdminDateField
                label="노출 시작"
                value={form.noticeBegin}
                disabled={saving}
                onChange={(noticeBegin) => setForm((f) => ({ ...f, noticeBegin }))}
              />
              <AdminDateField
                label="노출 종료"
                value={form.noticeEnd}
                disabled={saving}
                onChange={(noticeEnd) => setForm((f) => ({ ...f, noticeEnd }))}
              />
            </div>
            <label className="block text-xs text-gray-600">
              게시 활성
              <select
                className={`${inputClass} mt-1`}
                value={form.noticeActive}
                onChange={(e) =>
                  setForm((f) => ({ ...f, noticeActive: e.target.value === "N" ? "N" : "Y" }))
                }
              >
                <option value="Y">사이트에 표시</option>
                <option value="N">숨김</option>
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
                <option value="Y">버튼 표시</option>
                <option value="N">버튼 숨김</option>
              </select>
            </label>
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
    </FcbManageShell>
  )
}
