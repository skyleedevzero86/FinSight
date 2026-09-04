"use client"

import { useEffect, useState } from "react"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import CommunityWriteButton from "@/components/community/CommunityWriteButton"
import { useAuthSession } from "@/components/AuthSessionProvider"
import type { BoardRow } from "@/data/communityBoardData"
import type { BoardPagination, BoardTypeCode } from "@/lib/boardApi"
import { unwrapApiData } from "@/lib/boardApi"
import { authHeadersJson } from "@/lib/finsightToken"
import { mapListToBoardRows } from "@/lib/finsightBoardServer"

type Props = {
  boardId: string
  caption: string
  basePath: string
  boardType: BoardTypeCode
  initialRows: BoardRow[]
  initialTotalCount: number
  initialTotalPages: number
  currentPage: number
  initialSearchType?: string
  initialSearchValue?: string
  writeRequireAdmin?: boolean
  showWriteButton?: boolean
}

export default function CommunityAuthAwareBoardList({
  boardId,
  caption,
  basePath,
  boardType,
  initialRows,
  initialTotalCount,
  initialTotalPages,
  currentPage,
  initialSearchType = "",
  initialSearchValue = "",
  writeRequireAdmin = false,
  showWriteButton = true,
}: Props) {
  const { user, ready } = useAuthSession()
  const [rows, setRows] = useState(initialRows)
  const [totalCount, setTotalCount] = useState(initialTotalCount)
  const [totalPages, setTotalPages] = useState(initialTotalPages)

  useEffect(() => {
    setRows(initialRows)
    setTotalCount(initialTotalCount)
    setTotalPages(initialTotalPages)
  }, [initialRows, initialTotalCount, initialTotalPages])

  useEffect(() => {
    if (!ready || !user) return
    let cancelled = false
    const run = async () => {
      const params = new URLSearchParams()
      params.set("boardType", boardType)
      params.set("page", String(Math.max(0, currentPage - 1)))
      params.set("size", "20")
      if (initialSearchValue.trim()) params.set("keyword", initialSearchValue.trim())
      try {
        const res = await fetch(`/api/v1/boards?${params.toString()}`, {
          headers: { Accept: "application/json", ...authHeadersJson() },
          cache: "no-store",
        })
        if (!res.ok || cancelled) return
        const payload: unknown = await res.json().catch(() => null)
        const data = unwrapApiData<BoardPagination>(payload)
        if (!data || cancelled) return
        setRows(mapListToBoardRows(data.content ?? [], basePath))
        setTotalCount(Number(data.totalElements) || 0)
        setTotalPages(Math.max(1, Number(data.totalPages) || 1))
      } catch {
        void 0
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [
    ready,
    user?.email,
    user?.role,
    boardType,
    basePath,
    currentPage,
    initialSearchValue,
  ])

  return (
    <CommunityBoardList
      boardId={boardId}
      caption={caption}
      basePath={basePath}
      totalCount={totalCount}
      currentPage={currentPage}
      totalPages={totalPages}
      rows={rows}
      initialSearchType={initialSearchType}
      initialSearchValue={initialSearchValue}
      showWriteButton={showWriteButton}
      writeButton={
        showWriteButton ? (
          <CommunityWriteButton
            href={`${basePath}/write`}
            requireAdmin={writeRequireAdmin}
          />
        ) : null
      }
    />
  )
}
