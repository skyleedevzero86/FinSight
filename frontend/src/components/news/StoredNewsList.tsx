"use client"

import { useSearchParams } from "next/navigation"
import { useEffect, useState } from "react"
import FinsightNewsNav from "@/components/news/FinsightNewsNav"
import SectionNewsBoard from "@/components/news/SectionNewsBoard"
import {
  fetchNewsAiDown,
  fetchStoredNews,
  NEWS_ADMIN_NOTICE,
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
  const [aiDown, setAiDown] = useState(false)
  const [day, setDay] = useState("")

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
    fetchNewsAiDown().then((down) => {
      if (alive) setAiDown(down)
    })
    return () => {
      alive = false
    }
  }, [])

  if (failed) {
    return <p className="px-4 py-10 text-center text-sm text-gray-700">{NEWS_ADMIN_NOTICE}</p>
  }
  if (!rows) {
    return <p className="px-4 py-10 text-center text-sm text-gray-500">뉴스를 불러오는 중입니다.</p>
  }

  const visible = rows.filter((item) => matchesCategory(item, category) && matchesDay(item, day))

  return (
    <>
      <div className="sn-top">
        <h1 className="sn-title">분야별 뉴스</h1>
        <label className="sn-date">
          날짜별 보기
          <input
            type="date"
            value={day}
            aria-label="날짜별 보기"
            onChange={(event) => setDay(event.target.value)}
          />
        </label>
      </div>
      <FinsightNewsNav />
      {aiDown || visible.some((item) => !item.aiReady) ? (
        <div className="sn-note">
          <AdminAlarm />
        </div>
      ) : null}
      <SectionNewsBoard key={`${category}:${day}`} articles={visible} />
    </>
  )
}

function matchesDay(item: StoredNewsCard, day: string): boolean {
  if (!day) return true
  return (item.publishedAt ?? "").startsWith(day)
}

function AdminAlarm() {
  return (
    <p role="alert" className="mb-4 border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-gray-900">
      {NEWS_ADMIN_NOTICE}
    </p>
  )
}

