"use client"

import Link from "next/link"
import { useCallback, useState } from "react"
import {
  AI_PICK_BOTTOM_PANS,
  AI_PICK_PANS,
  HIT_NEWS_LIST,
  HIT_NEWS_TOP,
  MAIN_HERO,
  MAIN_RECOMMENDED,
  MBIG_EXTRA_PANS,
  MBIG_PANS,
  ONAIR_BLOCK,
  ONAIR_BLOCK_META,
  PLUS_BANNER_PANS,
  TIMELINE_ITEMS,
  type PanItem,
  WEEKLY_ITEMS,
} from "@/data/finsightNewsMainData"

function useCarousel(length: number) {
  const [i, setI] = useState(0)
  const prev = useCallback(() => setI((x) => (x - 1 + length) % length), [length])
  const next = useCallback(() => setI((x) => (x + 1) % length), [length])
  return { i, prev, next, setI }
}

function PanRow({ items }: { items: PanItem[] }) {
  return (
    <ul className="pan">
      {items.map((it) => (
        <li key={it.title}>
          <Link href={it.href}>
            <span className={`img${it.vod ? " ico_vod" : ""}`}>
              <img src={it.image} alt={it.alt} />
            </span>
            <div className="title ellipsis2">{it.title}</div>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function TimelineSection({ title = "BTC" }: { title?: string }) {
  return (
    <section className="news_timeline">
      <div className="wrap_box">
        <h2>{title}</h2>
        <div className="wrap_timeline slick-slider slick-initialized">
          <div className="slick-list">
            <div className="slick-track flv-timeline-track">
              {TIMELINE_ITEMS.map((it) => (
                <div key={it.title} className="slick-slide flv-timeline-slide">
                  <Link href={it.href}>
                    <span className={`img${it.vod ? " ico_vod" : ""}`}>
                      <img src={it.image} alt={it.alt} />
                    </span>
                    <div className="title ellipsis2">{it.title}</div>
                  </Link>
                  <div className="time">{it.time}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

export default function FinsightNewsMain() {
  const aiPick = useCarousel(AI_PICK_PANS.length)
  const aiPickBottom = useCarousel(AI_PICK_BOTTOM_PANS.length)
  const mbig = useCarousel(MBIG_PANS.length)
  const mbigExtra = useCarousel(MBIG_EXTRA_PANS.length)
  const plus = useCarousel(PLUS_BANNER_PANS.length)

  return (
    <>
      <section className="news_top">
        <div className="news_header">
          <div className="top_left">
            <Link href={MAIN_HERO.href}>
              <span className="img">
                <img src={MAIN_HERO.image} alt={MAIN_HERO.alt} />
              </span>
              <div className="top_txt">
                <div className="top_title ellipsis">{MAIN_HERO.title}</div>
                <div className="top_sub ellipsis2">{MAIN_HERO.summary}</div>
              </div>
            </Link>
          </div>
          <div className="news_text_right">
            <h2>추천 주요뉴스</h2>
            <ul>
              {MAIN_RECOMMENDED.map((n, idx) => (
                <li key={`${idx}-${n.title}`} className="ellipsis">
                  <Link href={n.href}>{n.title}</Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      <section className="news_pan finsight-news-pan-spy">
        <div className="wrap_box">
          <div className="box_vert">
            <h2>SPY</h2>
            <div className="pan_list">
              <PanRow items={AI_PICK_PANS[aiPick.i]} />
              <div className="paging">
                <div className="paging_number">
                  <span className="current">{aiPick.i + 1}</span>/
                  <span className="all">{AI_PICK_PANS.length}</span>
                </div>
                <button type="button" className="btn_left" onClick={aiPick.prev} aria-label="이전" />
                <button type="button" className="btn_right" onClick={aiPick.next} aria-label="다음" />
              </div>
            </div>
          </div>
          <div className="box_right box_live">
            <div className="onair_tit">
              <Link href="#">
                <span className="live">{ONAIR_BLOCK.liveLabel}</span>
                <span className="program">{ONAIR_BLOCK.program}</span>
                <span className="time" />
              </Link>
            </div>
            <div className="onair">
              <img src={ONAIR_BLOCK.image} alt={ONAIR_BLOCK.alt} />
            </div>
          </div>
        </div>
      </section>

      <section className="news_mbig">
        <div className="wrap_box">
          <div className="box_vert">
            <h2>QQQ</h2>
            <div className="pan_list">
              <ul className="pan">
                {MBIG_PANS[mbig.i].map((it) => (
                  <li key={it.title} className="item">
                    <Link href={it.href}>
                      <span className="img">
                        <img src={it.image} alt={it.alt} />
                      </span>
                      <div className="title ellipsis2">{it.title}</div>
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="paging">
                <div className="paging_number">
                  <span className="current">{mbig.i + 1}</span>/
                  <span className="all">{MBIG_PANS.length}</span>
                </div>
                <button type="button" className="btn_left" onClick={mbig.prev} aria-label="이전" />
                <button type="button" className="btn_right" onClick={mbig.next} aria-label="다음" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <TimelineSection />

      <section className="news_theme">
        <div className="wrap_box">
          <div className="cont_left news_rank">
            <h2>APPL</h2>
            <div className="list_hori">
              <div className="top_news">
                <Link href={HIT_NEWS_TOP.href}>
                  <span className="img">
                    <img src={HIT_NEWS_TOP.image} alt={HIT_NEWS_TOP.alt} />
                  </span>
                  <div className="wrap-txt ellipsis2">
                    <span className="num">1.</span>
                    <span className="title">{HIT_NEWS_TOP.title}</span>
                  </div>
                </Link>
              </div>
              <ul>
                {HIT_NEWS_LIST.map((n) => (
                  <li key={n.num} className="ellipsis">
                    <Link href={n.href}>
                      <span className="num">{n.num}.</span>
                      <span className="title ellipsis">{n.title}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </div>
          <div className="cont_right">
            <h2>MSFT</h2>
            <div className="plus_banner">
              <div className="pan_list">
                <ul className="pan">
                  {PLUS_BANNER_PANS[plus.i].map((b) => (
                    <li key={b.text}>
                      <Link href={b.href}>
                        <img src={b.image} alt={b.alt} />
                        <span className="txt">{b.text}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
                <div className="paging">
                  <div className="paging_number">
                    <span className="current">{plus.i + 1}</span>/
                    <span className="all">{PLUS_BANNER_PANS.length}</span>
                  </div>
                  <button type="button" className="btn_left" onClick={plus.prev} aria-label="이전" />
                  <button type="button" className="btn_right" onClick={plus.next} aria-label="다음" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="news_weekly">
        <div className="wrap_box">
          <h2>NVDA</h2>
          <div className="list_week">
            <ul>
              {WEEKLY_ITEMS.map((w) => (
                <li key={w.title}>
                  <Link href={w.href}>
                    <span className="img ico_vod">
                      <img src={w.image} alt={w.alt} />
                    </span>
                    <div className="wrap_txt">
                      <div className="title ellipsis2">{w.title}</div>
                    </div>
                  </Link>
                  <Link href="#">
                    <div className="channel">
                      <span className="name">{w.channel}</span>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      <section className="news_mbig">
        <div className="wrap_box">
          <div className="box_vert">
            <h2>GOOGL</h2>
            <div className="pan_list">
              <ul className="pan">
                {MBIG_EXTRA_PANS[mbigExtra.i].map((it) => (
                  <li key={it.title} className="item">
                    <Link href={it.href}>
                      <span className="img">
                        <img src={it.image} alt={it.alt} />
                      </span>
                      <div className="title ellipsis2">{it.title}</div>
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="paging">
                <div className="paging_number">
                  <span className="current">{mbigExtra.i + 1}</span>/
                  <span className="all">{MBIG_EXTRA_PANS.length}</span>
                </div>
                <button type="button" className="btn_left" onClick={mbigExtra.prev} aria-label="이전" />
                <button type="button" className="btn_right" onClick={mbigExtra.next} aria-label="다음" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="news_pan finsight-news-pan-meta">
        <div className="wrap_box">
          <div className="box_vert">
            <h2>META</h2>
            <div className="pan_list">
              <PanRow items={AI_PICK_BOTTOM_PANS[aiPickBottom.i]} />
              <div className="paging">
                <div className="paging_number">
                  <span className="current">{aiPickBottom.i + 1}</span>/
                  <span className="all">{AI_PICK_BOTTOM_PANS.length}</span>
                </div>
                <button type="button" className="btn_left" onClick={aiPickBottom.prev} aria-label="이전" />
                <button type="button" className="btn_right" onClick={aiPickBottom.next} aria-label="다음" />
              </div>
            </div>
          </div>
          <div className="box_right box_live">
            <div className="onair_tit">
              <Link href="#">
                <span className="live">{ONAIR_BLOCK_META.liveLabel}</span>
                <span className="program">{ONAIR_BLOCK_META.program}</span>
                <span className="time" />
              </Link>
            </div>
            <div className="onair">
              <img src={ONAIR_BLOCK_META.image} alt={ONAIR_BLOCK_META.alt} />
            </div>
          </div>
        </div>
      </section>

      <TimelineSection title="TSLA" />
    </>
  )
}
