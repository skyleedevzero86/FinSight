"use client"

import type { FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useState } from "react"
import type { BoardTypeCode } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"

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
  const [title, setTitle] = useState(initialTitle)
  const [content, setContent] = useState(initialContent)
  const [tags, setTags] = useState(initialTags)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function parseTags(raw: string): string[] {
    return raw
      .split(/[,\s]+/)
      .map((s) => s.trim().replace(/^#/, ""))
      .filter(Boolean)
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
    const hashtags = parseTags(tags)
    const path =
      mode === "create"
        ? "/api/v1/boards"
        : `/api/v1/boards/${boardId ?? ""}`
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
    <form className="mx-auto max-w-3xl space-y-4" onSubmit={onSubmit}>
      {error ? (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {error}
        </div>
      ) : null}
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="board-title">
          제목
        </label>
        <input
          id="board-title"
          className="w-full rounded border border-gray-300 px-3 py-2 text-[15px] outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"
          value={title}
          onChange={(ev) => setTitle(ev.target.value)}
          maxLength={200}
        />
      </div>
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="board-tags">
          태그 (쉼표로 구분)
        </label>
        <input
          id="board-tags"
          className="w-full rounded border border-gray-300 px-3 py-2 text-[15px] outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"
          value={tags}
          onChange={(ev) => setTags(ev.target.value)}
          placeholder="예: 주식, 포트폴리오"
        />
      </div>
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="board-content">
          내용 (마크다운)
        </label>
        <textarea
          id="board-content"
          className="min-h-[320px] w-full rounded border border-gray-300 px-3 py-2 font-mono text-[14px] leading-relaxed outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"
          value={content}
          onChange={(ev) => setContent(ev.target.value)}
        />
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-finsight-primary px-5 py-2.5 text-sm font-medium text-white hover:opacity-95 disabled:opacity-60"
        >
          {loading ? "저장 중…" : mode === "create" ? "등록" : "수정 완료"}
        </button>
        <button
          type="button"
          className="rounded border border-gray-300 px-5 py-2.5 text-sm text-gray-700 hover:bg-gray-50"
          onClick={() => router.push(basePath)}
        >
          취소
        </button>
      </div>
    </form>
  )
}
