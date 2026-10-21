"use client"

import Link from "next/link"
import { useEffect, useState, type ReactNode } from "react"
import { authHeadersJson } from "@/lib/finsightToken"

type Hit = { title: string; snippet: string; href: string }
type Section = { key: string; label: string; total: number; note: string; hits: Hit[] }
type Result = {
  query: string
  total: number
  sections: Section[]
}

function sameVisibleText(left: string, right: string) {
  return left.replace(/\s+/g, " ").trim() === right.replace(/\s+/g, " ").trim()
}

function isAbort(cause: unknown) {
  return cause instanceof DOMException && cause.name === "AbortError"
}

const TONE: Record<string, string> = {
  news: "bg-orange-500",
  vod: "bg-sky-500",
  community: "bg-emerald-500",
  activity: "bg-violet-500",
}

const SECTION_MORE: Record<string, string> = {
  news: "/news",
  vod: "/live-vod",
  community: "/community",
  activity: "/myinfo/activity",
}

export default function SearchResults({ query }: { query: string }) {
  const [result, setResult] = useState<Result | null>(null)
  const [error, setError] = useState("")

  useEffect(() => {
    if (!query) {
      setResult(null)
      setError("")
      return
    }
    setError("")
    setResult(null)
    let alive = true
    const controller = new AbortController()
    fetch(`/api/v1/search?q=${encodeURIComponent(query)}`, {
      headers: { Accept: "application/json", ...authHeadersJson() },
      cache: "no-store",
      credentials: "include",
      signal: controller.signal,
    })
      .then(async (res) => {
        const body: unknown = await res.json().catch(() => null)
        if (!res.ok) {
          throw new Error(messageOf(body))
        }
        return parseResult(body)
      })
      .then((next) => {
        if (alive) setResult(next)
      })
      .catch((cause: unknown) => {
        if (!alive || isAbort(cause)) return
        setError(cause instanceof Error ? cause.message : "검색 결과를 불러오지 못했습니다.")
      })
    return () => {
      alive = false
      controller.abort()
    }
  }, [query])

  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-10">
      <h1 className="text-center text-2xl font-bold text-[#231f20]">검색</h1>
      <form action="/search" className="mt-6 border border-neutral-200 bg-white px-5 py-4">
        <div className="flex items-center gap-3">
          <label htmlFor="search-q" className="shrink-0 text-sm text-neutral-500">검색어</label>
          <input
            id="search-q"
            type="search"
            name="q"
            defaultValue={query}
            placeholder="검색어를 입력해주세요"
            className="min-w-0 flex-1 border border-neutral-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none"
            autoComplete="off"
          />
          <button type="submit" className="shrink-0 bg-neutral-800 px-4 py-2 text-sm text-white">검색</button>
        </div>
      </form>
      {!query ? <p className="mt-8 text-center text-sm text-slate-500">검색어를 입력해 주세요.</p> : null}
      {error ? <p className="mt-8 text-center text-sm text-red-600">{error}</p> : null}
      {query && !result && !error ? <p className="mt-8 text-center text-sm text-slate-500">검색 중입니다.</p> : null}
      {query && result ? (
        <>
          <p className="mt-8 text-center text-sm text-rose-500">
            {result.query}{particle(result.query)} 총 {result.total.toLocaleString("ko-KR")}건이 검색되었습니다.
          </p>
          <div className="mt-8 space-y-8">
            {result.sections.map((section) => (
              <SearchSection key={section.key} section={section} query={result.query} />
            ))}
          </div>
        </>
      ) : null}
    </div>
  )
}

function SearchSection({ section, query }: { section: Section; query: string }) {
  const locked = section.key === "activity" && section.note
  const more = section.total > section.hits.length
  return (
    <section>
      <h2 className="flex items-center gap-2 text-sm font-bold text-neutral-900">
        <span className={`inline-block h-3 w-3 ${TONE[section.key] ?? "bg-slate-400"}`} />
        {section.label}
        {locked ? null : (
          <span className="font-medium text-neutral-500">({section.total.toLocaleString("ko-KR")}건)</span>
        )}
      </h2>
      {locked ? <LockedActivity query={query} message={section.note} /> : <SectionHits section={section} query={query} />}
      {more && !locked ? (
        <Link
          href={SECTION_MORE[section.key] ?? "/search"}
          className="mt-3 block border border-neutral-200 py-3 text-center text-sm text-neutral-600 hover:bg-neutral-50"
        >
          자세히보기
        </Link>
      ) : null}
    </section>
  )
}

