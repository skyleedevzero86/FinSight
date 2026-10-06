import { useState, type ReactNode } from "react"
import {
  MEDIA_CATEGORIES,
  MEDIA_SOURCE_TYPES,
  aiStatusLabel,
  categoryLabel,
  formatMediaDate,
  importStatusLabel,
  mediaButtonClass,
  mediaFieldClass,
  mediaPrimaryClass,
  type AdminMediaVideoDetail,
  type MediaSourceType,
} from "@/lib/adminMedia"

export function MediaDialog({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onMouseDown={onClose}>
      <div
        className="w-full max-w-lg border border-[#e7edf5] bg-white p-4"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-base font-semibold text-gray-900">{title}</h2>
          <button type="button" className={mediaButtonClass} onClick={onClose}>
            닫기
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function AdminMediaUrlModal({
  busy,
  onClose,
  onSubmit,
}: {
  busy: boolean
  onClose: () => void
  onSubmit: (urls: string[], category: string) => void
}) {
  const [text, setText] = useState("")
  const [category, setCategory] = useState("")
  const [error, setError] = useState<string | null>(null)

  function submit() {
    const urls = text
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
    if (urls.length === 0) {
      setError("URL을 한 줄에 하나씩 입력해 주세요.")
      return
    }
    onSubmit(urls, category)
  }

  return (
    <MediaDialog title="URL로 가져오기" onClose={onClose}>
      <p className="mb-3 text-sm text-gray-600">가져온 영상은 초안으로 목록에 추가됩니다.</p>
      <label className="mb-3 block text-sm text-gray-700">
        URL
        <textarea
          className={`${mediaFieldClass} mt-1 min-h-28`}
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="https://www.youtube.com/watch?v="
        />
      </label>
      <label className="mb-3 block text-sm text-gray-700">
        탭 카테고리
        <select className={`${mediaFieldClass} mt-1`} value={category} onChange={(event) => setCategory(event.target.value)}>
          <option value="">선택 안 함</option>
          {MEDIA_CATEGORIES.map((item) => (
            <option key={item.tab} value={item.tab}>
              {item.label}
            </option>
          ))}
        </select>
      </label>
      {error ? <p className="mb-3 text-sm text-red-700">{error}</p> : null}
      <button type="button" className={mediaPrimaryClass} disabled={busy} onClick={submit}>
        {busy ? "가져오는 중" : "가져오기"}
      </button>
    </MediaDialog>
  )
}

export function AdminMediaSourceModal({
  busy,
  onClose,
  onSubmit,
}: {
  busy: boolean
  onClose: () => void
  onSubmit: (body: { sourceType: MediaSourceType; sourceValue: string; category: string; active: boolean }) => void
}) {
  const [sourceType, setSourceType] = useState<MediaSourceType>("CHANNEL_HANDLE")
  const [sourceValue, setSourceValue] = useState("")
  const [category, setCategory] = useState("MARKET")
  const [active, setActive] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function submit() {
    if (!sourceValue.trim()) {
      setError("채널 또는 소스 값을 입력해 주세요.")
      return
    }
    onSubmit({ sourceType, sourceValue: sourceValue.trim(), category, active })
  }

  return (
    <MediaDialog title="소스 등록" onClose={onClose}>
      <p className="mb-3 text-sm text-gray-600">활성으로 두지 않으면 검토 대기로 등록됩니다.</p>
      <label className="mb-3 block text-sm text-gray-700">
        종류
        <select
          className={`${mediaFieldClass} mt-1`}
          value={sourceType}
          onChange={(event) => setSourceType(event.target.value as MediaSourceType)}
        >
          {MEDIA_SOURCE_TYPES.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </label>
      <label className="mb-3 block text-sm text-gray-700">
        채널명 또는 소스 값
        <input className={`${mediaFieldClass} mt-1`} value={sourceValue} onChange={(event) => setSourceValue(event.target.value)} />
      </label>
      <label className="mb-3 block text-sm text-gray-700">
        매핑 탭
        <select className={`${mediaFieldClass} mt-1`} value={category} onChange={(event) => setCategory(event.target.value)}>
          {MEDIA_CATEGORIES.map((item) => (
            <option key={item.tab} value={item.tab}>
              {item.label}
            </option>
          ))}
        </select>
      </label>
      <label className="mb-3 flex items-center gap-2 text-sm text-gray-700">
        <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
        등록과 동시에 활성화
      </label>
      {error ? <p className="mb-3 text-sm text-red-700">{error}</p> : null}
      <button type="button" className={mediaPrimaryClass} disabled={busy} onClick={submit}>
        {busy ? "등록 중" : "등록"}
      </button>
    </MediaDialog>
  )
}

export function AdminMediaDetailDrawer({
  detail,
  loading,
  onClose,
}: {
  detail: AdminMediaVideoDetail | null
  loading: boolean
  onClose: () => void
}) {
  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/40" onMouseDown={onClose}>
      <aside
        className="h-full w-full max-w-md overflow-y-auto border-l border-[#e7edf5] bg-white p-4"
        aria-label="영상 상세"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-base font-semibold text-gray-900">영상 상세</h2>
          <button type="button" className={mediaButtonClass} onClick={onClose}>
            닫기
          </button>
        </div>
        {loading || !detail ? (
          <p className="text-sm text-gray-500">불러오는 중...</p>
        ) : (
          <div className="flex flex-col gap-4 text-sm text-gray-800">
            <section>
              <h3 className="mb-2 font-semibold text-gray-900">원본</h3>
              <DetailLine label="제목" value={detail.youtubeTitle || detail.title} />
              <DetailLine label="채널" value={detail.channelTitle || detail.sourceValue || "-"} />
              <DetailLine label="카테고리" value={categoryLabel(detail.category)} />
              <DetailLine label="상태" value={importStatusLabel(detail.importStatus)} />
              <p className="mt-2 whitespace-pre-wrap text-gray-700">{detail.youtubeDescription || "원본 설명이 없습니다."}</p>
            </section>
            <section>
              <h3 className="mb-2 font-semibold text-gray-900">AI 보강</h3>
              <DetailLine label="상태" value={aiStatusLabel(detail.aiStatus)} />
              <DetailLine label="실패 시각" value={formatMediaDate(detail.aiFailedAt)} />
              <DetailLine label="요약" value={detail.summary || "-"} />
              <DetailLine label="편집 코멘트" value={detail.editorComment || "-"} />
              {detail.keyPoints.length > 0 ? (
                <ul className="mt-2 list-disc pl-4">
                  {detail.keyPoints.map((point) => (
                    <li key={point}>{point}</li>
                  ))}
                </ul>
              ) : (
                <p className="text-gray-500">핵심 포인트가 없습니다.</p>
              )}
            </section>
            {detail.videoId ? (
              <a
                className="text-sky-700 underline"
                href={`/live-vod/watch/${detail.videoId}`}
                target="_blank"
                rel="noreferrer"
              >
                /live-vod/watch/{detail.videoId} 미리보기
              </a>
            ) : null}
          </div>
        )}
      </aside>
    </div>
  )
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return (
    <p className="mt-1">
      <span className="text-gray-500">{label}</span> {value}
    </p>
  )
}
