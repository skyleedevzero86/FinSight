import Link from "next/link"
import type { PortfolioAsset, PortfolioInsight, PortfolioSlice, PortfolioSummary, PortfolioTrend } from "@/lib/portfolioApi"
import type { StoredNewsCard } from "@/lib/publicNews"

const SLICE_COLOR: Record<string, string> = {
  STOCK_ETF: "#325fd8",
  DEPOSIT: "#7d5cff",
  PENSION: "#21a179",
  CASH: "#f59e0b",
  OTHER: "#cbd5e1",
  REAL_ESTATE: "#ea580c",
  CAR: "#0284c7",
  CRYPTO: "#111827",
}

const ICON: Record<string, string> = {
  STOCK_ETF: "S",
  DEPOSIT: "예",
  PENSION: "연",
  CASH: "현",
  OTHER: "기",
  REAL_ESTATE: "부",
  CAR: "차",
  CRYPTO: "암",
}

function won(value: number): string {
  return `${value.toLocaleString("ko-KR")}원`
}

function manWon(value: number): string {
  if (Math.abs(value) < 10_000) return won(value)
  return `${Math.round(value / 10_000).toLocaleString("ko-KR")}만원`
}

function tenths(value: number | null | undefined): string {
  if (value == null) return "-"
  const whole = Math.trunc(value / 10)
  const fraction = Math.abs(value % 10)
  return fraction === 0 ? `${whole}%` : `${whole}.${fraction}%`
}

function signedWon(value: number): string {
  const mark = value > 0 ? "▲" : value < 0 ? "▼" : "·"
  return `${mark} ${Math.abs(value).toLocaleString("ko-KR")}원`
}

function profitText(value: number | null): string {
  if (value == null) return "-"
  const rounded = Math.round(value * 10) / 10
  return `${rounded > 0 ? "+" : ""}${rounded}%`
}

function goalChip(label: string | null): string {
  if (!label) return "목표 1억"
  const [yearText, monthText] = label.split(".")
  const year = Number(yearText)
  const month = Number(monthText)
  if (!Number.isFinite(year) || !Number.isFinite(month)) return label
  const now = new Date()
  const months = (year - now.getFullYear()) * 12 + (month - (now.getMonth() + 1))
  return months > 0 ? `D-${months}개월` : label
}

function paceText(value: number | null): string {
  if (value == null) return "이번 달 기록을 쌓으면 월평균이 나옵니다"
  const sign = value > 0 ? "+" : value < 0 ? "-" : ""
  return `최근 월평균 ${sign}${Math.round(Math.abs(value) / 10_000).toLocaleString("ko-KR")}만원`
}

function donutBackground(slices: PortfolioSlice[]): string {
  if (slices.length === 0) return "conic-gradient(#e2e8f0 0 100%)"
  let cursor = 0
  const stops = slices.map((slice) => {
    const start = cursor
    cursor += slice.percentTenths / 10
    const color = SLICE_COLOR[slice.category] ?? "#94a3b8"
    return `${color} ${start}% ${cursor}%`
  })
  return `conic-gradient(${stops.join(",")})`
}

function sentimentLabel(value: string | null): string {
  if (value === "POSITIVE") return "긍정"
  if (value === "NEGATIVE") return "부정"
  if (value === "NEUTRAL") return "중립"
  return "뉴스"
}

export function PortfolioDashboard({
  summary,
  news,
  newsCaption,
  shareNote,
  onRegister,
  onRecord,
  onShare,
  onDelete,
}: {
  summary: PortfolioSummary
  news: StoredNewsCard[]
  newsCaption: string
  shareNote: string
  onRegister: () => void
  onRecord: () => void
  onShare: () => void
  onDelete: (asset: PortfolioAsset) => void
}) {
  const goalWidth = Math.max(0, Math.min(100, summary.goalPercentTenths / 10))
  return (
    <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-4 px-4 py-6 md:px-6">
      <Hero
        summary={summary}
        shareNote={shareNote}
        goalWidth={goalWidth}
        onRegister={onRegister}
        onRecord={onRecord}
        onShare={onShare}
      />
      <div className="grid gap-4 lg:grid-cols-2">
        <AllocationCard slices={summary.allocation} totalAssets={summary.totalAssets} />
        <GoalCard summary={summary} goalWidth={goalWidth} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <TrendCard trend={summary.trend} />
        <DiagnosisCard insights={summary.insights} advice={summary.advice} score={summary.goalPercentTenths} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <NewsCard news={news} caption={newsCaption} />
        <AssetTable assets={summary.assets} onRegister={onRegister} onDelete={onDelete} />
      </div>
    </div>
  )
}

