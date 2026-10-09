"use client"

import Link from "next/link"
import { useState } from "react"
import { newsImageSrc } from "@/lib/newsImage"
import type { StoredNewsCard } from "@/lib/publicNews"
import "@/styles/section-news.css"

const PAGE_SIZE = 8
const FALLBACK_IMAGE = "/finsight-logo.png"

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
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}.${month}.${day}`
}

function excerpt(value: string): string {
  const flat = value.replace(/\s+/g, " ").trim()
  if (flat.length <= 180) return flat
  return `${flat.slice(0, 180)}…`
}

function NewsThumb({ item, className }: { item: StoredNewsCard; className: string }) {
  const [src, setSrc] = useState(newsImageSrc(item.imageUrl))
  return (
    <img
      className={className}
      src={src}
      alt=""
      onError={() => setSrc(FALLBACK_IMAGE)}
    />
  )
}

function ArticleRow({ item }: { item: StoredNewsCard }) {
  return (
    <article className="sn-row">
      <Link href={`/news/${item.id}`}>
        <NewsThumb key={newsImageSrc(item.imageUrl)} item={item} className="sn-thumb" />
      </Link>
      <div>
        <Link className="sn-item-title" href={`/news/${item.id}`}>
          {item.title}
        </Link>
        <p className="sn-excerpt">{excerpt(item.summary)}</p>
        <p className="sn-time">{relativeTime(item.publishedAt)}</p>
      </div>
    </article>
  )
}

function RankRow({ item, order }: { item: StoredNewsCard; order: number | null }) {
  return (
    <article className="sn-rank">
      {order ? <p className="sn-rank-no">{order}</p> : <span />}
      <div>
        <Link className="sn-rank-title" href={`/news/${item.id}`}>
          {item.title}
        </Link>
        <p className="sn-rank-time">{relativeTime(item.publishedAt)}</p>
      </div>
      <Link href={`/news/${item.id}`}>
        <NewsThumb key={newsImageSrc(item.imageUrl)} item={item} className="sn-rank-thumb" />
      </Link>
    </article>
  )
}

export default function SectionNewsBoard({ articles }: { articles: StoredNewsCard[] }) {
  const [count, setCount] = useState(PAGE_SIZE)
  const shown = articles.slice(0, count)
  const ranking = articles.slice(0, 5)
  const featured = articles.slice(5, 9)

  return (
    <div className="sn-board">
      <div>
        {shown.length === 0 ? <p className="sn-empty">표시할 뉴스가 없습니다.</p> : shown.map((item) => <ArticleRow key={item.id} item={item} />)}
        {articles.length > shown.length ? (
          <div className="sn-more">
            <button type="button" onClick={() => setCount((value) => value + PAGE_SIZE)}>
              더보기
            </button>
          </div>
        ) : null}
      </div>
      <aside>
        <section className="sn-side">
          <h2 className="sn-side-title">랭킹 뉴스</h2>
          {ranking.map((item, index) => (
            <RankRow key={item.id} item={item} order={index + 1} />
          ))}
        </section>
        {featured.length > 0 ? (
          <section className="sn-side">
            <h2 className="sn-side-title">주요 뉴스</h2>
            {featured.map((item) => (
              <RankRow key={item.id} item={item} order={null} />
            ))}
          </section>
        ) : null}
      </aside>
    </div>
  )
}
