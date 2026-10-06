import Link from "next/link"

const SLICES = [
  { label: "주식", pct: 40, color: "#3b82f6" },
  { label: "예금", pct: 25, color: "#f5c451" },
  { label: "펀드", pct: 15, color: "#34d399" },
  { label: "기타", pct: 20, color: "#94a3b8" },
] as const

const TREND = [1900, 1550, 2280, 1720, 2360, 2450]

function recentMonths(): string[] {
  const now = new Date()
  return Array.from({ length: TREND.length }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (TREND.length - 1) + index, 1)
    return `${date.getMonth() + 1}월`
  })
}

function PanelHead({
  title,
  href,
  action,
}: {
  title: string
  href: string
  action: string
}) {
  return (
    <div className="mb-3 flex items-center justify-between gap-2">
      <h2 className="flex items-center gap-2 text-[15px] font-bold text-slate-800">
        {title}
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-500">
          샘플
        </span>
      </h2>
      <Link href={href} className="shrink-0 text-xs text-slate-400 hover:text-finsight-primary">
        {action} <span aria-hidden>›</span>
      </Link>
    </div>
  )
}

function AssetDonut() {
  const radius = 38
  const circumference = 2 * Math.PI * radius
  let cursor = 0
  return (
    <svg viewBox="0 0 120 120" className="h-36 w-36 shrink-0" role="img" aria-label="자산 구성 샘플">
      <g transform="rotate(-90 60 60)">
        {SLICES.map((slice) => {
          const length = (slice.pct / 100) * circumference
          const node = (
            <circle
              key={slice.label}
              cx="60"
              cy="60"
              r={radius}
              fill="none"
              stroke={slice.color}
              strokeWidth="14"
              strokeDasharray={`${length} ${circumference - length}`}
              strokeDashoffset={-cursor}
            />
          )
          cursor += length
          return node
        })}
      </g>
      <text x="60" y="56" textAnchor="middle" fontSize="9" fill="#94a3b8">
        총자산
      </text>
      <text x="60" y="74" textAnchor="middle" fontSize="13" fontWeight="700" fill="#0f172a">
        5,200만원
      </text>
    </svg>
  )
}

function AssetSummary() {
  const rows = [
    { label: "총자산", value: "5,200", unit: "만원", tone: "text-sky-600" },
    { label: "총부채", value: "2,750", unit: "만원", tone: "text-slate-800" },
    { label: "순자산", value: "2,450", unit: "만원", tone: "text-slate-900" },
  ]
  return (
    <div className="grid grid-cols-3 gap-2 text-center">
      {rows.map((row) => (
        <div key={row.label}>
          <p className="text-[11px] text-slate-400">{row.label}</p>
          <p className={`mt-1 text-lg font-bold leading-none ${row.tone}`}>
            {row.value}
            <span className="ml-0.5 text-[11px] font-medium text-slate-500">{row.unit}</span>
          </p>
        </div>
      ))}
    </div>
  )
}

export function MyInfoAssetStatus() {
  return (
    <section className="rounded-2xl border border-[#e7edf5] bg-white px-4 py-4 shadow-[0_8px_24px_rgba(15,23,42,0.04)]">
      <PanelHead title="나의 자산 현황" href="/myinfo/portfolio" action="포트폴리오로 바로가기" />
      <AssetSummary />
      <div className="mt-4 flex flex-wrap items-center justify-center gap-6">
        <AssetDonut />
        <ul className="grid grid-cols-2 gap-x-6 gap-y-2 text-xs text-slate-600">
          {SLICES.map((slice) => (
            <li key={slice.label} className="flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: slice.color }} />
              <span>{slice.label}</span>
              <span className="font-semibold text-slate-800">{slice.pct}%</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

export function MyInfoAssetTrend() {
  const months = recentMonths()
  const width = 320
  const height = 150
  const max = 4000
  const left = 36
  const right = 12
  const top = 16
  const bottom = 28
  const plotW = width - left - right
  const plotH = height - top - bottom
  const points = TREND.map((value, index) => {
    const x = left + (index / (TREND.length - 1)) * plotW
    const y = top + (1 - value / max) * plotH
    return { x, y }
  })
  const last = points[points.length - 1]
  const ticks = [0, 1000, 2000, 3000, 4000]

  return (
    <section className="rounded-2xl border border-[#e7edf5] bg-white px-4 py-4 shadow-[0_8px_24px_rgba(15,23,42,0.04)]">
      <PanelHead title="최근 자산 변동 (순자산)" href="/myinfo/portfolio" action="6개월" />
      <svg viewBox={`0 0 ${width} ${height}`} className="h-40 w-full" role="img" aria-label="순자산 변동 샘플">
        {ticks.map((tick) => {
          const y = top + (1 - tick / max) * plotH
          return (
            <g key={tick}>
              <line x1={left} x2={width - right} y1={y} y2={y} stroke="#e8eef5" strokeWidth="1" />
              <text x={left - 6} y={y + 3} textAnchor="end" fontSize="9" fill="#94a3b8">
                {tick === 0 ? "0" : tick.toLocaleString("ko-KR")}
              </text>
            </g>
          )
        })}
        <polyline
          fill="none"
          stroke="#3b82f6"
          strokeWidth="2"
          points={points.map((point) => `${point.x},${point.y}`).join(" ")}
        />
        {points.map((point, index) => (
          <circle key={months[index]} cx={point.x} cy={point.y} r={index === points.length - 1 ? 4 : 3} fill="#3b82f6" />
        ))}
        {months.map((month, index) => (
          <text key={month} x={points[index].x} y={height - 8} textAnchor="middle" fontSize="10" fill="#94a3b8">
            {month}
          </text>
        ))}
        {last ? (
          <g>
            <rect x={Math.min(last.x - 54, width - 70)} y={Math.max(4, last.y - 22)} width="62" height="16" rx="4" fill="white" stroke="#dbe4ee" />
            <text x={Math.min(last.x - 23, width - 39)} y={Math.max(15, last.y - 11)} textAnchor="middle" fontSize="9" fontWeight="700" fill="#0f172a">
              2,450만원
            </text>
          </g>
        ) : null}
      </svg>
    </section>
  )
}