function Hero({
  summary,
  shareNote,
  goalWidth,
  onRegister,
  onRecord,
  onShare,
}: {
  summary: PortfolioSummary
  shareNote: string
  goalWidth: number
  onRegister: () => void
  onRecord: () => void
  onShare: () => void
}) {
  return (
    <section className="border border-neutral-800 bg-[#1d4ed8] px-5 py-6 text-white md:px-7">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-semibold tracking-[0.16em] text-white/80">MY NET WORTH</p>
          <p className="mt-2 text-3xl font-bold tracking-tight md:text-4xl">{won(summary.netWorth)}</p>
          <p className="mt-2 flex flex-wrap gap-2 text-sm text-white/90">
            <span>{summary.monthDelta == null ? "이번 달 기록 없음" : `${signedWon(summary.monthDelta)} 이번 달`}</span>
            <span>·</span>
            <span>{summary.yearDelta == null ? "올해 기록 없음" : `${signedWon(summary.yearDelta)} 올해`}</span>
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={onRegister} className="border border-white bg-transparent px-4 py-2 text-sm font-semibold">
            ＋ 자산 등록
          </button>
          <button type="button" onClick={onRecord} className="border border-white bg-transparent px-4 py-2 text-sm font-semibold">
            ✓ 이번 달 기록
          </button>
          <button type="button" onClick={onShare} className="border border-neutral-900 bg-white px-4 py-2 text-sm font-semibold text-[#1d4ed8]">
            ↗ 공유하기
          </button>
        </div>
      </div>
      {shareNote ? <p className="mt-3 text-sm text-white/90">{shareNote}</p> : null}
      <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-4">
        <HeroStat label="총자산" value={won(summary.totalAssets)} />
        <HeroStat label="총부채" value={won(summary.totalLiabilities)} />
        <HeroStat label="이번 달 증가" value={summary.monthDelta == null ? "-" : signedWon(summary.monthDelta)} />
        <HeroStat label="자산 목표" value={tenths(summary.goalPercentTenths)} />
      </div>
      <div className="mt-4 h-1.5 overflow-hidden bg-white/25" aria-hidden>
        <div className="h-full bg-white" style={{ width: `${goalWidth}%` }} />
      </div>
    </section>
  )
}

function HeroStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="border border-white/40 px-3 py-3">
      <p className="text-xs text-white/75">{label}</p>
      <p className="mt-1 text-sm font-bold md:text-base">{value}</p>
    </div>
  )
}

