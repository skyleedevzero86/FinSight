"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import NewsArticleComments from "@/components/news/NewsArticleComments"
import { newsImageSrc } from "@/lib/newsImage"
import {
  categoryLabel,
  fetchNewsAiDown,
  fetchStoredNews,
  fetchStoredNewsDetail,
  NEWS_ADMIN_NOTICE,
  sentimentExpression,
  type StoredNewsCard,
  type StoredNewsDetail,
} from "@/lib/publicNews"

const LOGO = "/finsight-logo.png"

function relativeTime(raw: string | null): string {
  if (!raw) return ""
  const date = new Date(raw.includes("T") ? raw : raw.replace(" ", "T"))
  const time = date.getTime()
  if (Number.isNaN(time)) return ""
  const minutes = Math.floor((Date.now() - time) / 60000)
  if (minutes < 1) return "방금 전"
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}일 전`
  return inputDate(date)
}

function inputDate(date: Date): string {
  const pad = (part: number) => String(part).padStart(2, "0")
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`
}

function inputStamp(raw: string | null): string {
  if (!raw) return ""
  const date = new Date(raw.includes("T") ? raw : raw.replace(" ", "T"))
  if (Number.isNaN(date.getTime())) return ""
  const pad = (part: number) => String(part).padStart(2, "0")
  return `입력 ${inputDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function sourceLabel(sourceUrl: string | null, provider: string | null): string {
  if (sourceUrl) {
    try {
      const host = new URL(sourceUrl).hostname.replace(/^www\./, "")
      if (host) return host
    } catch {
      return providerLabel(provider)
    }
  }
  return providerLabel(provider)
}

function providerLabel(provider: string | null): string {
  if (provider === "MARKETAUX") return "MarketAux"
  if (provider === "ALPHA_VANTAGE") return "Alpha Vantage"
  if (provider === "YAHOO_FINANCE") return "Yahoo Finance"
  return "FinSight"
}

function ArticleImage({ imageUrl }: { imageUrl: string | null }) {
  const [src, setSrc] = useState(newsImageSrc(imageUrl))
  return (
    <img
      src={src}
      alt=""
      className="mt-5 w-full bg-black object-cover"
      style={{ maxHeight: 460 }}
      onError={() => setSrc(LOGO)}
    />
  )
}

export default function StoredNewsDetail({ newsId }: { newsId: string }) {
  const [detail, setDetail] = useState<StoredNewsDetail | null>(null)
  const [fallbackImage, setFallbackImage] = useState<string | null>(null)
  const [related, setRelated] = useState<StoredNewsCard[]>([])
  const [missing, setMissing] = useState(false)
  const [failed, setFailed] = useState(false)
  const [aiDown, setAiDown] = useState(false)
  const [summaryOpen, setSummaryOpen] = useState(false)

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
    fetchStoredNews(12).then((rows) => {
      if (!alive || !rows) return
      const match = rows.find((row) => String(row.id) === newsId)
      if (match?.imageUrl) setFallbackImage(match.imageUrl)
      setRelated(rows.filter((row) => String(row.id) !== newsId).slice(0, 5))
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
  const category = detail.categories.find((code) => code && code !== "NONE" && code !== "GENERAL")
  const when = relativeTime(detail.publishedAt)
  const stamp = inputStamp(detail.publishedAt)

  return (
    <div className="mx-auto grid max-w-6xl gap-8 px-4 py-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      <article>
        <p className="mb-8">
          <Link
            href="/news"
            className="inline-block rounded border border-gray-300 bg-white px-4 py-2 text-sm text-gray-900 hover:border-[#1f444b] hover:text-[#1f444b]"
          >
            뉴스 목록
          </Link>
        </p>
        {aiDown || !detail.aiReady ? (
          <p role="alert" className="mb-4 border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-gray-900">
            {NEWS_ADMIN_NOTICE}
          </p>
        ) : null}
        {category ? (
          <p className="text-sm font-medium text-[#1f444b]">
            <Link href={`/news?category=${encodeURIComponent(category)}`}>{categoryLabel(category)}</Link>
          </p>
        ) : null}
        <h1 className="mt-2 text-2xl font-semibold leading-8 text-gray-900">{detail.title}</h1>
        <div className="mt-3 flex items-start justify-between gap-4">
          <button
            type="button"
            className="rounded border border-gray-300 px-3 py-1 text-sm"
            aria-expanded={summaryOpen}
            onClick={() => setSummaryOpen((open) => !open)}
          >
            요약
          </button>
          <div className="text-right">
            <p className="text-sm text-gray-500">
              {when ? <span>{when}</span> : null}
              {when && stamp ? <span> · </span> : null}
              {stamp ? <span>{stamp}</span> : null}
            </p>
            <div className="mt-2 flex items-center justify-end gap-3">
              <img src={LOGO} alt="" className="h-11 w-11 rounded-full border border-gray-200 object-cover" />
              <p className="text-sm font-medium text-gray-900">{sourceLabel(detail.sourceUrl, detail.provider)}</p>
            </div>
          </div>
        </div>
        {summaryOpen ? (
          <p className="mt-4 rounded bg-gray-50 px-3 py-3 text-sm leading-6 text-gray-800">{detail.summary}</p>
        ) : null}
        <ArticleImage imageUrl={detail.imageUrl || fallbackImage} />
        <p className="mt-6 whitespace-pre-wrap text-base leading-7 text-gray-900">{detail.body}</p>
        <p className="mt-6 text-right text-sm text-gray-800">감정: {sentiment ?? NEWS_ADMIN_NOTICE}</p>
        <NewsArticleComments newsId={newsId} />
      </article>
      <aside>
        <h2 className="text-base font-semibold text-gray-900">랭킹 뉴스</h2>
        <ol className="mt-3 space-y-3">
          {related.map((item, index) => (
            <li key={item.id} className="flex gap-2 text-sm">
              <span className="w-4 font-semibold text-[#1f444b]">{index + 1}</span>
              <span className="min-w-0 flex-1">
                <Link href={`/news/${item.id}`} className="line-clamp-2 text-gray-900">
                  {item.title}
                </Link>
                <span className="mt-1 block text-right text-xs text-gray-500">{relativeTime(item.publishedAt)}</span>
              </span>
            </li>
          ))}
        </ol>
      </aside>
    </div>
  )
}
