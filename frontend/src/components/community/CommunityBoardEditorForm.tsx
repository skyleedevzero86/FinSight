"use client"

import type { ClipboardEvent, FormEvent, KeyboardEvent } from "react"
import { useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import type { BoardTypeCode } from "@/lib/boardApi"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { authHeadersJson, readUsableAccessToken } from "@/lib/finsightToken"
import { prepareImageForUpload, uploadEditorAsset } from "@/lib/editorUpload"
import {
  applyMarkdownCommand,
  communityToolbarActions,
  insertTextAtSelection,
  normalizeTags,
  splitTagInput,
  type MarkdownCommandId,
} from "@/lib/communityMarkdown"
import CommunityMarkdownPreview from "@/components/community/markdown/CommunityMarkdownPreview"
import CommunityMarkdownToolbar from "@/components/community/markdown/CommunityMarkdownToolbar"

type Mode = "create" | "edit"

type Props = {
  mode: Mode
  boardType: BoardTypeCode
  basePath: string
  boardId?: number
  initialTitle?: string
  initialContent?: string
  initialTags?: string
  initialStatus?: "ACTIVE" | "PRIVATE"
  enableVisibility?: boolean
  enableCommentsHint?: boolean
}

export default function CommunityBoardEditorForm({
  mode,
  boardType,
  basePath,
  boardId,
  initialTitle = "",
  initialContent = "",
  initialTags = "",
  initialStatus = "ACTIVE",
  enableVisibility = false,
}: Props) {
  const router = useRouter()
  const { user, ready, hasToken } = useAuthSession()
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const imageInputRef = useRef<HTMLInputElement | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const [title, setTitle] = useState(initialTitle)
  const [content, setContent] = useState(initialContent)
  const [tagList, setTagList] = useState<string[]>(() =>
    initialTags ? splitTagInput(initialTags) : []
  )
  const [tagInput, setTagInput] = useState("")
  const [visibility, setVisibility] = useState<"ACTIVE" | "PRIVATE">(
    initialStatus === "PRIVATE" ? "PRIVATE" : "ACTIVE"
  )
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mobileTab, setMobileTab] = useState<"write" | "preview">("write")

  const contentRef = useRef(content)
  contentRef.current = content
  const uploadChainRef = useRef(Promise.resolve())

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = "auto"
    el.style.height = `${Math.max(480, el.scrollHeight)}px`
  }, [content])

  function applyContentResult(result: {
    value: string
    selectionStart: number
    selectionEnd: number
  }) {
    contentRef.current = result.value
    setContent(result.value)
    requestAnimationFrame(() => {
      const target = textareaRef.current
      if (!target) return
      target.focus()
      target.setSelectionRange(result.selectionStart, result.selectionEnd)
    })
  }

  function currentSelection() {
    const el = textareaRef.current
    const value = contentRef.current
    return {
      start: el?.selectionStart ?? value.length,
      end: el?.selectionEnd ?? value.length,
    }
  }

  async function uploadAndInsert(file: File, asFile: boolean) {
    const run = async () => {
      setUploading(true)
      setError(null)
      let previewUrl: string | null = null
      try {
        if (!ready) {
          setError("로그인 상태를 확인하는 중입니다. 잠시 후 다시 시도해 주세요.")
          return
        }
        if (!user || !hasToken || !readUsableAccessToken()) {
          setError("로그인이 필요합니다. 다시 로그인한 뒤 이미지를 올려 주세요.")
          return
        }

        const name = file.name || "file"
        const showImagePreview = !asFile && file.type.startsWith("image/")

        if (showImagePreview) {
          previewUrl = URL.createObjectURL(file)
          const selection = currentSelection()
          const value = contentRef.current
          const prefix =
            value.slice(0, selection.start).endsWith("\n") || selection.start === 0 ? "" : "\n"
          applyContentResult(
            insertTextAtSelection(value, selection, `${prefix}![${name}](${previewUrl})\n`),
          )
        }

        const uploadFile =
          asFile || !file.type.startsWith("image/")
            ? file
            : await prepareImageForUpload(file)
        const uploaded = await uploadEditorAsset(uploadFile, { allowFile: asFile })
        const finalName = uploaded.originalFileName || uploadFile.name || name
        if (previewUrl) {
          const next = contentRef.current
            .split(`![${name}](${previewUrl})`)
            .join(`![${finalName}](${uploaded.url})`)
          contentRef.current = next
          setContent(next)
        } else {
          const selection = currentSelection()
          const value = contentRef.current
          const insertion = `[${finalName}](${uploaded.url})`
          const prefix =
            value.slice(0, selection.start).endsWith("\n") || selection.start === 0 ? "" : "\n"
          applyContentResult(
            insertTextAtSelection(value, selection, `${prefix}${insertion}\n`),
          )
        }
      } catch (err) {
        if (previewUrl) {
          const pattern = new RegExp(`!?\\[[^\\]]*\\]\\(${escapeRegExp(previewUrl)}\\)\\n?`, "g")
          const next = contentRef.current.replace(pattern, "")
          contentRef.current = next
          setContent(next)
          URL.revokeObjectURL(previewUrl)
          previewUrl = null
        }
        const message =
          err instanceof Error && err.message.trim()
            ? err.message
            : "업로드에 실패했습니다."
        setError(message)
      } finally {
        if (previewUrl) {
          const toRevoke = previewUrl
          window.setTimeout(() => URL.revokeObjectURL(toRevoke), 2_000)
        }
        setUploading(false)
      }
    }

    const next = uploadChainRef.current.catch(() => undefined).then(run)
    uploadChainRef.current = next.catch(() => undefined)
    await next
  }

  function runCommand(command: string) {
    if (command === "image") {
      imageInputRef.current?.click()
      return
    }
    if (command === "file") {
      fileInputRef.current?.click()
      return
    }
    const result = applyMarkdownCommand(command as MarkdownCommandId, content, currentSelection())
    applyContentResult(result)
  }

  async function onPaste(e: ClipboardEvent<HTMLTextAreaElement>) {
    const clipboard = e.clipboardData
    if (!clipboard) return

    const imageFiles: File[] = []
    for (const item of Array.from(clipboard.items ?? [])) {
      if (!item.type.startsWith("image/")) continue
      const file = item.getAsFile()
      if (file) imageFiles.push(file)
    }
    for (const file of Array.from(clipboard.files ?? [])) {
      if (file.type.startsWith("image/") && !imageFiles.some((f) => f === file)) {
        imageFiles.push(file)
      }
    }

    if (imageFiles.length > 0) {
      e.preventDefault()
      const file = normalizePasteFile(imageFiles[0])
      void uploadAndInsert(file, false)
      return
    }

    const html = clipboard.getData("text/html")
    if (html) {
      const remote = extractImageUrlsFromHtml(html)
      if (remote.length > 0) {
        e.preventDefault()
        void (async () => {
          try {
            const file = await fetchRemoteImageAsFile(remote[0])
            await uploadAndInsert(file, false)
          } catch (err) {
            setError(err instanceof Error ? err.message : "이미지 붙여넣기에 실패했습니다.")
          }
        })()
      }
    }
  }

  function commitTagInput(raw: string = tagInput) {
    const next = normalizeTags([...tagList, ...splitTagInput(raw)])
    setTagList(next)
    setTagInput("")
  }

  function removeTag(tag: string) {
    setTagList((prev) => prev.filter((t) => t !== tag))
  }

  function onTagKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault()
      commitTagInput()
      return
    }
    if (e.key === "Backspace" && !tagInput && tagList.length > 0) {
      removeTag(tagList[tagList.length - 1])
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const t = title.trim()
    const c = content.trim()
    if (!t || !c) {
      setError("제목과 내용을 입력해 주세요.")
      return
    }
    setLoading(true)
    const hashtags = normalizeTags([...tagList, ...splitTagInput(tagInput)])
    const path = mode === "create" ? "/api/v1/boards" : `/api/v1/boards/${boardId ?? ""}`
    const status = enableVisibility ? visibility : undefined
    const body =
      mode === "create"
        ? JSON.stringify({
            title: t,
            content: c,
            boardType,
            hashtags,
            ...(status ? { status } : {}),
          })
        : JSON.stringify({
            title: t,
            content: c,
            hashtags,
            ...(status ? { status } : {}),
          })
    const res = await fetch(path, {
      method: mode === "create" ? "POST" : "PUT",
      headers: { "Content-Type": "application/json", ...authHeadersJson() },
      body,
    })
    let payload: unknown
    try {
      payload = await res.json()
    } catch {
      payload = null
    }
    setLoading(false)
    if (!res.ok) {
      const msg =
        payload && typeof payload === "object" && "message" in payload
          ? String((payload as { message?: string }).message)
          : ""
      setError(/[가-힣]/.test(msg) ? msg : "저장에 실패했습니다.")
      return
    }
    let newId = boardId
    if (mode === "create" && payload && typeof payload === "object") {
      const data = (payload as { data?: { id?: number } }).data
      if (data?.id != null) newId = data.id
    }
    if (newId != null) router.push(`${basePath}/${newId}`)
    else router.push(basePath)
    router.refresh()
  }

  return (
    <form className="fcb-md-workspace" onSubmit={onSubmit}>
      {error ? (
        <div role="alert" className="fcb-md-alert">
          {error}
        </div>
      ) : null}

      <div className="fcb-md-mobile-tabs" role="tablist" aria-label="작성 / 미리보기">
        <button
          type="button"
          role="tab"
          aria-selected={mobileTab === "write"}
          className={`fcb-md-mobile-tab${mobileTab === "write" ? " is-active" : ""}`}
          onClick={() => setMobileTab("write")}
        >
          작성
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mobileTab === "preview"}
          className={`fcb-md-mobile-tab${mobileTab === "preview" ? " is-active" : ""}`}
          onClick={() => setMobileTab("preview")}
        >
          미리보기
        </button>
      </div>

      <div className="fcb-md-split">
        <section
          className={`fcb-md-editor${mobileTab === "preview" ? " fcb-md-pane--hidden-mobile" : ""}`}
        >
          <div className="fcb-md-editor__section">
            <label className="fcb-md-field__label" htmlFor="board-title">
              제목
            </label>
            <input
              id="board-title"
              className="fcb-md-field__input fcb-md-field__input--title"
              value={title}
              onChange={(ev) => setTitle(ev.target.value)}
              maxLength={200}
              placeholder="제목을 입력하세요"
            />
          </div>

          <div className="fcb-md-editor__section fcb-md-editor__section--tight">
            <label className="fcb-md-field__label" htmlFor="board-tags">
              태그
            </label>
            <div className="fcb-md-chip-row">
              {tagList.map((tag) => (
                <button
                  key={tag}
                  type="button"
                  className="fcb-md-chip"
                  onClick={() => removeTag(tag)}
                  title="클릭하여 제거"
                >
                  #{tag}
                </button>
              ))}
              <input
                id="board-tags"
                className="fcb-md-tag-input"
                value={tagInput}
                onChange={(ev) => setTagInput(ev.target.value)}
                onKeyDown={onTagKeyDown}
                onBlur={() => {
                  if (tagInput.trim()) commitTagInput()
                }}
                placeholder={tagList.length ? "태그 추가" : "Enter로 태그 추가"}
              />
            </div>
          </div>

          {enableVisibility ? (
            <div className="fcb-md-editor__section fcb-md-editor__section--tight">
              <span className="fcb-md-field__label">공개 설정</span>
              <div className="fcb-md-visibility" role="radiogroup" aria-label="공개 설정">
                <label className="fcb-md-visibility__option">
                  <input
                    type="radio"
                    name="board-visibility"
                    checked={visibility === "ACTIVE"}
                    onChange={() => setVisibility("ACTIVE")}
                  />
                  <span>공개글</span>
                  <em>누구나 목록·상세에서 볼 수 있습니다</em>
                </label>
                <label className="fcb-md-visibility__option">
                  <input
                    type="radio"
                    name="board-visibility"
                    checked={visibility === "PRIVATE"}
                    onChange={() => setVisibility("PRIVATE")}
                  />
                  <span>비공개글</span>
                  <em>작성자와 관리자만 볼 수 있습니다</em>
                </label>
              </div>
            </div>
          ) : null}

          <div className="fcb-md-editor__section fcb-md-editor__section--tight">
            <CommunityMarkdownToolbar
              actions={communityToolbarActions}
              onCommand={runCommand}
              disabled={uploading || loading}
            />
            <input
              ref={imageInputRef}
              type="file"
              accept="image/*"
              className="sr-only"
              onChange={(ev) => {
                const file = ev.target.files?.[0]
                ev.target.value = ""
                if (file) void uploadAndInsert(file, false)
              }}
            />
            <input
              ref={fileInputRef}
              type="file"
              className="sr-only"
              onChange={(ev) => {
                const file = ev.target.files?.[0]
                ev.target.value = ""
                if (file) void uploadAndInsert(file, true)
              }}
            />
            {uploading ? (
              <p className="mt-2 text-sm text-slate-500">업로드 중…</p>
            ) : null}
          </div>

          <div className="fcb-md-editor__section">
            <label className="sr-only" htmlFor="board-content">
              내용 (마크다운)
            </label>
            <textarea
              id="board-content"
              ref={textareaRef}
              className="fcb-md-editor-surface"
              value={content}
              onChange={(ev) => setContent(ev.target.value)}
              onPaste={(ev) => void onPaste(ev)}
              placeholder={"마크다운으로 글을 작성하세요.\n\n## 소제목\n본문을 입력합니다.\n이미지 붙여넣기도 가능합니다."}
              spellCheck={false}
            />
          </div>

          <div className="fcb-md-actions">
            <button
              type="button"
              className="fcb-md-action fcb-md-action--ghost"
              onClick={() => {
                if (typeof window !== "undefined" && window.history?.length > 1) {
                  window.history.back()
                } else {
                  router.push(basePath)
                }
              }}
            >
              이전으로 가기
            </button>
            <button type="submit" disabled={loading} className="fcb-md-action fcb-md-action--primary">
              {loading ? "저장 중…" : mode === "create" ? "출간하기" : "수정 완료"}
            </button>
          </div>
        </section>

        <div
          className={`fcb-md-preview-wrap${mobileTab === "write" ? " fcb-md-pane--hidden-mobile" : ""}`}
        >
          <CommunityMarkdownPreview
            title={title}
            markdown={content}
            tags={normalizeTags([...tagList, ...splitTagInput(tagInput)])}
            eyebrow="Live Preview"
            showTableOfContents
          />
        </div>
      </div>
    </form>
  )
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

