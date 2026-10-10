"use client"

import { useState } from "react"
import type { PortfolioShareRequest, PortfolioSummary } from "@/lib/portfolioApi"
import { PortfolioDialog } from "@/components/portfolio/PortfolioDialog"

type AmountMode = "exact" | "band" | "ratio"
type Visibility = "public" | "followers" | "private"

const VISIBILITY_LABEL: Record<Visibility, string> = {
  public: "전체 공개",
  followers: "팔로워",
  private: "나만 보기",
}

export function PortfolioShareDialog({
  summary,
  pending,
  onClose,
  onSubmit,
}: {
  summary: PortfolioSummary
  pending: boolean
  onClose: () => void
  onSubmit: (request: PortfolioShareRequest) => void
}) {
  const [netWorth, setNetWorth] = useState(true)
  const [monthRate, setMonthRate] = useState(true)
  const [allocation, setAllocation] = useState(true)
  const [goal, setGoal] = useState(true)
  const [exactNames, setExactNames] = useState(false)
  const [debt, setDebt] = useState(false)
  const [principal, setPrincipal] = useState(false)
  const [profit, setProfit] = useState(false)
  const [amountMode, setAmountMode] = useState<AmountMode>("band")
  const [visibility, setVisibility] = useState<Visibility>("public")
  const [message, setMessage] = useState("")

  const monthPercent = monthRateOf(summary)
  const goalText = tenths(summary.goalPercentTenths)
  const allocationText = (summary.allocation ?? []).slice(0, 3).map((slice) => `${slice.label} ${tenths(slice.percentTenths)}`).join(" · ")
  const amountText = amountMode === "ratio" ? "비율만" : amountMode === "exact" ? `${summary.netWorth.toLocaleString("ko-KR")}원` : amountBand(summary.netWorth)

  return (
    <PortfolioDialog title="포트폴리오 공유" subtitle="현재 자산을 Snapshot으로 고정해 공유합니다." onClose={onClose} wide>
      <p className="text-sm font-semibold text-slate-800">공개할 정보</p>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <Check label="순자산" checked={netWorth} onChange={setNetWorth} />
        <Check label="전월 대비 증가율" checked={monthRate} onChange={setMonthRate} />
        <Check label="자산 구성" checked={allocation} onChange={setAllocation} />
        <Check label="목표 달성률" checked={goal} onChange={setGoal} />
        <Check label="정확한 종목" checked={exactNames} onChange={setExactNames} />
        <Check label="부채" checked={debt} onChange={setDebt} />
        <Check label="투자원금" checked={principal} onChange={setPrincipal} />
        <Check label="수익금" checked={profit} onChange={setProfit} />
      </div>
      <p className="mt-5 text-sm font-semibold text-slate-800">금액 표시</p>
      <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-700">
        <Radio name="amount" label="정확한 금액" checked={amountMode === "exact"} onChange={() => setAmountMode("exact")} />
        <Radio name="amount" label="금액 구간" checked={amountMode === "band"} onChange={() => setAmountMode("band")} />
        <Radio name="amount" label="비율만" checked={amountMode === "ratio"} onChange={() => setAmountMode("ratio")} />
      </div>
      <p className="mt-5 text-sm font-semibold text-slate-800">공개 범위</p>
      <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-700">
        <Radio name="scope" label="전체 공개" checked={visibility === "public"} onChange={() => setVisibility("public")} />
        <Radio name="scope" label="팔로워" checked={visibility === "followers"} onChange={() => setVisibility("followers")} />
        <Radio name="scope" label="나만 보기" checked={visibility === "private"} onChange={() => setVisibility("private")} />
      </div>
      <label className="mt-5 block text-sm font-medium text-slate-700">
        공유 메시지
        <textarea value={message} onChange={(event) => setMessage(event.target.value)} maxLength={500} rows={3} className="mt-1 w-full border border-neutral-300 px-3 py-2 text-sm" placeholder="이번 달 순자산 변화를 짧게 적어 주세요." />
      </label>
      <div className="mt-5 border border-neutral-200 bg-slate-50 px-4 py-4">
        <p className="text-xs text-slate-500">공유 카드 미리보기</p>
        <p className="mt-2 text-xs text-slate-500">1억 만들기 · {VISIBILITY_LABEL[visibility]}</p>
        {netWorth && amountMode !== "ratio" ? <p className="mt-1 text-2xl font-bold text-slate-900">{amountText}</p> : null}
        <p className="mt-2 text-xs text-slate-600">
          {monthRate && monthPercent != null ? `이번 달 ${monthPercent} ` : ""}
          {goal ? `목표 ${goalText} ` : ""}
          {allocation && allocationText ? allocationText : ""}
          {debt ? ` 부채 ${summary.totalLiabilities.toLocaleString("ko-KR")}원` : ""}
          {principal ? " 투자원금 포함" : ""}
          {profit ? " 수익금 포함" : ""}
          {exactNames ? ` ${nameLine(summary)}` : ""}
        </p>
        {message.trim() ? <p className="mt-2 text-sm text-slate-800">{message.trim()}</p> : null}
      </div>
      <p className="mt-4 border border-amber-200 bg-amber-50 px-3 py-3 text-xs leading-5 text-amber-900">
        공유 전 개인정보 보호: 계좌번호, 전화번호, 주소, 주민번호, 증권계좌 번호가 포함되어 있는지 자동 검사합니다. PRIVATE 자산 원본은 공유 Snapshot과 분리됩니다.
      </p>
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" onClick={onClose} className="border border-neutral-300 bg-white px-4 py-2 text-sm text-slate-700">취소</button>
        <button
          type="button"
          disabled={pending || message.trim().length === 0}
          onClick={() =>
            onSubmit({
              message: message.trim(),
              visibility: visibility === "public" ? "PUBLIC" : visibility === "followers" ? "FOLLOWERS" : "PRIVATE",
              amountMode: amountMode === "exact" ? "EXACT" : amountMode === "ratio" ? "RATIO" : "BAND",
              showNetWorth: netWorth,
              showMonthRate: monthRate,
              showAllocation: allocation,
              showGoal: goal,
              showDebt: debt,
              showExactNames: exactNames,
              showPrincipal: principal,
              showProfit: profit,
            })
          }
          className="border border-blue-700 bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
        >
          {pending ? "저장 중…" : "Snapshot 생성 후 공유"}
        </button>
      </div>
    </PortfolioDialog>
  )
}