function AllocationCard({ slices, totalAssets }: { slices: PortfolioSlice[]; totalAssets: number }) {
  return (
    <section className="border border-neutral-300 bg-white p-5">
      <CardHead title="자산 구성" subtitle="현재 평가금액 기준" action="자산 상세" />
      <div className="mt-4 flex flex-col items-center gap-5 sm:flex-row">
        <div className="relative h-36 w-36 shrink-0 rounded-full" style={{ background: donutBackground(slices) }} aria-label="자산 구성">
          <div className="absolute inset-5 flex flex-col items-center justify-center rounded-full bg-white text-center">
            <span className="text-sm font-bold text-slate-900">{manWon(totalAssets)}</span>
            <span className="text-[11px] text-slate-400">총자산</span>
          </div>
        </div>
        <ul className="w-full space-y-2 text-sm">
          {slices.length === 0 ? <li className="text-slate-500">등록된 자산이 없습니다.</li> : null}
          {slices.map((slice) => (
            <li key={slice.category} className="grid grid-cols-[auto_1fr_auto_auto] items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full" style={{ background: SLICE_COLOR[slice.category] ?? "#94a3b8" }} />
              <span className="font-semibold text-slate-800">{slice.label}</span>
              <span className="text-slate-400">{tenths(slice.percentTenths)}</span>
              <span className="tabular-nums text-slate-700">{slice.amount.toLocaleString("ko-KR")}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

function GoalCard({ summary, goalWidth }: { summary: PortfolioSummary; goalWidth: number }) {
  return (
    <section className="border border-neutral-300 bg-white p-5">
      <CardHead title="1억 만들기" subtitle="대표 목표" extra={goalChip(summary.expectedGoalLabel)} />
      <div className="mt-4 border border-neutral-300 bg-slate-50 p-4">
        <div className="flex items-end justify-between gap-3">
          <div>
            <p className="text-2xl font-bold text-slate-900">{won(summary.netWorth)}</p>
            <p className="mt-1 text-xs text-slate-500">목표 {won(summary.goalAmount)}</p>
          </div>
          <p className="text-lg font-bold text-[#325fd8]">{tenths(summary.goalPercentTenths)}</p>
        </div>
        <div className="mt-3 h-2 overflow-hidden bg-slate-200">
          <div className="h-full bg-[#325fd8]" style={{ width: `${goalWidth}%` }} />
        </div>
        <div className="mt-3 flex justify-between gap-3 text-xs text-slate-500">
          <span>{paceText(summary.averageMonthlyGain)}</span>
          <span>{summary.expectedGoalLabel ? `예상 달성 ${summary.expectedGoalLabel}` : "예상 시점 없음"}</span>
        </div>
      </div>
      <div className="mt-3 grid grid-cols-2 gap-3">
        <MiniStat label="현금 비중" value={cashShare(summary)} />
        <MiniStat label="부채비율" value={tenths(summary.debtPercentTenths)} />
      </div>
    </section>
  )
}

function cashShare(summary: PortfolioSummary): string {
  const cash = summary.allocation.find((slice) => slice.category === "CASH")
  return cash ? tenths(cash.percentTenths) : "0%"
}

function TrendCard({ trend }: { trend: PortfolioTrend[] }) {
  return (
    <section className="border border-neutral-300 bg-white p-5">
      <CardHead title="순자산 추이" subtitle="월별 기록 기준, 이번 달은 현재 금액" action="전체 기록" />
      <div className="mt-4 flex h-44 items-end gap-2">
        {trend.map((bar) => (
          <div key={`${bar.label}-${bar.netWorth}`} className="flex h-full flex-1 flex-col items-center justify-end gap-2">
            <div className="flex w-full flex-1 items-end">
              <div className="w-full bg-[#325fd8]" style={{ height: `${bar.heightPercent}%` }} title={won(bar.netWorth)} />
            </div>
            <span className="text-[11px] text-slate-400">{bar.label}</span>
          </div>
        ))}
      </div>
    </section>
  )
}

function DiagnosisCard({ insights, advice, score }: { insights: PortfolioInsight[]; advice: string; score: number }) {
  const ring = Math.max(0, Math.min(100, score / 10))
  return (
    <section className="border border-neutral-400 bg-white p-5">
      <CardHead title="자산 진단" subtitle="보유 금액으로 계산" extra="오늘 계산" />
      <div className="mt-4 flex gap-4">
        <div
          className="grid h-20 w-20 shrink-0 place-items-center rounded-full"
          style={{ background: `conic-gradient(#325fd8 ${ring}%, #e2e8f0 ${ring}% 100%)` }}
        >
          <div className="grid h-14 w-14 place-items-center rounded-full bg-white text-xl font-bold text-slate-900">{Math.round(ring)}</div>
        </div>
        <ul className="min-w-0 flex-1 space-y-2 text-sm">
          {insights.length === 0 ? <li className="text-slate-500">자산을 등록하면 진단 문구가 나옵니다.</li> : null}
          {insights.map((line) => (
            <li key={line.text} className={line.tone === "WARN" ? "text-amber-700" : "text-emerald-700"}>
              {line.tone === "WARN" ? "⚠ " : "✓ "}
              {line.text}
            </li>
          ))}
        </ul>
      </div>
      <p className="mt-4 border border-neutral-300 bg-white px-3 py-3 text-xs leading-6 text-slate-600">{advice}</p>
    </section>
  )
}

function NewsCard({ news, caption }: { news: StoredNewsCard[]; caption: string }) {
  return (
    <section className="border border-neutral-300 bg-white p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-slate-900">내 자산 관련 뉴스</h2>
          <p className="text-xs text-slate-500">{caption}</p>
        </div>
        <Link href="/news" className="text-xs font-semibold text-[#325fd8]">더보기 →</Link>
      </div>
      <ul className="mt-4 space-y-3">
        {news.length === 0 ? <li className="text-sm text-slate-500">표시할 뉴스가 없습니다.</li> : null}
        {news.map((item) => (
          <li key={item.id}>
            <Link href={`/news/${item.id}`} className="flex items-start gap-3 border border-transparent px-1 py-1 hover:border-neutral-200">
              <span className="grid h-9 w-9 shrink-0 place-items-center border border-neutral-300 bg-slate-100 text-xs font-bold text-slate-600">뉴</span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-semibold text-slate-900">{item.title}</span>
                <span className="mt-1 block truncate text-xs text-slate-500">{item.summary || "본문 요약 없음"}</span>
              </span>
              <span className="shrink-0 text-xs text-emerald-600">{sentimentLabel(item.sentiment)}</span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}

function AssetTable({
  assets,
  onRegister,
  onDelete,
}: {
  assets: PortfolioAsset[]
  onRegister: () => void
  onDelete: (asset: PortfolioAsset) => void
}) {
  return (
    <section className="border border-neutral-300 bg-white p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-slate-900">내 자산</h2>
          <p className="text-xs text-slate-500">평가금액이 큰 순서</p>
        </div>
        <button type="button" onClick={onRegister} className="border border-neutral-800 px-3 py-1.5 text-xs font-semibold text-slate-700">
          ＋ 추가
        </button>
      </div>
      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[420px] text-left text-sm">
          <thead className="text-xs text-slate-400">
            <tr>
              <th className="pb-2 font-medium">자산</th>
              <th className="pb-2 font-medium">평가금액</th>
              <th className="pb-2 font-medium">손익</th>
              <th className="pb-2 font-medium">비중</th>
              <th className="pb-2 font-medium" />
            </tr>
          </thead>
          <tbody>
            {assets.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-6 text-center text-slate-500">등록된 자산이 없습니다.</td>
              </tr>
            ) : null}
            {assets.map((asset) => (
              <tr key={asset.id} className="border-t border-slate-100">
                <td className="py-3">
                  <span className="flex items-center gap-2 font-semibold text-slate-800">
                    <span className="grid h-7 w-7 place-items-center border border-neutral-300 bg-slate-100 text-xs">{asset.kind === "LIABILITY" ? "부" : ICON[asset.category] ?? "자"}</span>
                    <span>
                      {asset.name}
                      {asset.kind === "LIABILITY" ? <span className="ml-2 text-[11px] font-medium text-slate-400">부채</span> : null}
                    </span>
                  </span>
                </td>
                <td className="py-3 tabular-nums">{asset.amount.toLocaleString("ko-KR")}</td>
                <td className={`py-3 ${asset.profitRate != null && asset.profitRate > 0 ? "text-emerald-600" : "text-slate-600"}`}>{profitText(asset.profitRate)}</td>
                <td className="py-3">{tenths(asset.sharePercentTenths)}</td>
                <td className="py-3 text-right">
                  <button type="button" onClick={() => onDelete(asset)} className="text-xs text-slate-400 hover:text-red-600">삭제</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function CardHead({ title, subtitle, action, extra }: { title: string; subtitle: string; action?: string; extra?: string }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <div>
        <h2 className="text-base font-bold text-slate-900">{title}</h2>
        <p className="text-xs text-slate-500">{subtitle}</p>
      </div>
      {extra ? <span className="border border-neutral-300 bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{extra}</span> : null}
      {action ? <span className="text-xs font-semibold text-[#325fd8]">{action}</span> : null}
    </div>
  )
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="border border-neutral-300 bg-slate-50 px-3 py-3">
      <p className="text-[11px] text-slate-400">{label}</p>
      <p className="mt-1 text-base font-bold text-slate-900">{value}</p>
    </div>
  )
}