function normalizePasteFile(file: File): File {
  if (file.name && file.name !== "image.png" && file.name !== "blob") return file
  const ext = mimeToExt(file.type) || "png"
  return new File([file], `paste-${Date.now()}.${ext}`, {
    type: file.type || "image/png",
    lastModified: file.lastModified || Date.now(),
  })
}

function mimeToExt(mime: string): string | null {
  if (mime === "image/png") return "png"
  if (mime === "image/jpeg" || mime === "image/jpg") return "jpg"
  if (mime === "image/gif") return "gif"
  if (mime === "image/webp") return "webp"
  return null
}

function extractImageUrlsFromHtml(html: string): string[] {
  const urls: string[] = []
  const re = /<img[^>]+src=["']([^"']+)["']/gi
  let match: RegExpExecArray | null
  while ((match = re.exec(html)) != null) {
    const src = match[1]?.trim()
    if (!src) continue
    if (!urls.includes(src)) urls.push(src)
  }
  return urls.slice(0, 5)
}

async function fetchRemoteImageAsFile(src: string): Promise<File> {
  if (src.startsWith("data:image/")) {
    const res = await fetch(src)
    const blob = await res.blob()
    const ext = mimeToExt(blob.type) || "png"
    return new File([blob], `paste-${Date.now()}.${ext}`, { type: blob.type || "image/png" })
  }
  const res = await fetch(src, { mode: "cors", credentials: "omit", cache: "no-store" })
  if (!res.ok) {
    throw new Error("사이트 이미지를 가져오지 못했습니다. 이미지 파일을 직접 업로드해 주세요.")
  }
  const blob = await res.blob()
  if (!blob.type.startsWith("image/")) {
    throw new Error("이미지 형식이 아닙니다.")
  }
  const ext = mimeToExt(blob.type) || "png"
  return new File([blob], `web-${Date.now()}.${ext}`, { type: blob.type })
}

