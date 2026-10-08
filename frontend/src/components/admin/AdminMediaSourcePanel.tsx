"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { AdminMediaSourceModal } from "@/components/admin/AdminMediaDialogs"
import type { MediaNotice } from "@/components/admin/AdminMediaClient"
import {
  categoryLabel,
  createAdminMediaSource,
  fetchAdminMediaSources,
  formatMediaDate,
  formatSyncMessage,
  mediaButtonClass,
  mediaPrimaryClass,
  reviewStatusLabel,
  sourceTypeLabel,
  syncAdminMediaSource,
  syncAllAdminMediaSources,
  updateAdminMediaSourceState,
  type AdminMediaSource,
  type MediaCall,
  type MediaSourceType,
} from "@/lib/adminMedia"

type Props = {
  onNotice: (notice: MediaNotice) => void
  onForbidden: () => void
}

export default function AdminMediaSourcePanel({ onNotice, onForbidden }: Props) {
  const router = useRouter()
  const [rows, setRows] = useState<AdminMediaSource[]>([])
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)

  const fail = useCallback(
    (result: Extract<MediaCall<unknown>, { ok: false }>) => {
      if (result.forbidden) onForbidden()
      if (result.unauthorized) router.replace("/login")
      onNotice({ tone: "error", text: result.message })
    },
    [onForbidden, onNotice, router],
  )

  const load = useCallback(async () => {
    setLoading(true)
    const result = await fetchAdminMediaSources()
    setLoading(false)
    if (!result.ok) {
      fail(result)
      setRows([])
      return
    }
    setRows(result.data)
  }, [fail])

  useEffect(() => {
    void load()
  }, [load])

  async function runCreate(body: { sourceType: MediaSourceType; sourceValue: string; category: string; active: boolean }) {
    if (busy) return
    setBusy(true)
    const result = await createAdminMediaSource(body)
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    setCreateOpen(false)
    onNotice({ tone: "ok", text: body.active ? "소스를 활성으로 등록했습니다." : "소스를 검토 대기로 등록했습니다." })
    await load()
  }

  async function runState(source: AdminMediaSource, active: boolean) {
    if (busy) return
    setBusy(true)
    const result = await updateAdminMediaSourceState(source.id, active, !active)
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    onNotice({ tone: "ok", text: active ? "소스를 승인했습니다." : "소스를 거부했습니다." })
    await load()
  }

  async function runSync(sourceId: number) {
    if (busy) return
    setBusy(true)
    const result = await syncAdminMediaSource(sourceId)
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    onNotice({ tone: "ok", text: formatSyncMessage(result.data) })
    await load()
  }

  async function runSyncAll() {
    if (busy) return
    const confirmed = window.confirm("활성 소스를 모두 동기화합니다. YouTube API 할당량을 사용합니다. 계속할까요?")
    if (!confirmed) return
    setBusy(true)
    const result = await syncAllAdminMediaSources()
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    onNotice({ tone: "ok", text: formatSyncMessage(result.data) })
    await load()
  }

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" className={mediaButtonClass} disabled={busy} onClick={() => void runSyncAll()}>
          {busy ? "처리 중" : "전체 동기화"}
        </button>
        <button type="button" className={mediaPrimaryClass} disabled={busy} onClick={() => setCreateOpen(true)}>
          소스 등록
        </button>
      </div>
      <div className="bbs bbs_list bbs_basic">
        <div className="bbs_listing">
        <table className="table">
          <thead className="border-b border-[#e7edf5] text-gray-500">
            <tr>
              <th className="px-3 py-2 font-medium">채널명</th>
              <th className="px-3 py-2 font-medium">매핑된 탭</th>
              <th className="px-3 py-2 font-medium">상태</th>
              <th className="px-3 py-2 font-medium">마지막 동기화</th>
              <th className="px-3 py-2 font-medium">가져온 건수</th>
              <th className="px-3 py-2 font-medium">동작</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td className="px-3 py-8 text-center text-gray-500" colSpan={6}>불러오는 중...</td></tr>
            ) : rows.length === 0 ? (
              <tr><td className="px-3 py-8 text-center text-gray-500" colSpan={6}>등록된 소스가 없습니다.</td></tr>
            ) : rows.map((row) => (
              <tr key={row.id} className="border-b border-[#e7edf5]">
                <td className="px-3 py-2">
                  <div className="text-gray-900">{row.sourceValue}</div>
                  <div className="text-xs text-gray-500">{sourceTypeLabel(row.sourceType)}</div>
                </td>
                <td className="px-3 py-2">{categoryLabel(row.category)}</td>
                <td className="px-3 py-2">{reviewStatusLabel(row.reviewStatus)}</td>
                <td className="px-3 py-2 whitespace-nowrap">{formatMediaDate(row.lastSyncedAt)}</td>
                <td className="px-3 py-2">{row.totalVideoCount}</td>
                <td className="px-3 py-2">
                  <div className="flex flex-wrap gap-1">
                    {row.reviewStatus !== "ACTIVE" ? (
                      <button type="button" className={mediaButtonClass} disabled={busy} onClick={() => void runState(row, true)}>승인</button>
                    ) : null}
                    {row.reviewStatus !== "STOPPED" ? (
                      <button type="button" className={mediaButtonClass} disabled={busy} onClick={() => void runState(row, false)}>거부</button>
                    ) : null}
                    <button type="button" className={mediaButtonClass} disabled={busy} onClick={() => void runSync(row.id)}>
                      {busy ? "처리 중" : "동기화"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      </div>
      {createOpen ? (
        <AdminMediaSourceModal busy={busy} onClose={() => setCreateOpen(false)} onSubmit={(body) => void runCreate(body)} />
      ) : null}
    </section>
  )
}
