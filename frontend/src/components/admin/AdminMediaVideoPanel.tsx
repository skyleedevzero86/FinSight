"use client"

import { useCallback, useEffect, useState, type ReactNode } from "react"
import { useRouter } from "next/navigation"
import { AdminMediaDetailDrawer, AdminMediaUrlModal } from "@/components/admin/AdminMediaDialogs"
import type { MediaNotice } from "@/components/admin/AdminMediaClient"
import {
  MEDIA_CATEGORIES,
  MEDIA_STATUS_OPTIONS,
  aiStatusLabel,
  buildPublishBody,
  categoryLabel,
  enrichAdminMediaVideos,
  fetchAdminMediaSources,
  fetchAdminMediaVideo,
  fetchAdminMediaVideos,
  formatEnrichMessage,
  formatSyncMessage,
  hideAdminMediaVideo,
  importAdminMediaUrls,
  importStatusLabel,
  mediaButtonClass,
  mediaFieldClass,
  mediaPrimaryClass,
  publishAdminMediaVideo,
  videoListDate,
  type AdminMediaSource,
  type AdminMediaVideo,
  type AdminMediaVideoDetail,
  type MediaCall,
} from "@/lib/adminMedia"

type Props = {
  onNotice: (notice: MediaNotice) => void
  onForbidden: () => void
}