function Check({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 border border-neutral-300 px-3 py-3 text-sm text-slate-800">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      {label}
    </label>
  )
}

function Radio({ name, label, checked, onChange }: { name: string; label: string; checked: boolean; onChange: () => void }) {
  return (
    <label className="inline-flex items-center gap-2">
      <input type="radio" name={name} checked={checked} onChange={onChange} />
      {label}
    </label>
  )
}

function tenths(value: number | null | undefined): string {
  if (value == null) return "-"
  const whole = Math.trunc(value / 10)
  const fraction = Math.abs(value % 10)
  return fraction === 0 ? `${whole}%` : `${whole}.${fraction}%`
}

function monthRateOf(summary: PortfolioSummary): string | null {
  if (summary.monthDelta == null) return null
  const previous = summary.netWorth - summary.monthDelta
  if (previous === 0) return null
  const rate = (summary.monthDelta * 100) / previous
  const sign = rate > 0 ? "+" : ""
  return `${sign}${rate.toFixed(1)}%`
}

function amountBand(value: number): string {
  const sign = value < 0 ? "-" : ""
  const abs = Math.abs(value)
  if (abs >= 100_000_000) {
    const eok = Math.floor(abs / 100_000_000)
    const cheon = Math.floor((abs % 100_000_000) / 10_000_000)
    return cheon > 0 ? `${sign}약 ${eok}억 ${cheon},000만원` : `${sign}약 ${eok}억원`
  }
  if (abs >= 10_000_000) {
    const man = Math.floor(abs / 10_000_000) * 1000
    return `${sign}약 ${man.toLocaleString("ko-KR")}만원`
  }
  if (abs >= 10_000) return `${sign}약 ${Math.floor(abs / 10_000).toLocaleString("ko-KR")}만원`
  return `${sign}${abs.toLocaleString("ko-KR")}원`
}

function nameLine(summary: PortfolioSummary): string {
  return (summary.assets ?? []).filter((asset) => asset.kind === "ASSET").slice(0, 3).map((asset) => asset.name).join(", ")
}
