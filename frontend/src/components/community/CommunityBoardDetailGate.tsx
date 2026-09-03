"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import type { BoardDetail } from "@/lib/boardApi"
import { unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import CommunityBoardDetail from "@/components/community/CommunityBoardDetail"

type Props = {
  boardId: number
  basePath: string
  initialDetail?: BoardDetail | null
  enableComments?: boolean
}

export default function CommunityBoardDetailGate({
  boardId,
  basePath,
  initialDetail = null,
  enableComments = false,
}: Props) {
  const [detail, setDetail] = useState<BoardDetail | null>(initialDetail)
  const [loading, setLoading] = useState(!initialDetail)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await fetch(`/api/v1/boards/${boardId}`, {
          headers: { Accept: "application/json", ...authHeadersJson() },
          cache: "no-store",
        })
        if (cancelled) return
        if (res.status === 403) {
          setDetail(null)
          setError("비공개 글은 작성자와 관리자만 볼 수 있습니다.")
          return
        }
        if (res.status === 404) {
          setDetail(null)
          setError("게시글을 찾을 수 없습니다.")
          return
        }
        if (!res.ok) {
          if (!initialDetail) setError("게시글을 불러오지 못했습니다.")
          return
        }
        const payload: unknown = await res.json().catch(() => null)
        const data = unwrapApiData<BoardDetail>(payload)
        if (data) setDetail(data)
        else if (!initialDetail) setError("게시글을 불러오지 못했습니다.")
      } catch {
        if (!initialDetail) setError("게시글을 불러오지 못했습니다.")
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [boardId, initialDetail])

  if (loading && !detail) {
    return <p className="text-sm text-gray-500">게시글을 불러오는 중…</p>
  }

  if (error && !detail) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-6 text-sm text-amber-900">
        {error}
        <div className="mt-4">
          <Link href={basePath} className="underline">
            목록으로
          </Link>
        </div>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="rounded-xl border border-gray-200 px-4 py-6 text-sm text-gray-600">
        게시글을 찾을 수 없습니다.
        <div className="mt-4">
          <Link href={basePath} className="underline">
            목록으로
          </Link>
        </div>
      </div>
    )
  }

  return (
    <CommunityBoardDetail
      detail={detail}
      basePath={basePath}
      enableComments={enableComments}
    />
  )
}
