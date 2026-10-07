"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import {
  fetchNewsAiDown,
  fetchStoredNewsDetail,
  NEWS_ADMIN_NOTICE,
  sentimentExpression,
  type StoredNewsDetail,
} from "@/lib/publicNews"

export default function StoredNewsDetail({ newsId }: { newsId: string }) {
  const [detail, setDetail] = useState<StoredNewsDetail | null>(null)
  const [missing, setMissing] = useState(false)
  const [failed, setFailed] = useState(false)
  const [aiDown, setAiDown] = useState(false)

  useEffect(() => {
    let alive = true
    fetchNewsAiDown().then((down) => {
      if (alive) setAiDown(down)
    })
    fetchStoredNewsDetail(newsId).then((result) => {
      if (!alive) return
      if (result === "missing") {
        setMissing(true)
        return
      }
      if (!result) {
        setFailed(true)
        return
      }
      setDetail(result)
    })
    return () => {
      alive = false
    }
  }, [newsId])

  if (missing) {
    return <p className="px-4 py-10 text-center text-sm text-gray-700">뉴스를 찾을 수 없습니다.</p>
  }
  if (failed) {
    return <p className="px-4 py-10 text-center text-sm text-gray-700">{NEWS_ADMIN_NOTICE}</p>
  }
  if (!detail) {
    return <p className="px-4 py-10 text-center text-sm text-gray-500">뉴스를 불러오는 중입니다.</p>
  }

  const sentiment = sentimentExpression(detail.sentiment)

  return (
    <article className="mx-auto max-w-3xl px-4 py-6">
      <p className="mb-3 text-sm">
        <Link href="/news" className="text-sky-700 underline">
          뉴스 목록
        </Link>
      </p>
      {aiDown || !detail.aiReady ? (
        <p role="alert" className="mb-4 border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-gray-900">
          {NEWS_ADMIN_NOTICE}
        </p>
      ) : null}
      <h1 className="text-2xl font-semibold leading-8 text-gray-900">{detail.title}</h1>
      <p className="mt-3 text-sm leading-6 text-gray-600">{detail.summary}</p>
      <p className="mt-6 whitespace-pre-wrap text-base leading-7 text-gray-900">{detail.body}</p>
      <p className="mt-6 text-sm text-gray-800">감정: {sentiment ?? NEWS_ADMIN_NOTICE}</p>
    </article>
  )
}