export default function AdminMediaVideoPanel({ onNotice, onForbidden }: Props) {
  const router = useRouter()
  const [status, setStatus] = useState("")
  const [category, setCategory] = useState("")
  const [sourceId, setSourceId] = useState("")
  const [keywordInput, setKeywordInput] = useState("")
  const [keyword, setKeyword] = useState("")
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<AdminMediaVideo[]>([])
  const [sources, setSources] = useState<AdminMediaSource[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [selected, setSelected] = useState<number[]>([])
  const [urlOpen, setUrlOpen] = useState(false)
  const [drawerId, setDrawerId] = useState<number | null>(null)
  const [detail, setDetail] = useState<AdminMediaVideoDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

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
    const result = await fetchAdminMediaVideos({ page, importStatus: status, category, sourceId, keyword })
    setLoading(false)
    if (!result.ok) {
      fail(result)
      setRows([])
      return
    }
    setRows(result.data.content)
    setTotalPages(result.data.totalPages)
    setSelected([])
  }, [page, status, category, sourceId, keyword, fail])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    void fetchAdminMediaSources().then((result) => {
      if (result.ok) setSources(result.data)
    })
  }, [])

  useEffect(() => {
    if (drawerId === null) return
    let cancelled = false
    setDetailLoading(true)
    void fetchAdminMediaVideo(drawerId).then((result) => {
      if (cancelled) return
      setDetailLoading(false)
      if (!result.ok) {
        fail(result)
        setDrawerId(null)
        return
      }
      setDetail(result.data)
    })
    return () => {
      cancelled = true
    }
  }, [drawerId, fail])

  async function runEnrich() {
    if (busy) return
    setBusy(true)
    const result = await enrichAdminMediaVideos()
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    onNotice({ tone: "ok", text: formatEnrichMessage(result.data) })
    await load()
  }

  async function runImport(urls: string[], nextCategory: string) {
    if (busy) return
    setBusy(true)
    const result = await importAdminMediaUrls({ urls, category: nextCategory })
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    setUrlOpen(false)
    onNotice({ tone: "ok", text: formatSyncMessage(result.data) })
    setStatus("DRAFT")
    setPage(0)
    if (status === "DRAFT" && page === 0) await load()
  }

  async function publishOne(boardId: number) {
    const loaded = await fetchAdminMediaVideo(boardId)
    if (!loaded.ok) return loaded
    return publishAdminMediaVideo(boardId, buildPublishBody(loaded.data))
  }

  async function runRow(kind: "publish" | "hide", boardId: number) {
    if (busy) return
    setBusy(true)
    const result = kind === "hide" ? await hideAdminMediaVideo(boardId) : await publishOne(boardId)
    setBusy(false)
    if (!result.ok) {
      fail(result)
      return
    }
    onNotice({ tone: "ok", text: kind === "hide" ? "영상을 숨겼습니다." : "영상을 게시했습니다." })
    await load()
  }

  async function runBulk(kind: "publish" | "hide") {
    if (busy || selected.length === 0) return
    setBusy(true)
    let ok = 0
    let failed = 0
    for (const boardId of selected) {
      const result = kind === "hide" ? await hideAdminMediaVideo(boardId) : await publishOne(boardId)
      if (result.ok) ok += 1
      else {
        failed += 1
        if (result.forbidden || result.unauthorized) {
          fail(result)
          break
        }
      }
    }
    setBusy(false)
    onNotice({
      tone: failed > 0 && ok === 0 ? "error" : "ok",
      text: kind === "hide" ? `숨김 ${ok}건, 실패 ${failed}건` : `게시 ${ok}건, 실패 ${failed}건`,
    })
    await load()
  }

  const allChecked = rows.length > 0 && rows.every((row) => selected.includes(row.boardId))
  const draftOnly = status === "DRAFT" && !category && !sourceId && !keyword
  const emptyCopy = draftOnly
    ? "초안 영상이 없습니다."
    : status || category || sourceId || keyword
      ? "조건에 맞는 영상이 없습니다."
      : "영상이 없습니다."

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <form
          className="flex flex-wrap items-end gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            const next = keywordInput.trim()
            if (next === keyword && page === 0) {
              void load()
              return
            }
            setPage(0)
            setKeyword(next)
          }}
        >
          <Filter label="상태">
            <select className={mediaFieldClass} value={status} onChange={(event) => { setPage(0); setStatus(event.target.value) }}>
              {MEDIA_STATUS_OPTIONS.map((item) => (
                <option key={item.label} value={item.value}>{item.label}</option>
              ))}
            </select>
          </Filter>
          <Filter label="소스">
            <select className={mediaFieldClass} value={sourceId} onChange={(event) => { setPage(0); setSourceId(event.target.value) }}>
              <option value="">전체</option>
              {sources.map((source) => (
                <option key={source.id} value={String(source.id)}>{source.sourceValue}</option>
              ))}
            </select>
          </Filter>
          <Filter label="카테고리">
            <select className={mediaFieldClass} value={category} onChange={(event) => { setPage(0); setCategory(event.target.value) }}>
              <option value="">전체</option>
              {MEDIA_CATEGORIES.map((item) => (
                <option key={item.tab} value={item.tab}>{item.label}</option>
              ))}
            </select>
          </Filter>
          <Filter label="검색">
            <input className={mediaFieldClass} value={keywordInput} onChange={(event) => setKeywordInput(event.target.value)} placeholder="제목, 채널" />
          </Filter>
          <button type="submit" className={mediaButtonClass} disabled={loading}>검색</button>
        </form>
        <button type="button" className={mediaPrimaryClass} disabled={busy} onClick={() => setUrlOpen(true)}>
          URL로 가져오기
        </button>
      </div>
      <div className="flex flex-wrap gap-2">
        <button type="button" className={mediaButtonClass} disabled={busy || selected.length === 0} onClick={() => void runBulk("publish")}>
          {busy ? "처리 중" : "일괄 게시"}
        </button>
        <button type="button" className={mediaButtonClass} disabled={busy || selected.length === 0} onClick={() => void runBulk("hide")}>
          일괄 숨김
        </button>
      </div>
      <div className="overflow-x-auto border border-[#e7edf5] bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-[#e7edf5] text-gray-500">
            <tr>
              <th className="px-3 py-2">
                <input
                  type="checkbox"
                  checked={allChecked}
                  aria-label="현재 페이지 전체 선택"
                  onChange={(event) => setSelected(event.target.checked ? rows.map((row) => row.boardId) : [])}
                />
              </th>
              <th className="px-3 py-2 font-medium">썸네일</th>
              <th className="px-3 py-2 font-medium">제목</th>
              <th className="px-3 py-2 font-medium">소스</th>
              <th className="px-3 py-2 font-medium">카테고리</th>
              <th className="px-3 py-2 font-medium">상태</th>
              <th className="px-3 py-2 font-medium">AI 보강</th>
              <th className="px-3 py-2 font-medium">수집·게시</th>
              <th className="px-3 py-2 font-medium">동작</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td className="px-3 py-8 text-center text-gray-500" colSpan={9}>불러오는 중...</td></tr>
            ) : rows.length === 0 ? (
              <tr><td className="px-3 py-8 text-center text-gray-500" colSpan={9}>{emptyCopy}</td></tr>
            ) : rows.map((row) => (
              <tr key={row.boardId} className="border-b border-[#e7edf5]">
                <td className="px-3 py-2">
                  <input
                    type="checkbox"
                    checked={selected.includes(row.boardId)}
                    aria-label={`${row.title} 선택`}
                    onChange={(event) => {
                      setSelected((current) => event.target.checked
                        ? [...current, row.boardId]
                        : current.filter((id) => id !== row.boardId))
                    }}
                  />
                </td>
                <td className="px-3 py-2">
                  {row.thumbnailUrl ? (
                    <img src={row.thumbnailUrl} alt="" className="h-12 w-20 border border-[#e7edf5] object-cover" />
                  ) : (
                    <span className="inline-block h-12 w-20 border border-[#e7edf5] bg-[#f8fafc]" />
                  )}
                </td>
                <td className="max-w-xs px-3 py-2 text-gray-900">{row.title}</td>
                <td className="px-3 py-2">{row.channelTitle || row.sourceValue || "-"}</td>
                <td className="px-3 py-2">{categoryLabel(row.category)}</td>
                <td className="px-3 py-2">{importStatusLabel(row.importStatus)}</td>
                <td className={["px-3 py-2", row.aiStatus === "FAILED" ? "text-red-700" : ""].join(" ")}>
                  {aiStatusLabel(row.aiStatus)}
                </td>
                <td className="px-3 py-2 whitespace-nowrap">{videoListDate(row)}</td>
                <td className="px-3 py-2">
                  <div className="flex flex-wrap gap-1">
                    <button type="button" className={mediaButtonClass} disabled={busy || row.importStatus === "PUBLISHED"} onClick={() => void runRow("publish", row.boardId)}>게시</button>
                    <button type="button" className={mediaButtonClass} disabled={busy || row.importStatus === "HIDDEN"} onClick={() => void runRow("hide", row.boardId)}>숨김</button>
                    <button type="button" className={mediaButtonClass} disabled={busy} onClick={() => void runEnrich()}>AI 보강</button>
                    <button type="button" className={mediaButtonClass} onClick={() => { setDetail(null); setDrawerId(row.boardId) }}>상세</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center gap-2 text-sm text-gray-600">
        <button type="button" className={mediaButtonClass} disabled={page <= 0 || loading} onClick={() => setPage((current) => current - 1)}>이전</button>
        <span>{page + 1} / {totalPages}</span>
        <button type="button" className={mediaButtonClass} disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)}>다음</button>
      </div>
      {urlOpen ? (
        <AdminMediaUrlModal busy={busy} onClose={() => setUrlOpen(false)} onSubmit={(urls, nextCategory) => void runImport(urls, nextCategory)} />
      ) : null}
      {drawerId !== null ? (
        <AdminMediaDetailDrawer detail={detail} loading={detailLoading} onClose={() => setDrawerId(null)} />
      ) : null}
    </section>
  )
}

function Filter({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-xs text-gray-500">
      {label}
      {children}
    </label>
  )
}
