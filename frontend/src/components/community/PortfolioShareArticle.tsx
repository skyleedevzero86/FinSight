"use client"

import Link from "next/link"
import type { PortfolioShareCard } from "@/lib/portfolioApi"

export type ShareModeration = "OPEN" | "WARN" | "BLIND" | "REMOVED"

export function shareModeration(card: PortfolioShareCard): ShareModeration {
  if (card.moderationStatus === "WARN" || card.moderationStatus === "BLIND" || card.moderationStatus === "REMOVED") {
    return card.moderationStatus
  }
  return "OPEN"
}

export default function PortfolioShareArticle({
  card,
  pending = false,
  linked = true,
  expanded = false,
  onModerate,
}: {
  card: PortfolioShareCard
  pending?: boolean
  linked?: boolean
  expanded?: boolean
  onModerate?: (status: Exclude<ShareModeration, "OPEN">) => void
}) {
  const status = shareModeration(card)
  const progress = Math.max(0, Math.min(card.progressPercent, 100))
  const rateLabel = card.cheerCount > 0 ? card.cheerCount.toLocaleString("ko-KR") : card.goalRateLabel
  const warned = status === "WARN"
  const locked = status === "REMOVED"
  const normal = status === "OPEN" && (card.likeCount ?? 0) >= 10
  const body = (
    <>
      {normal ? <p className="mb-3 text-xs font-semibold text-emerald-600">정상</p> : null}
      {warned ? <p className="mb-3 text-xs font-semibold text-rose-600">위험</p> : null}
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-orange-500 text-xs font-bold text-white">
            {card.authorName.slice(0, 1)}
          </span>
          <span className={`truncate text-sm font-semibold text-slate-800 ${locked ? "line-through" : ""}`}>
            {card.authorName}
          </span>
        </div>
        <time className="shrink-0 text-xs text-slate-400">{card.sharedAt}</time>
      </div>
      {status === "BLIND" ? (
        <p className="mt-3 text-sm leading-6 text-slate-500">블라인드 처리된 게시물입니다.</p>
      ) : null}
      {locked ? (
        <div className="mt-3 select-none">
          <p className="text-sm leading-6 text-slate-400 line-through">삭제된 게시물입니다.</p>
          <div className="mt-3 border-t border-slate-500" />
        </div>
      ) : null}
      {status === "OPEN" || status === "WARN" ? (
        <>
          <p className={`mt-3 text-sm leading-6 text-slate-500 ${expanded ? "" : "line-clamp-2"}`}>{card.message}</p>
          <div className={`mt-4 rounded-2xl px-4 py-4 ${warned ? "bg-rose-100" : "bg-[#f4f6fb]"}`}>
            <p className="text-[11px] text-slate-400">목표달성</p>
            <p className="mt-1 text-sm font-semibold text-slate-800">{card.goalLabel}</p>
            <div className="mt-2 flex items-end justify-between gap-3">
              <p className="text-2xl font-bold tracking-tight text-slate-900">{card.amountLabel || "비율만"}</p>
              <p className="text-sm font-semibold text-slate-500">{progress}%</p>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-slate-200">
              <div className="h-full rounded-full bg-emerald-500" style={{ width: `${progress}%` }} />
            </div>
            <div className="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-xs">
              {rateLabel ? <span className="text-emerald-600">달성률 {rateLabel}</span> : null}
              {card.monthRateLabel ? <span className="text-rose-500">이 달 {card.monthRateLabel}</span> : null}
              {card.showAsset ? <span className="text-slate-500">자산</span> : null}
              {card.showDebt ? <span className="text-slate-500">부채</span> : null}
            </div>
          </div>
        </>
      ) : null}
    </>
  )
  return (
    <article
      className={`rounded-2xl border p-5 ${
        warned ? "border-rose-200 bg-rose-50" : "border-slate-200 bg-white"
      }`}
    >
      {linked ? <Link href={`/community/free/share/${card.id}`} className="block">{body}</Link> : body}
      {onModerate && !locked ? (
        <div className="mt-4 flex gap-3">
          <button
            type="button"
            disabled={pending}
            onClick={() => onModerate("BLIND")}
            className="text-xs font-medium text-slate-500 hover:text-slate-900 disabled:opacity-40"
          >
            블라인드
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={() => onModerate("REMOVED")}
            className="text-xs font-medium text-slate-500 hover:text-slate-900 disabled:opacity-40"
          >
            삭제
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={() => onModerate("WARN")}
            className="text-xs font-medium text-slate-500 hover:text-slate-900 disabled:opacity-40"
          >
            경고
          </button>
        </div>
      ) : null}
    </article>
  )
}
