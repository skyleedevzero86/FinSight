"use client"

import Link from "next/link"
import { useCallback, useState } from "react"
import { newsImageSrc } from "@/lib/newsImage"
import type { StoredNewsCard } from "@/lib/publicNews"

const FALLBACK_IMAGE = "/finsight-logo.png"

type PanItem = { href: string; title: string; image: string; alt: string }

function useCarousel(length: number) {
  const total = Math.max(length, 1)
  const [i, setI] = useState(0)
  const prev = useCallback(() => setI((x) => (x - 1 + total) % total), [total])
  const next = useCallback(() => setI((x) => (x + 1) % total), [total])
  return { i: Math.min(i, total - 1), prev, next }
}

function toPan(item: StoredNewsCard): PanItem {
  return {
    href: `/news/${item.id}`,
    title: item.title,
    image: newsImageSrc(item.imageUrl),
    alt: item.title,
  }
}

function take(items: StoredNewsCard[], start: number, count: number): StoredNewsCard[] {
  if (items.length === 0 || count <= 0) return []
  const out: StoredNewsCard[] = []
  const seen = new Set<number>()
  for (let step = 0; step < items.length && out.length < count; step += 1) {
    const item = items[(start + step) % items.length]
    if (seen.has(item.id)) continue
    seen.add(item.id)
    out.push(item)
  }
  return out
}

function pages(items: StoredNewsCard[], start: number, pageSize: number, pageCount: number): PanItem[][] {
  const out: PanItem[][] = []
  for (let page = 0; page < pageCount; page += 1) {
    const slice = take(items, start + page * pageSize, pageSize)
    if (slice.length === 0) break
    out.push(slice.map(toPan))
  }
  return out
}

function NewsPhoto({ src, alt }: { src: string; alt: string }) {
  return <NewsPhotoFrame key={src} src={src} alt={alt} />
}

function NewsPhotoFrame({ src, alt }: { src: string; alt: string }) {
  const [current, setCurrent] = useState(src)
  return (
    <img
      src={current}
      alt={alt}
      onError={() => {
        if (current !== FALLBACK_IMAGE) setCurrent(FALLBACK_IMAGE)
      }}
    />
  )
}