function SectionHits({ section, query }: { section: Section; query: string }) {
  if (section.hits.length === 0) {
    return <p className="mt-3 border border-neutral-200 px-4 py-8 text-center text-sm text-neutral-400">검색된 항목이 없습니다.</p>
  }
  return (
    <ul className="mt-3 space-y-3">
      {section.hits.map((hit) => (
        <li key={`${section.key}-${hit.href}-${hit.title}`} className="border border-neutral-200 px-4 py-3">
          <div className="flex items-start justify-between gap-4">
            <Link href={hit.href || "/search"} className="min-w-0 text-sm font-semibold text-sky-600 hover:underline">
              <Highlight text={hit.title} query={query} />
            </Link>
            <Link href={hit.href || "/search"} className="shrink-0 text-xs text-neutral-500 hover:underline">
              자세히보기
            </Link>
          </div>
          {hit.snippet && !sameVisibleText(hit.snippet, hit.title) ? (
            <p className="mt-1 line-clamp-2 text-sm leading-6 text-neutral-500">
              <Highlight text={hit.snippet} query={query} />
            </p>
          ) : null}
        </li>
      ))}
    </ul>
  )
}

function LockedActivity({ query, message }: { query: string; message: string }) {
  const rows = ["내 글", "내 댓글", "알림"]
  return (
    <div className="relative mt-3 min-h-40 border border-neutral-200 px-4 py-8">
      <ul className="pointer-events-none select-none space-y-4 blur-[6px]" aria-hidden>
        {rows.map((label) => (
          <li key={label}>
            <p className="text-sm font-semibold text-sky-700">{query} {label}</p>
            <p className="mt-1 text-sm leading-6 text-slate-600">{query} 관련 {label} 내용입니다.</p>
          </li>
        ))}
      </ul>
      <div className="absolute inset-0 flex items-center justify-center">
        <Link href="/login" className="border border-neutral-300 bg-white px-4 py-2 text-sm text-neutral-800">
          {message}
        </Link>
      </div>
    </div>
  )
}

function Highlight({ text, query }: { text: string; query: string }) {
  const needle = query.trim()
  if (!needle) return <>{text}</>
  const lower = text.toLowerCase()
  const target = needle.toLowerCase()
  const parts: ReactNode[] = []
  let cursor = 0
  let found = lower.indexOf(target)
  let key = 0
  while (found >= 0) {
    if (found > cursor) parts.push(text.slice(cursor, found))
    parts.push(
      <span key={key} className="font-bold">
        {text.slice(found, found + target.length)}
      </span>,
    )
    key += 1
    cursor = found + target.length
    found = lower.indexOf(target, cursor)
  }
  if (cursor < text.length) parts.push(text.slice(cursor))
  return <>{parts}</>
}

function particle(word: string) {
  const code = word.codePointAt([...word].length - 1) ?? 0
  if (code < 0xac00 || code > 0xd7a3) return "으로"
  return (code - 0xac00) % 28 === 0 ? "로" : "으로"
}

function parseResult(body: unknown): Result {
  const data = asRecord(asRecord(body)?.data)
  const sections = Array.isArray(data?.sections) ? data.sections : []
  return {
    query: typeof data?.query === "string" ? data.query : "",
    total: typeof data?.total === "number" ? data.total : 0,
    sections: sections.flatMap(parseSection),
  }
}

function parseSection(item: unknown): Section[] {
  const row = asRecord(item)
  if (!row || typeof row.key !== "string") return []
  const hits = Array.isArray(row.hits) ? row.hits.flatMap(parseHit) : []
  return [{
    key: row.key,
    label: typeof row.label === "string" ? row.label : row.key,
    total: typeof row.total === "number" ? row.total : hits.length,
    note: typeof row.note === "string" ? row.note : "",
    hits,
  }]
}

function parseHit(item: unknown): Hit[] {
  const row = asRecord(item)
  if (!row || typeof row.title !== "string") return []
  return [{
    title: row.title,
    snippet: typeof row.snippet === "string" ? row.snippet : "",
    href: typeof row.href === "string" ? row.href : "",
  }]
}

function messageOf(body: unknown) {
  const message = asRecord(body)?.message
  return typeof message === "string" && message ? message : "검색 결과를 불러오지 못했습니다."
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (!value || typeof value !== "object") return null
  return value as Record<string, unknown>
}
