"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { fetchStoredNews, type StoredNewsCard } from "@/lib/publicNews"

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
  return `${days}일 전`
}

function marketAuxNews(items: StoredNewsCard[]): StoredNewsCard[] {
  return items.filter((item) => !item.provider || item.provider === "MARKETAUX")
}

export default function OnAirSchedule() {
  const [articles, setArticles] = useState<StoredNewsCard[]>([])
  const [cardsPerView, setCardsPerView] = useState(4)
  const [currentPage, setCurrentPage] = useState(0)

  useEffect(() => {
    let alive = true
    fetchStoredNews(8).then((items) => {
      if (alive) setArticles(marketAuxNews(items ?? []))
    })
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    const updateCardsPerView = () => {
      if (window.innerWidth < 768) {
        setCardsPerView(1)
        return
      }
      if (window.innerWidth < 1200) {
        setCardsPerView(2)
        return
      }
      setCardsPerView(4)
    }

    updateCardsPerView()
    window.addEventListener("resize", updateCardsPerView)
    return () => window.removeEventListener("resize", updateCardsPerView)
  }, [])

  const pages = useMemo(() => {
    const grouped: StoredNewsCard[][] = []
    for (let i = 0; i < articles.length; i += cardsPerView) {
      grouped.push(articles.slice(i, i + cardsPerView))
    }
    return grouped
  }, [articles, cardsPerView])

  useEffect(() => {
    setCurrentPage((prev) => Math.min(prev, Math.max(pages.length - 1, 0)))
  }, [pages.length])

  useEffect(() => {
    if (pages.length <= 1) return
    const timer = setInterval(() => {
      setCurrentPage((prev) => (prev + 1) % pages.length)
    }, 4500)
    return () => clearInterval(timer)
  }, [pages.length])

  const newestId = articles[0]?.id
  const goPrev = () => setCurrentPage((prev) => (prev - 1 + pages.length) % pages.length)
  const goNext = () => setCurrentPage((prev) => (prev + 1) % pages.length)

  return (
    <section className="bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 md:px-8">
        <div className="flex items-center justify-between mb-4">
          <h3 data-popup-anchor="popular-news" className="text-2xl md:text-3xl font-bold">
            인기뉴스
          </h3>
          <div className="flex gap-2">
            <button type="button" onClick={goPrev} className="p-1 rounded-full transition bg-white hover:bg-gray-100 text-gray-700" aria-label="이전 뉴스">
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button type="button" onClick={goNext} className="p-1 rounded-full transition bg-white hover:bg-gray-100 text-gray-700" aria-label="다음 뉴스">
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
        {articles.length === 0 ? <p className="text-sm text-gray-600">표시할 뉴스가 없습니다.</p> : null}
        <div className="relative">
          <div className="overflow-hidden">
            <div className="flex transition-transform duration-700 ease-in-out" style={{ transform: `translateX(-${currentPage * 100}%)` }}>
              {pages.map((page, pageIndex) => (
                <div key={pageIndex} className="w-full shrink-0">
                  <div className="grid gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-4">
                    {page.map((item) => (
                      <NewsCard key={item.id} item={item} newest={item.id === newestId} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
          {pages.length > 1 ? (
            <div className="mt-4 flex items-center justify-center gap-2">
              {pages.map((_, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => setCurrentPage(index)}
                  className={`h-2 rounded-full transition-all ${index === currentPage ? "w-6 bg-finsight-primary" : "w-2 bg-gray-300"}`}
                  aria-label={`${index + 1}번 인기뉴스 페이지로 이동`}
                />
              ))}
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}

function NewsCard({ item, newest }: { item: StoredNewsCard; newest: boolean }) {
  return (
    <Link
      href={`/news/${item.id}`}
      className={`flex items-start justify-between gap-3 p-4 rounded-lg transition ${newest ? "bg-finsight-primary text-white" : "bg-white hover:shadow-md"}`}
    >
      <h4 className={`min-w-0 font-semibold ${newest ? "" : "text-gray-900"}`}>{item.title}</h4>
      <div className={`shrink-0 text-sm ${newest ? "text-white/80" : "text-gray-500"}`}>{relativeTime(item.publishedAt)}</div>
    </Link>
  )
}