function PanRow({ items }: { items: PanItem[] }) {
  return (
    <ul className="pan">
      {items.map((it) => (
        <li key={`${it.href}-${it.title}`}>
          <Link href={it.href}>
            <span className="img">
              <NewsPhoto src={it.image} alt={it.alt} />
            </span>
            <div className="title ellipsis2">{it.title}</div>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function Paging({
  index,
  total,
  onPrev,
  onNext,
}: {
  index: number
  total: number
  onPrev: () => void
  onNext: () => void
}) {
  if (total <= 1) return null
  return (
    <div className="paging">
      <div className="paging_number">
        <span className="current">{index + 1}</span>/<span className="all">{total}</span>
      </div>
      <button type="button" className="btn_left" onClick={onPrev} aria-label="이전" />
      <button type="button" className="btn_right" onClick={onNext} aria-label="다음" />
    </div>
  )
}

function TimelineSection({ title, items }: { title: string; items: StoredNewsCard[] }) {
  if (items.length === 0) return null
  return (
    <section className="news_timeline">
      <div className="wrap_box">
        <h2>{title}</h2>
        <div className="wrap_timeline slick-slider slick-initialized">
          <div className="slick-list">
            <div className="slick-track flv-timeline-track">
              {items.map((it) => (
                <div key={`${title}-${it.id}`} className="slick-slide flv-timeline-slide">
                  <Link href={`/news/${it.id}`}>
                    <span className="img">
                      <NewsPhoto src={newsImageSrc(it.imageUrl)} alt="" />
                    </span>
                    <div className="title ellipsis2">{it.title}</div>
                  </Link>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function SideLive({ item }: { item: StoredNewsCard }) {
  return (
    <div className="box_right box_live">
      <div className="onair_tit">
        <Link href={`/news/${item.id}`}>
          <span className="program">{item.title}</span>
          <span className="time" />
        </Link>
      </div>
      <div className="onair">
        <Link href={`/news/${item.id}`}>
          <NewsPhoto src={newsImageSrc(item.imageUrl)} alt="" />
        </Link>
      </div>
    </div>
  )
}

export default function FinsightNewsMain({ articles }: { articles: StoredNewsCard[] }) {
  const spyPages = pages(articles, 5, 3, 2)
  const qqqPages = pages(articles, 11, 3, 2)
  const msftPages = pages(articles, 25, 2, 2)
  const googlPages = pages(articles, 3, 3, 2)
  const metaPages = pages(articles, 9, 3, 2)
  const spy = useCarousel(spyPages.length)
  const qqq = useCarousel(qqqPages.length)
  const msft = useCarousel(msftPages.length)
  const googl = useCarousel(googlPages.length)
  const meta = useCarousel(metaPages.length)
  const hero = articles[0]
  if (!hero) return null

  const recommended = take(articles, 1, 4)
  const appl = take(articles, 21, 4)
  const applTop = appl[0]
  const applRest = appl.slice(1)
  const nvda = take(articles, 0, 3)
  const side = articles[1] ?? hero
  const spyPage = spyPages[spy.i] ?? []
  const qqqPage = qqqPages[qqq.i] ?? []
  const msftPage = msftPages[msft.i] ?? []
  const googlPage = googlPages[googl.i] ?? []
  const metaPage = metaPages[meta.i] ?? []

  return (
    <>
      <section className="news_top">
        <div className="news_header">
          <div className="top_left">
            <Link href={`/news/${hero.id}`}>
              <span className="img">
                <NewsPhoto src={newsImageSrc(hero.imageUrl)} alt="" />
              </span>
              <div className="top_txt">
                <div className="top_title ellipsis">{hero.title}</div>
                <div className="top_sub ellipsis2">{hero.summary}</div>
              </div>
            </Link>
          </div>
          <div className="news_text_right">
            <h2>추천 주요뉴스</h2>
            <ul>
              {recommended.map((item) => (
                <li key={item.id} className="ellipsis">
                  <Link href={`/news/${item.id}`}>{item.title}</Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {spyPage.length > 0 ? (
        <section className="news_pan finsight-news-pan-spy">
          <div className="wrap_box">
            <div className="box_vert">
              <h2>SPY</h2>
              <div className="pan_list">
                <PanRow items={spyPage} />
                <Paging index={spy.i} total={spyPages.length} onPrev={spy.prev} onNext={spy.next} />
              </div>
            </div>
            <SideLive item={side} />
          </div>
        </section>
      ) : null}

      {qqqPage.length > 0 ? (
        <section className="news_mbig">
          <div className="wrap_box">
            <div className="box_vert">
              <h2>QQQ</h2>
              <div className="pan_list">
                <PanRow items={qqqPage} />
                <Paging index={qqq.i} total={qqqPages.length} onPrev={qqq.prev} onNext={qqq.next} />
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <TimelineSection title="BTC" items={take(articles, 17, 4)} />

      {applTop ? (
        <section className="news_theme">
          <div className="wrap_box">
            <div className="cont_left news_rank">
              <h2>APPL</h2>
              <div className="list_hori">
                <div className="top_news">
                  <Link href={`/news/${applTop.id}`}>
                    <span className="img">
                      <NewsPhoto src={newsImageSrc(applTop.imageUrl)} alt="" />
                    </span>
                    <div className="wrap-txt ellipsis2">
                      <span className="num">1.</span>
                      <span className="title">{applTop.title}</span>
                    </div>
                  </Link>
                </div>
                <ul>
                  {applRest.map((item, index) => (
                    <li key={item.id} className="ellipsis">
                      <Link href={`/news/${item.id}`}>
                        <span className="num">{index + 2}.</span>
                        <span className="title ellipsis">{item.title}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
            {msftPage.length > 0 ? (
              <div className="cont_right">
                <h2>MSFT</h2>
                <div className="plus_banner">
                  <div className="pan_list">
                    <ul className="pan">
                      {msftPage.map((item) => (
                        <li key={item.href}>
                          <Link href={item.href}>
                            <NewsPhoto src={item.image} alt={item.alt} />
                            <span className="txt">{item.title}</span>
                          </Link>
                        </li>
                      ))}
                    </ul>
                    <Paging index={msft.i} total={msftPages.length} onPrev={msft.prev} onNext={msft.next} />
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        </section>
      ) : null}

      {nvda.length > 0 ? (
        <section className="news_weekly">
          <div className="wrap_box">
            <h2>NVDA</h2>
            <div className="list_week">
              <ul>
                {nvda.map((item) => (
                  <li key={item.id}>
                    <Link href={`/news/${item.id}`}>
                      <span className="img">
                        <NewsPhoto src={newsImageSrc(item.imageUrl)} alt="" />
                      </span>
                      <div className="wrap_txt">
                        <div className="title ellipsis2">{item.title}</div>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>
      ) : null}

      {googlPage.length > 0 ? (
        <section className="news_mbig">
          <div className="wrap_box">
            <div className="box_vert">
              <h2>GOOGL</h2>
              <div className="pan_list">
                <PanRow items={googlPage} />
                <Paging index={googl.i} total={googlPages.length} onPrev={googl.prev} onNext={googl.next} />
              </div>
            </div>
          </div>
        </section>
      ) : null}

      {metaPage.length > 0 ? (
        <section className="news_pan finsight-news-pan-meta">
          <div className="wrap_box">
            <div className="box_vert">
              <h2>META</h2>
              <div className="pan_list">
                <PanRow items={metaPage} />
                <Paging index={meta.i} total={metaPages.length} onPrev={meta.prev} onNext={meta.next} />
              </div>
            </div>
            <SideLive item={articles[2] ?? hero} />
          </div>
        </section>
      ) : null}

      <TimelineSection title="TSLA" items={take(articles, 15, 4)} />
    </>
  )
}
