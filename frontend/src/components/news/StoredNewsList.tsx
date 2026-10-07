"use client"

import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { useEffect, useMemo, useState } from "react"
import { fetchWatchlist } from "@/lib/myAccount"
import {
  categoryLabel,
  fetchStoredNews,
  NEWS_ADMIN_NOTICE,
  recommendTargetCategories,
  sentimentExpression,
  type StoredNewsCard,
} from "@/lib/publicNews"

function matchesCategory(item: StoredNewsCard, category: string): boolean {
  if (!category || category === "ALL") return true
  return item.categories.some((code) => code.toUpperCase() === category)
}

export default function StoredNewsList() {
  const params = useSearchParams()
  const category = (params.get("category") ?? "ALL").trim().toUpperCase()
  const [rows, setRows] = useState<StoredNewsCard[] | null>(null)
  const [failed, setFailed] = useState(false)
  const [selected, setSelected] = useState<string[]>([])

  useEffect(() => {
    let alive = true
    fetchStoredNews(30).then((items) => {
      if (!alive) return
      if (!items) {
        setFailed(true)
        setRows([])
        return
      }
      setFailed(false)
      setRows(items)
    })
    fetchWatchlist()
      .then((categories) => {
        if (alive) setSelected(categories)
      })
      .catch(() => {
        if (alive) setSelected([])
      })
    return () => {
      alive = false
    }
  }, [])

  const recommendation = useMemo(
    () => recommendTargetCategories(rows ?? [], selected),
    [rows, selected],
  )

  if (failed) {
    return <p className="px-4 py-10 text-center text-sm text-gray-700">{NEWS_ADMIN_NOTICE}</p>
  }
  if (!rows) {
    return <p className="px-4 py-10 text-center text-sm text-gray-500">뉴스를 불러오는 중입니다.</p>
  }

  const visible = rows.filter((item) => matchesCategory(item, category))

  return (
    <section className="mx-auto max-w-3xl px-4 py-6">
      <h1 className="mb-4 text-xl font-semibold">뉴스</h1>
      <CategoryRecommendation recommendation={recommendation} />
      {visible.length === 0 ? (
        <p className="px-4 py-10 text-center text-sm text-gray-700">표시할 뉴스가 없습니다.</p>
      ) : (
      <ul className="divide-y divide-gray-200 border-y border-gray-200">
        {visible.map((item) => {
          const mood = sentimentExpression(item.sentiment)
          return (
            <li key={item.id}>
              <Link href={`/news/${item.id}`} className="block px-1 py-4 hover:bg-gray-50">
                <h2 className="text-base font-semibold leading-6 text-gray-900">{item.title}</h2>
                <p className="mt-1 text-sm leading-6 text-gray-600">{item.summary}</p>
                <p className="mt-2 text-sm text-gray-800">{mood ?? NEWS_ADMIN_NOTICE}</p>
              </Link>
            </li>
          )
        })}
      </ul>
      )}
    </section>
  )
}

function CategoryRecommendation({
  recommendation,
}: {
  recommendation: { mode: "selected" | "random"; categories: string[] }
}) {
  if (recommendation.categories.length === 0) return null
  const caption =
    recommendation.mode === "selected"
      ? "선택한 관심 카테고리와 기사 AI 분류를 맞춰 추천합니다."
      : "선택한 관심 카테고리가 없어 무작위로 추천합니다."
  return (
    <div className="mb-6">
      <h2 className="text-sm font-semibold text-gray-900">관심 카테고리 추천</h2>
      <p className="mt-1 text-sm text-gray-600">{caption}</p>
      <ul className="mt-3 flex flex-wrap gap-2">
        {recommendation.categories.map((code) => (
          <li key={code}>
            <Link href={`/news?category=${code}`} className="inline-block border border-gray-300 px-3 py-1 text-sm text-gray-800">
              {categoryLabel(code)}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
