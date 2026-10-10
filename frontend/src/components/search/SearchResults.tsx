"use client"

import Link from "next/link"
import { useEffect, useState, type ReactNode } from "react"
import { authHeadersJson } from "@/lib/finsightToken"

type Hit = { title: string; snippet: string; href: string }
type Section = { key: string; label: string; total: number; note: string; hits: Hit[] }
type Result = {
  query: string
  total: number
  tookMs: number
  engine: string
  cached: boolean
  sections: Section[]
}

const TONE: Record<string, string> = {
  news: "bg-orange-500",
  vod: "bg-sky-500",
  community: "bg-emerald-500",
  activity: "bg-violet-500",
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
    fetch(`/api/v1/search?q=${encodeURIComponent(query)}`, {
      headers: { Accept: "application/json", ...authHeadersJson() },
      cache: "no-store",
      credentials: "include",
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
        if (alive) setError(cause instanceof Error ? cause.message : "검색 결과를 불러오지 못했습니다.")
      })
    return () => {
      alive = false
    }
  }, [query])

  return (
    <div className="mx-auto w-full max-w-3xl px-4 py-10">
      <h1 className="text-center text-xl font-bold text-[#231f20]">검색</h1>
      <form action="/search" className="mx-auto mt-4 flex max-w-md border border-neutral-300 bg-white">
        <input
          type="search"
          name="q"
          defaultValue={query}
          placeholder="검색어를 입력해주세요"
          className="min-w-0 flex-1 border-0 bg-transparent px-3 py-2 text-sm text-gray-900 outline-none"
          autoComplete="off"
        />
        <button type="submit" className="px-4 text-sm text-slate-700">검색</button>
      </form>
      {!query ? <p className="mt-8 text-center text-sm text-slate-500">검색어를 입력해 주세요.</p> : null}
      {error ? <p className="mt-8 text-sm text-red-600">{error}</p> : null}
      {query && !result && !error ? <p className="mt-8 text-center text-sm text-slate-500">검색 중입니다.</p> : null}
      {query && result ? (
        <>
          <p className="mt-8 text-sm text-slate-600">
            <span className="text-rose-600">{result.query}</span>
            {particle(result.query)} 총 <span className="font-bold text-rose-600">{result.total.toLocaleString("ko-KR")}</span>
            건이 검색되었습니다.
          </p>
          <p className="mt-1 text-xs text-slate-400">
            {result.tookMs}ms · {engineLabel(result.engine)}{result.cached ? " · Redis 캐시" : ""}
          </p>
          <div className="mt-6 space-y-8">
            {result.sections.map((section) => (
              <section key={section.key}>
                <h2 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                  <span className={`inline-block h-3 w-3 ${TONE[section.key] ?? "bg-slate-400"}`} />
                  {section.label}
                  {section.key === "activity" && section.note ? null : (
                    <span className="font-medium text-slate-400">({section.total.toLocaleString("ko-KR")}건)</span>
                  )}
                </h2>
                {section.key === "activity" && section.note ? (
                  <LockedActivity query={result.query} message={section.note} />
                ) : (
                  <>
                    {section.hits.length === 0 ? (
                      <p className="mt-3 text-sm text-slate-400">검색된 항목이 없습니다.</p>
                    ) : null}
                    <ul className="mt-3 space-y-4">
                      {section.hits.map((hit) => (
                        <li key={`${section.key}-${hit.href}-${hit.title}`}>
                          <Link href={hit.href || "/search"} className="text-sm font-semibold text-sky-700 hover:underline">
                            <Highlight text={hit.title} query={result.query} />
                          </Link>
                          {hit.snippet ? (
                            <p className="mt-1 text-sm leading-6 text-slate-600">
                              <Highlight text={hit.snippet} query={result.query} />
                            </p>
                          ) : null}
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </section>
            ))}
          </div>
        </>
      ) : null}
    </div>
  )
}

function LockedActivity({ query, message }: { query: string; message: string }) {
  const rows = ["내 글", "내 댓글", "알림"]
  return (
    <div className="relative mt-3 min-h-36">
      <ul className="pointer-events-none select-none space-y-4 blur-[6px]" aria-hidden>
        {rows.map((label) => (
          <li key={label}>
            <p className="text-sm font-semibold text-sky-700">{query} {label}</p>
            <p className="mt-1 text-sm leading-6 text-slate-600">{query} 관련 {label} 내용입니다.</p>
          </li>
        ))}
      </ul>
      <div className="absolute inset-0 flex items-center justify-center">
        <Link href="/login" className="bg-white/90 px-4 py-2 text-sm font-semibold text-slate-800 shadow-sm">
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
      <span key={key} className="font-semibold text-rose-600">
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

function engineLabel(engine: string) {
  if (engine === "ELASTICSEARCH") return "Elasticsearch"
  if (engine === "REDIS") return "Redis"
  return "MySQL"
}

function parseResult(body: unknown): Result {
  const data = asRecord(asRecord(body)?.data)
  const sections = Array.isArray(data?.sections) ? data.sections : []
  return {
    query: typeof data?.query === "string" ? data.query : "",
    total: typeof data?.total === "number" ? data.total : 0,
    tookMs: typeof data?.tookMs === "number" ? data.tookMs : 0,
    engine: typeof data?.engine === "string" ? data.engine : "MYSQL",
    cached: data?.cached === true,
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
