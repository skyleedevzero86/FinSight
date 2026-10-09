"use client"

import { useState } from "react"
import type { PortfolioSummary } from "@/lib/portfolioApi"
import { PortfolioDialog } from "@/components/portfolio/PortfolioDialog"

export function PortfolioRecordDialog({
  summary,
  pending,
  onClose,
  onSubmit,
}: {
  summary: PortfolioSummary
  pending: boolean
  onClose: () => void
  onSubmit: (memo: string) => void
}) {
  const [memo, setMemo] = useState("")
  const [locked, setLocked] = useState(true)
  const [formError, setFormError] = useState("")
  const month = seoulMonth()
  const history = summary.history ?? []

  function submit() {
    if (!locked) {
      setFormError("공식 기록으로 고정에 동의해 주세요.")
      return
    }
    onSubmit(memo.trim())
  }

  return (
    <PortfolioDialog
      title="이번 달 자산 기록"
      subtitle="현재 값을 월간 Snapshot으로 고정합니다. 이후 자산을 수정해도 이 기록은 유지됩니다."
      onClose={onClose}
      wide
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block text-sm font-medium text-slate-700">
          기록 월
          <div className="mt-1 border border-neutral-300 px-3 py-2 text-sm text-slate-900">{month.label}</div>
        </label>
        <label className="block text-sm font-medium text-slate-700">
          기록 상태
          <select className="mt-1 w-full border border-neutral-300 bg-white px-3 py-2 text-sm" defaultValue="CONFIRMED">
            <option value="CONFIRMED">확정</option>
          </select>
        </label>
      </div>
      <div className="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="총자산" value={`${summary.totalAssets.toLocaleString("ko-KR")}원`} />
        <Stat label="총부채" value={`${summary.totalLiabilities.toLocaleString("ko-KR")}원`} />
        <Stat label="순자산" value={`${summary.netWorth.toLocaleString("ko-KR")}원`} />
        <Stat label="전월 대비" value={deltaText(summary.monthDelta)} positive={summary.monthDelta != null && summary.monthDelta > 0} />
      </div>
      <label className="mt-4 block text-sm font-medium text-slate-700">
        이번 달 메모
        <textarea value={memo} onChange={(event) => setMemo(event.target.value)} maxLength={500} rows={3} className="mt-1 w-full border border-neutral-300 px-3 py-2 text-sm" placeholder="부채 상환을 우선했고, 추가 투자보다 비상금 확보에 집중." />
      </label>
      <label className="mt-4 flex items-start gap-2 border border-neutral-300 px-3 py-3 text-sm text-slate-700">
        <input type="checkbox" checked={locked} onChange={(event) => setLocked(event.target.checked)} className="mt-0.5" />
        현재 자산·부채 값을 이 달의 공식 기록으로 고정합니다.
      </label>
      <h3 className="mt-6 text-sm font-bold text-slate-900">최근 기록</h3>
      <table className="mt-2 w-full text-left text-sm">
        <thead className="text-xs text-slate-400">
          <tr>
            <th className="pb-2 font-medium">월</th>
            <th className="pb-2 font-medium">순자산</th>
            <th className="pb-2 font-medium">증감</th>
            <th className="pb-2 font-medium">상태</th>
          </tr>
        </thead>
        <tbody>
          {history.length === 0 ? (
            <tr>
              <td colSpan={4} className="border-t border-neutral-200 py-4 text-slate-500">저장된 월 기록이 없습니다.</td>
            </tr>
          ) : null}
          {history.map((row) => (
            <tr key={row.yearMonth} className="border-t border-neutral-200">
              <td className="py-3">{row.yearMonth.replace("-", ".")}</td>
              <td className="py-3">{row.netWorth.toLocaleString("ko-KR")}원</td>
              <td className={`py-3 ${row.delta != null && row.delta > 0 ? "text-emerald-600" : "text-slate-700"}`}>{deltaText(row.delta)}</td>
              <td className="py-3"><span className="border border-neutral-300 bg-slate-100 px-2 py-1 text-xs">확정</span></td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="mt-4 border border-sky-100 bg-sky-50 px-3 py-3 text-xs leading-5 text-slate-600">
        Snapshot은 공유 카드, 월별 추이, 목표 달성률의 기준 데이터가 됩니다. 같은 월을 다시 확정할 때는 실제 서비스에서는 수정 이력을 남기는 방식을 권장합니다.
      </p>
      {formError ? <p className="mt-3 text-sm text-red-600">{formError}</p> : null}
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" onClick={onClose} className="border border-neutral-300 bg-white px-4 py-2 text-sm text-slate-700">취소</button>
        <button type="button" disabled={pending} onClick={submit} className="border border-blue-700 bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
          {pending ? "저장 중…" : `${month.label} 기록 확정`}
        </button>
      </div>
    </PortfolioDialog>
  )
}

function Stat({ label, value, positive }: { label: string; value: string; positive?: boolean }) {
  return (
    <div className="border border-neutral-200 bg-slate-50 px-3 py-3">
      <p className="text-xs text-slate-400">{label}</p>
      <p className={`mt-1 text-base font-bold ${positive ? "text-emerald-600" : "text-slate-900"}`}>{value}</p>
    </div>
  )
}

function deltaText(value: number | null | undefined): string {
  if (value == null) return "-"
  const sign = value > 0 ? "+" : ""
  return `${sign}${value.toLocaleString("ko-KR")}원`
}

function seoulMonth(): { label: string } {
  const label = new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", year: "numeric", month: "long" }).format(new Date())
  return { label }
}
