"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"
import {
  EMAIL_ACTOR_OPTIONS,
  EMAIL_PURPOSE_OPTIONS,
  EMAIL_STATUS_LABEL,
  fetchAdminEmailLog,
  fetchAdminEmailLogs,
  type AdminEmailLog,
  type EmailActorType,
  type EmailMailPurpose,
  type EmailStatus,
} from "@/lib/adminEmailLogs"

const inputClass =
  "rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

function statusClass(status: EmailStatus): string {
  if (status === "SENT" || status === "DELIVERED") return "bg-emerald-50 text-emerald-800"
  if (status === "FAILED" || status === "BOUNCED") return "bg-red-50 text-red-700"
  if (status === "PENDING") return "bg-amber-50 text-amber-800"
  return "bg-sky-50 text-sky-800"
}

function formatDate(value: string | null): string {
  if (!value) return "-"
  return value.replace("T", " ").slice(0, 19)
}

export default function AdminEmailLogsClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState("")
  const [status, setStatus] = useState<EmailStatus | "">("")
  const [purpose, setPurpose] = useState<EmailMailPurpose | "">("")
  const [actorType, setActorType] = useState<EmailActorType | "">("")
  const [requestIp, setRequestIp] = useState("")
  const [rows, setRows] = useState<AdminEmailLog[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<AdminEmailLog | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const allowed = Boolean(user && canManageUsers(user.role))

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await fetchAdminEmailLogs({
      page,
      size: 20,
      keyword,
      status,
      purpose,
      actorType,
      requestIp,
    })
    setLoading(false)
    if (!result.ok) {
      setError(result.message)
      return
    }
    setRows(result.data.content)
    setTotalPages(Math.max(1, result.data.totalPages))
    setTotalElements(result.data.totalElements)
  }, [page, keyword, status, purpose, actorType, requestIp])

  useEffect(() => {
    if (ready && !user) {
      router.replace("/login")
      return
    }
    if (ready && user && !canManageUsers(user.role)) {
      router.replace("/")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!ready || !allowed) return
    void load()
  }, [ready, allowed, load])

  async function openDetail(row: AdminEmailLog) {
    setDetailLoading(true)
    setSelected(row)
    const result = await fetchAdminEmailLog(row.id)
    setDetailLoading(false)
    if (result.ok) {
      setSelected(result.data)
    }
  }

  if (!ready || !allowed) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-16 text-sm text-gray-600">
        권한을 확인하는 중…
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">메일 발송 이력</h1>
          <p className="mt-1 text-sm text-gray-600">
            언제·누구에게·어떤 용도로 메일이 나갔는지 확인합니다. 비로그인 요청은 IP로 추적합니다.
          </p>
        </div>
        <p className="text-sm text-gray-500">총 {totalElements.toLocaleString()}건</p>
      </div>

      <div className="mb-4 flex flex-wrap gap-2">
        <input
          className={`${inputClass} min-w-[12rem] flex-1`}
          placeholder="수신·제목·IP·위치 검색"
          value={keyword}
          onChange={(e) => {
            setPage(0)
            setKeyword(e.target.value)
          }}
        />
        <input
          className={`${inputClass} w-40`}
          placeholder="IP 정확 일치"
          value={requestIp}
          onChange={(e) => {
            setPage(0)
            setRequestIp(e.target.value)
          }}
        />
        <select
          className={inputClass}
          value={status}
          onChange={(e) => {
            setPage(0)
            setStatus(e.target.value as EmailStatus | "")
          }}
        >
          <option value="">전체 상태</option>
          {(Object.keys(EMAIL_STATUS_LABEL) as EmailStatus[]).map((key) => (
            <option key={key} value={key}>
              {EMAIL_STATUS_LABEL[key]}
            </option>
          ))}
        </select>
        <select
          className={inputClass}
          value={purpose}
          onChange={(e) => {
            setPage(0)
            setPurpose(e.target.value as EmailMailPurpose | "")
          }}
        >
          {EMAIL_PURPOSE_OPTIONS.map((opt) => (
            <option key={opt.value || "all-purpose"} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <select
          className={inputClass}
          value={actorType}
          onChange={(e) => {
            setPage(0)
            setActorType(e.target.value as EmailActorType | "")
          }}
        >
          {EMAIL_ACTOR_OPTIONS.map((opt) => (
            <option key={opt.value || "all-actor"} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={() => void load()}
          className="rounded bg-finsight-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90"
        >
          새로고침
        </button>
      </div>

      {error ? (
        <p className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      ) : null}

      <div className="overflow-x-auto rounded border border-gray-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
            <tr>
              <th className="px-3 py-3">시각</th>
              <th className="px-3 py-3">상태</th>
              <th className="px-3 py-3">용도</th>
              <th className="px-3 py-3">주체</th>
              <th className="px-3 py-3">수신</th>
              <th className="px-3 py-3">IP / 위치</th>
              <th className="px-3 py-3">제목</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-gray-500">
                  불러오는 중…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-3 py-8 text-center text-gray-500">
                  이력이 없습니다.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={row.id}
                  className="cursor-pointer border-b border-gray-100 hover:bg-gray-50"
                  onClick={() => void openDetail(row)}
                >
                  <td className="whitespace-nowrap px-3 py-3 text-gray-700">
                    {formatDate(row.sentAt ?? row.createdAt)}
                  </td>
                  <td className="px-3 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs font-medium ${statusClass(row.status)}`}>
                      {row.statusLabel}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-gray-800">{row.purposeLabel}</td>
                  <td className="px-3 py-3 text-gray-700">{row.actorTypeLabel}</td>
                  <td className="px-3 py-3 text-gray-800">{row.recipient}</td>
                  <td className="px-3 py-3 text-gray-600">
                    <div>{row.requestIp || "-"}</div>
                    <div className="text-xs text-gray-400">{row.requestLocation || ""}</div>
                  </td>
                  <td className="max-w-[16rem] truncate px-3 py-3 text-gray-700" title={row.subject}>
                    {row.subject}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-between">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          className="rounded border border-gray-300 px-3 py-1.5 text-sm disabled:opacity-40"
        >
          이전
        </button>
        <span className="text-sm text-gray-600">
          {page + 1} / {totalPages}
        </span>
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
          className="rounded border border-gray-300 px-3 py-1.5 text-sm disabled:opacity-40"
        >
          다음
        </button>
      </div>

      {selected ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-lg bg-white p-6 shadow-xl">
            <div className="mb-4 flex items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">메일 발송 상세 #{selected.id}</h2>
                <p className="mt-1 text-sm text-gray-500">{formatDate(selected.sentAt ?? selected.createdAt)}</p>
              </div>
              <button
                type="button"
                className="rounded border border-gray-300 px-3 py-1 text-sm"
                onClick={() => setSelected(null)}
              >
                닫기
              </button>
            </div>
            {detailLoading ? (
              <p className="text-sm text-gray-500">상세 불러오는 중…</p>
            ) : (
              <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                <DetailItem label="상태" value={`${selected.statusLabel} (${selected.status})`} />
                <DetailItem label="용도" value={`${selected.purposeLabel} (${selected.purpose})`} />
                <DetailItem label="주체" value={`${selected.actorTypeLabel} (${selected.actorType})`} />
                <DetailItem label="템플릿" value={selected.templateType || "-"} />
                <DetailItem label="발신" value={selected.fromAddress || "-"} />
                <DetailItem label="수신" value={selected.recipient} />
                <DetailItem label="제목" value={selected.subject} />
                <DetailItem label="요청 IP" value={selected.requestIp || "-"} />
                <DetailItem label="요청 위치" value={selected.requestLocation || "-"} />
                <DetailItem label="User-Agent" value={selected.userAgent || "-"} />
                <DetailItem label="userId" value={selected.userId != null ? String(selected.userId) : "-"} />
                <DetailItem
                  label="actorUserId"
                  value={selected.actorUserId != null ? String(selected.actorUserId) : "-"}
                />
                <DetailItem label="관련 참조" value={selected.relatedRef || "-"} />
                <DetailItem label="오류" value={selected.errorMessage || "-"} />
                <div className="sm:col-span-2">
                  <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">본문 미리보기</dt>
                  <dd className="mt-1 whitespace-pre-wrap rounded border border-gray-200 bg-gray-50 p-3 text-gray-800">
                    {selected.bodyPreview || "-"}
                  </dd>
                </div>
              </dl>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</dt>
      <dd className="mt-1 break-all text-gray-900">{value}</dd>
    </div>
  )
}
