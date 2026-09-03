"use client"

import type { FormEvent, KeyboardEvent } from "react"
import { useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import type { BoardTypeCode } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import {
  applyMarkdownCommand,
  normalizeTags,
  splitTagInput,
  toolbarActions,
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
}

export default function CommunityBoardEditorForm({
  mode,
  boardType,
  basePath,
  boardId,
  initialTitle = "",
  initialContent = "",
  initialTags = "",
}: Props) {
  const router = useRouter()
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const [title, setTitle] = useState(initialTitle)
  const [content, setContent] = useState(initialContent)
  const [tagList, setTagList] = useState<string[]>(() =>
    initialTags ? splitTagInput(initialTags) : []
  )
  const [tagInput, setTagInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mobileTab, setMobileTab] = useState<"write" | "preview">("write")

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = "auto"
    el.style.height = `${Math.max(480, el.scrollHeight)}px`
  }, [content])

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

  function runCommand(command: MarkdownCommandId) {
    const el = textareaRef.current
    const selection = {
      start: el?.selectionStart ?? content.length,
      end: el?.selectionEnd ?? content.length,
    }
    const result = applyMarkdownCommand(command, content, selection)
    setContent(result.value)
    requestAnimationFrame(() => {
      const target = textareaRef.current
      if (!target) return
      target.focus()
      target.setSelectionRange(result.selectionStart, result.selectionEnd)
    })
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
    const body =
      mode === "create"
        ? JSON.stringify({ title: t, content: c, boardType, hashtags })
        : JSON.stringify({ title: t, content: c, hashtags })
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
          : "저장에 실패했습니다."
      setError(msg)
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

          <div className="fcb-md-editor__section fcb-md-editor__section--tight">
            <CommunityMarkdownToolbar actions={toolbarActions} onCommand={runCommand} />
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
              placeholder={"마크다운으로 글을 작성하세요.\n\n## 소제목\n본문을 입력합니다."}
              spellCheck={false}
            />
          </div>

          <div className="fcb-md-actions">
            <button
              type="button"
              className="fcb-md-action fcb-md-action--ghost"
              onClick={() => router.push(basePath)}
            >
              취소
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
