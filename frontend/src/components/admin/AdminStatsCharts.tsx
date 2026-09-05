"use client"

import { useMemo } from "react"
import type { AdminStatsNamedSeries } from "@/lib/adminStats"

const SERIES_COLORS = ["#3b82f6", "#03c75a", "#14b8a6", "#f97316", "#a855f7", "#ef4444"]

function formatDayLabel(iso: string): string {
  if (iso.length >= 10 && iso.includes("-")) {
    return iso.slice(0, 10).replace(/-/g, ".")
  }
  return iso
}

export function AdminStatsLineChart({
  series,
  unit,
}: {
  series: AdminStatsNamedSeries[]
  unit: string
}) {
  const width = 920
  const height = 360
  const padL = 56
  const padR = 140
  const padT = 24
  const padB = 48
  const plotW = width - padL - padR
  const plotH = height - padT - padB

  const labels = useMemo(() => {
    const first = series[0]?.points ?? []
    return first.map((p) => p.date)
  }, [series])

  const maxValue = useMemo(() => {
    let max = 0
    for (const s of series) {
      for (const p of s.points) max = Math.max(max, p.value)
    }
    return Math.max(10, Math.ceil(max / 10) * 10)
  }, [series])

  const yTicks = useMemo(() => {
    const steps = 10
    const step = maxValue / steps
    return Array.from({ length: steps + 1 }, (_, i) => Math.round(step * i))
  }, [maxValue])

  const xCount = Math.max(labels.length, 1)

  function xAt(index: number): number {
    if (xCount <= 1) return padL + plotW / 2
    return padL + (plotW * index) / (xCount - 1)
  }

  function yAt(value: number): number {
    return padT + plotH - (plotH * value) / maxValue
  }

  return (
    <div className="w-full overflow-x-auto">
      <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[720px] w-full h-auto">
        <text x={12} y={18} className="fill-gray-500" fontSize="12">
          {`단위 : ${unit}`}
        </text>
        {yTicks.map((tick) => {
          const y = yAt(tick)
          return (
            <g key={tick}>
              <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="#e5e7eb" strokeWidth="1" />
              <text x={padL - 10} y={y + 4} textAnchor="end" className="fill-gray-400" fontSize="11">
                {tick}
              </text>
            </g>
          )
        })}
        {labels.map((label, i) => (
          <text
            key={`${label}-${i}`}
            x={xAt(i)}
            y={height - 16}
            textAnchor="middle"
            className="fill-gray-500"
            fontSize="11"
          >
            {formatDayLabel(label)}
          </text>
        ))}
        {series.map((s, si) => {
          const color = SERIES_COLORS[si % SERIES_COLORS.length]
          const pts = s.points.map((p, i) => `${xAt(i)},${yAt(p.value)}`).join(" ")
          return (
            <g key={s.name || s.label}>
              <polyline fill="none" stroke={color} strokeWidth="2.5" points={pts} />
              {s.points.map((p, i) => (
                <circle key={`${s.name}-${i}`} cx={xAt(i)} cy={yAt(p.value)} r="3.5" fill={color} />
              ))}
            </g>
          )
        })}
        {series.map((s, si) => {
          const color = SERIES_COLORS[si % SERIES_COLORS.length]
          const y = padT + 8 + si * 28
          return (
            <g key={`legend-${s.name}`}>
              <rect x={width - padR + 16} y={y} width="14" height="14" fill={color} />
              <text x={width - padR + 36} y={y + 12} className="fill-gray-700" fontSize="13">
                {s.label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}

export function AdminStatsBarChart({
  series,
  unit,
}: {
  series: AdminStatsNamedSeries[]
  unit: string
}) {
  const width = 920
  const height = 360
  const padL = 56
  const padR = 40
  const padT = 24
  const padB = 56
  const plotW = width - padL - padR
  const plotH = height - padT - padB

  const bars = useMemo(
    () =>
      series.map((s) => ({
        label: s.label,
        value: s.points[0]?.value ?? 0,
      })),
    [series],
  )

  const maxValue = useMemo(() => {
    const max = bars.reduce((acc, b) => Math.max(acc, b.value), 0)
    return Math.max(10, Math.ceil(max / 10) * 10)
  }, [bars])

  const barGap = 24
  const barW = bars.length ? Math.min(72, (plotW - barGap * (bars.length + 1)) / bars.length) : 40

  return (
    <div className="w-full overflow-x-auto">
      <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[720px] w-full h-auto">
        <text x={12} y={18} className="fill-gray-500" fontSize="12">
          {`단위 : ${unit}`}
        </text>
        {Array.from({ length: 11 }, (_, i) => {
          const tick = Math.round((maxValue / 10) * i)
          const y = padT + plotH - (plotH * tick) / maxValue
          return (
            <g key={tick}>
              <line x1={padL} y1={y} x2={width - padR} y2={y} stroke="#e5e7eb" strokeWidth="1" />
              <text x={padL - 10} y={y + 4} textAnchor="end" className="fill-gray-400" fontSize="11">
                {tick}
              </text>
            </g>
          )
        })}
        {bars.map((bar, i) => {
          const color = SERIES_COLORS[i % SERIES_COLORS.length]
          const x = padL + barGap + i * (barW + barGap)
          const h = (plotH * bar.value) / maxValue
          const y = padT + plotH - h
          return (
            <g key={bar.label}>
              <rect x={x} y={y} width={barW} height={Math.max(h, 1)} fill={color} />
              <text
                x={x + barW / 2}
                y={height - 20}
                textAnchor="middle"
                className="fill-gray-600"
                fontSize="12"
              >
                {bar.label}
              </text>
              <text
                x={x + barW / 2}
                y={y - 8}
                textAnchor="middle"
                className="fill-gray-700"
                fontSize="12"
              >
                {bar.value}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}
