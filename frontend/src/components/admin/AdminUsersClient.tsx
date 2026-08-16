"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { authProviderLabel } from "@/lib/authSession"
import { validatePassword } from "@/lib/registration"
import {
  canManageUsers,
  deleteAdminUser,
  fetchAdminUsers,
  resetAdminUserPassword,
  restoreAdminUser,
  suspendAdminUser,
  USER_ROLE_LABEL,
  USER_STATUS_LABEL,
  type AdminUser,
  type RevealField,
  type UserStatus,
} from "@/lib/adminUsers"

const inputClass =
  "rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

function statusClass(status: UserStatus): string {
  if (status === "APPROVED") return "bg-emerald-50 text-emerald-800"
  if (status === "SUSPENDED") return "bg-amber-50 text-amber-800"
  if (status === "WITHDRAWN") return "bg-gray-100 text-gray-600"
  if (status === "REJECTED") return "bg-red-50 text-red-700"
  return "bg-sky-50 text-sky-800"
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 16)
}

export default function AdminUsersClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<UserStatus | "">("")
  const [keyword, setKeyword] = useState("")
  const [reveal, setReveal] = useState<RevealField[]>([])
  const [rows, setRows] = useState<AdminUser[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [passwordUser, setPasswordUser] = useState<AdminUser | null>(null)
  const [newPassword, setNewPassword] = useState("")
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("")
  const [saving, setSaving] = useState(false)

  const allowed = Boolean(user && canManageUsers(user.role))

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchAdminUsers({
      page,
      size: 20,
      status,
      keyword,
      reveal,
    })
    setLoading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setRows(result.data.content)
    setTotalPages(Math.max(1, result.data.totalPages))
    setTotalElements(result.data.totalElements)
  }, [page, status, keyword, reveal])

  useEffect(() => {
    if (ready && !user) {
      router.replace("/login")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (ready && user && !canManageUsers(user.role)) {
      router.replace("/")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!allowed) return
    void load()
  }, [allowed, load])

  function toggleReveal(field: RevealField) {
    setPage(0)
    setReveal((prev) =>
      prev.includes(field) ? prev.filter((v) => v !== field) : [...prev, field],
    )
  }

  async function runAction(
    action: () => Promise<{ ok: true } | { ok: false; message: string }>,
    success: string,
  ) {
    setSaving(true)
    setError(null)
    setMessage(null)
    const result = await action()
    setSaving(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setMessage(success)
    await load()
  }

  async function onResetPassword() {
    if (!passwordUser) return
    const pwErr = validatePassword(newPassword)
    if (pwErr) {
      setError(pwErr)
      return
    }
    if (newPassword !== newPasswordConfirm) {
      setError("새 비밀번호 확인이 일치하지 않습니다.")
      return
    }
    await runAction(
      () => resetAdminUserPassword(passwordUser.id, newPassword, newPasswordConfirm),
      "비밀번호를 변경했습니다.",
    )
    setPasswordUser(null)
    setNewPassword("")
    setNewPasswordConfirm("")
  }

  if (!ready || !user || !allowed) {
    return <div className="min-h-[40vh]" />
  }

  return (
    <section className="w-full px-4 py-12 md:px-8 md:py-16">
      <div className="mx-auto w-full max-w-6xl">
        <h1 className="mb-2 text-2xl font-bold text-gray-900">사용자 관리</h1>
        <p className="mb-8 text-sm text-gray-500">
          민감 정보 컬럼을 선택하면 마스킹이 해제됩니다. 정지·사용자 탈퇴 계정은 복구하면 다시 로그인할 수 있고, 관리자 탈퇴는 DB에서 삭제합니다.
        </p>

        <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm md:p-6">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <label className="flex-1 text-sm text-gray-700">
              검색
              <input
                className={`${inputClass} mt-1 w-full`}
                value={keyword}
                onChange={(e) => {
                  setPage(0)
                  setKeyword(e.target.value)
                }}
                placeholder="아이디, 이메일, 닉네임"
              />
            </label>
            <label className="text-sm text-gray-700">
              상태
              <select
                className={`${inputClass} mt-1 block min-w-[8rem]`}
                value={status}
                onChange={(e) => {
                  setPage(0)
                  setStatus(e.target.value as UserStatus | "")
                }}
              >
                <option value="">전체</option>
                {Object.entries(USER_STATUS_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-4 flex flex-wrap gap-4 border-t border-gray-100 pt-4 text-sm text-gray-700">
            <span className="font-medium text-gray-900">민감 정보 복원</span>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={reveal.includes("username")}
                onChange={() => toggleReveal("username")}
              />
              아이디
            </label>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={reveal.includes("email")}
                onChange={() => toggleReveal("email")}
              />
              이메일
            </label>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={reveal.includes("phone")}
                onChange={() => toggleReveal("phone")}
              />
              전화번호
            </label>
          </div>

          {error ? (
            <p className="mt-4 text-sm text-red-600" role="alert">
              {error}
            </p>
          ) : null}
          {message ? (
            <p className="mt-4 text-sm text-emerald-700" role="status">
              {message}
            </p>
          ) : null}

          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200 text-gray-500">
                  <th className="px-2 py-2 font-medium">닉네임</th>
                  <th className="px-2 py-2 font-medium">아이디</th>
                  <th className="px-2 py-2 font-medium">이메일</th>
                  <th className="px-2 py-2 font-medium">전화</th>
                  <th className="px-2 py-2 font-medium">가입</th>
                  <th className="px-2 py-2 font-medium">상태</th>
                  <th className="px-2 py-2 font-medium">역할</th>
                  <th className="px-2 py-2 font-medium">최근 로그인</th>
                  <th className="px-2 py-2 font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={9} className="px-2 py-8 text-center text-gray-400">
                      불러오는 중...
                    </td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="px-2 py-8 text-center text-gray-400">
                      사용자가 없습니다.
                    </td>
                  </tr>
                ) : (
                  rows.map((row) => (
                    <tr key={row.id} className="border-b border-gray-100">
                      <td className="px-2 py-3 font-medium text-gray-900">{row.nickname}</td>
                      <td className="px-2 py-3 font-mono text-xs text-gray-700">
                        {row.username || "-"}
                      </td>
                      <td className="px-2 py-3 text-gray-700">{row.email || "-"}</td>
                      <td className="px-2 py-3 text-gray-700">{row.phoneNumber || "-"}</td>
                      <td className="px-2 py-3 text-gray-500">
                        {authProviderLabel(row.authProvider)}
                      </td>
                      <td className="px-2 py-3">
                        <span className={`rounded-full px-2 py-0.5 text-xs ${statusClass(row.status)}`}>
                          {USER_STATUS_LABEL[row.status]}
                        </span>
                      </td>
                      <td className="px-2 py-3 text-gray-700">{USER_ROLE_LABEL[row.role]}</td>
                      <td className="px-2 py-3 text-gray-500">{formatDate(row.lastLoginAt)}</td>
                      <td className="px-2 py-3">
                        <div className="flex flex-wrap gap-1">
                          {row.status === "SUSPENDED" ? (
                            <button
                              type="button"
                              disabled={saving}
                              className="rounded border border-emerald-200 px-2 py-1 text-xs text-emerald-800 hover:bg-emerald-50 disabled:opacity-50"
                              onClick={() => {
                                if (!window.confirm(`${row.nickname} 계정 정지를 해제할까요? 다시 로그인할 수 있습니다.`)) {
                                  return
                                }
                                void runAction(
                                  () => restoreAdminUser(row.id),
                                  "정지를 해제했습니다. 다시 로그인할 수 있습니다.",
                                )
                              }}
                            >
                              정지 해제
                            </button>
                          ) : row.status === "WITHDRAWN" ? (
                            <button
                              type="button"
                              disabled={saving}
                              className="rounded border border-emerald-200 px-2 py-1 text-xs text-emerald-800 hover:bg-emerald-50 disabled:opacity-50"
                              onClick={() => {
                                if (
                                  !window.confirm(
                                    `${row.nickname} 탈퇴 계정을 복구할까요? 복구하면 다시 로그인할 수 있습니다.`,
                                  )
                                ) {
                                  return
                                }
                                void runAction(
                                  () => restoreAdminUser(row.id),
                                  "탈퇴를 복구했습니다. 다시 로그인할 수 있습니다.",
                                )
                              }}
                            >
                              탈퇴 복구
                            </button>
                          ) : (
                            <button
                              type="button"
                              disabled={saving || row.role === "ADMIN" || row.id === user.id}
                              className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50 disabled:opacity-50"
                              onClick={() => {
                                if (!window.confirm(`${row.nickname} 계정을 정지할까요?`)) return
                                void runAction(
                                  () => suspendAdminUser(row.id),
                                  "계정을 정지했습니다.",
                                )
                              }}
                            >
                              정지
                            </button>
                          )}
                          <button
                            type="button"
                            disabled={saving || row.authProvider !== "WEB"}
                            className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50 disabled:opacity-50"
                            onClick={() => {
                              setError(null)
                              setPasswordUser(row)
                              setNewPassword("")
                              setNewPasswordConfirm("")
                            }}
                          >
                            비밀번호
                          </button>
                          <button
                            type="button"
                            disabled={saving || row.role === "ADMIN" || row.id === user.id}
                            className="rounded border border-red-200 px-2 py-1 text-xs text-red-700 hover:bg-red-50 disabled:opacity-50"
                            onClick={() => {
                              if (
                                !window.confirm(
                                  `${row.nickname} 계정을 DB에서 삭제할까요? 이 작업은 되돌릴 수 없습니다.`,
                                )
                              ) {
                                return
                              }
                              void runAction(
                                () => deleteAdminUser(row.id),
                                "사용자를 삭제했습니다.",
                              )
                            }}
                          >
                            탈퇴
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
            <p>총 {totalElements}명</p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={page <= 0 || loading}
                className="rounded border border-gray-300 px-3 py-1 disabled:opacity-40"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                이전
              </button>
              <span className="px-2 py-1">
                {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                disabled={page + 1 >= totalPages || loading}
                className="rounded border border-gray-300 px-3 py-1 disabled:opacity-40"
                onClick={() => setPage((p) => p + 1)}
              >
                다음
              </button>
            </div>
          </div>
        </div>
      </div>

      {passwordUser ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
            <h2 className="text-lg font-semibold text-gray-900">비밀번호 변경</h2>
            <p className="mt-1 text-sm text-gray-500">{passwordUser.nickname} 계정의 새 비밀번호를 입력하세요.</p>
            <div className="mt-4 space-y-3">
              <input
                type="password"
                className={`${inputClass} w-full`}
                placeholder="새 비밀번호"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              <input
                type="password"
                className={`${inputClass} w-full`}
                placeholder="새 비밀번호 확인"
                value={newPasswordConfirm}
                onChange={(e) => setNewPasswordConfirm(e.target.value)}
              />
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                className="rounded-md border border-gray-300 px-4 py-2 text-sm"
                onClick={() => setPasswordUser(null)}
              >
                취소
              </button>
              <button
                type="button"
                disabled={saving}
                className="rounded-md bg-finsight-primary px-4 py-2 text-sm text-white disabled:opacity-60"
                onClick={() => void onResetPassword()}
              >
                변경
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}
