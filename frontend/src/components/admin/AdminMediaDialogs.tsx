import { useState, type ReactNode } from "react"
import { toPrivacyEmbedUrl, YOUTUBE_EMBED_ALLOW } from "@/lib/liveVod"
import {
  MEDIA_CATEGORIES,
  MEDIA_SOURCE_TYPES,
  categoryLabel,
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
    <div className="fixed inset-0 z-[60] flex justify-end bg-black/40" onMouseDown={onClose}>
      <aside
        className="h-full w-full max-w-2xl overflow-y-auto border-l border-[#e7edf5] bg-white p-5"
        aria-label="영상 상세"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-end">
          <button type="button" className={mediaButtonClass} onClick={onClose}>
            닫기
          </button>
        </div>
        {loading || !detail ? (
          <p className="text-sm text-gray-500">불러오는 중...</p>
        ) : (
          <DetailBody detail={detail} />
        )}
      </aside>
    </div>
  )
}

function DetailBody({ detail }: { detail: AdminMediaVideoDetail }) {
  const titleText = detail.youtubeTitle || detail.title
  const embedSrc = detail.videoId ? toPrivacyEmbedUrl(detail.videoId) : ""
  const watchHref = detail.videoId ? `/live-vod/watch/${detail.videoId}` : ""
  const content = contentRow(detail)
  return (
    <div className="flex flex-col gap-4">
      {embedSrc ? (
        <div className="aspect-video w-full bg-black">
          <iframe
            className="h-full w-full"
            title={titleText}
            src={embedSrc}
            referrerPolicy="strict-origin-when-cross-origin"
            allow={YOUTUBE_EMBED_ALLOW}
            allowFullScreen
          />
        </div>
      ) : null}
      <DetailTable
        rows={[
          {
            label: "제목",
            value: watchHref ? (
              <a className="text-sky-700 underline" href={watchHref}>
                {titleText}
              </a>
            ) : (
              titleText
            ),
          },
          { label: "채널", value: detail.channelTitle || detail.sourceValue || "-" },
          { label: "카테고리", value: categoryLabel(detail.category) },
          { label: "상태", value: importStatusLabel(detail.importStatus) },
          ...(content ? [{ label: "내용", value: content }] : []),
        ]}
      />
    </div>
  )
}

function contentRow(detail: AdminMediaVideoDetail) {
  if (detail.summary || detail.editorComment || detail.keyPoints.length > 0) {
    return <EnrichmentBody detail={detail} />
  }
  if (detail.aiFailedAt) return "관리자에게 문의주세요."
  return null
}

function EnrichmentBody({ detail }: { detail: AdminMediaVideoDetail }) {
  return (
    <div className="flex flex-col gap-3">
      {detail.summary ? <EnrichmentBlock label="요약" text={detail.summary} /> : null}
      {detail.editorComment ? <EnrichmentBlock label="편집 코멘트" text={detail.editorComment} /> : null}
      {detail.keyPoints.length > 0 ? (
        <div>
          <p className="font-semibold text-gray-900">핵심 포인트</p>
          <ul className="mt-1 list-disc pl-4">
            {detail.keyPoints.map((point) => (
              <li key={point}>
                <LinkedText text={point} />
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  )
}

function EnrichmentBlock({ label, text }: { label: string; text: string }) {
  return (
    <div>
      <p className="font-semibold text-gray-900">{label}</p>
      <p className="mt-1">
        <LinkedText text={text} />
      </p>
    </div>
  )
}

function DetailTable({ rows }: { rows: { label: string; value: ReactNode }[] }) {
  return (
    <table className="w-full border-collapse text-left text-sm">
      <tbody>
        {rows.map((row) => (
          <tr key={row.label}>
            <th className="w-28 border border-[#e7edf5] bg-[#f6f8fb] px-4 py-3 align-top font-medium text-gray-500">
              {row.label}
            </th>
            <td className="border border-[#e7edf5] px-4 py-3 align-top text-gray-800">{row.value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function LinkedText({ text }: { text: string }) {
  const nodes: ReactNode[] = []
  const pattern = /https?:\/\/[^\s<>"']+/g
  let cursor = 0
  for (const found of text.matchAll(pattern)) {
    const raw = found[0]
    const start = found.index ?? 0
    const href = raw.replace(/[),.;]+$/g, "")
    const tail = raw.slice(href.length)
    if (start > cursor) nodes.push(text.slice(cursor, start))
    nodes.push(<TextLink key={`${start}-${href}`} href={href} label={readableUrl(href)} />)
    if (tail) nodes.push(tail)
    cursor = start + raw.length
  }
  if (cursor < text.length) nodes.push(text.slice(cursor))
  return <span className="whitespace-pre-wrap break-words">{nodes}</span>
}

function TextLink({ href, label }: { href: string; label?: string }) {
  return (
    <a className="break-all text-sky-700 underline" href={href} target="_blank" rel="noreferrer">
      {label ?? href}
    </a>
  )
}

function readableUrl(href: string) {
  try {
    return decodeURI(href)
  } catch {
    return href
  }
}
