"use client"

import { useMemo, useState } from "react"
import { Banknote, Car, Coins, Home, Landmark, LineChart, Plus, Wallet } from "lucide-react"
import type { PortfolioAssetInput } from "@/lib/portfolioApi"
import { PortfolioDialog } from "@/components/portfolio/PortfolioDialog"

const TYPES = [
  { value: "CASH", label: "현금", icon: Banknote },
  { value: "DEPOSIT", label: "은행·예금", icon: Landmark },
  { value: "STOCK_ETF", label: "주식·ETF", icon: LineChart },
  { value: "PENSION", label: "연금", icon: Wallet },
  { value: "REAL_ESTATE", label: "부동산", icon: Home },
  { value: "CAR", label: "자동차", icon: Car },
  { value: "CRYPTO", label: "암호화폐", icon: Coins },
  { value: "OTHER", label: "기타", icon: Plus },
] as const

const fieldClass = "mt-1 w-full border border-neutral-300 bg-white px-3 py-2 text-sm text-slate-900"

export function PortfolioAssetDialog({
  pending,
  onClose,
  onSubmit,
}: {
  pending: boolean
  onClose: () => void
  onSubmit: (input: PortfolioAssetInput) => void
}) {
  const [kind, setKind] = useState<"ASSET" | "LIABILITY">("ASSET")
  const [category, setCategory] = useState("OTHER")
  const [name, setName] = useState("")
  const [custodian, setCustodian] = useState("")
  const [amount, setAmount] = useState("")
  const [acquisition, setAcquisition] = useState("")
  const [quantity, setQuantity] = useState("")
  const [unitPrice, setUnitPrice] = useState("")
  const [memo, setMemo] = useState("")
  const [formError, setFormError] = useState("")

  const parsedAmount = numberOrNull(amount)
  const parsedAcquisition = numberOrNull(acquisition)
  const categoryLabel = kind === "LIABILITY" ? "부채" : TYPES.find((item) => item.value === category)?.label ?? "기타"
  const profit = useMemo(() => profitRate(parsedAmount, parsedAcquisition), [parsedAmount, parsedAcquisition])

  function submit() {
    if (!name.trim()) {
      setFormError("자산명을 입력해 주세요.")
      return
    }
    if (parsedAmount == null || parsedAmount <= 0) {
      setFormError("평가금액은 1원 이상의 숫자여야 합니다.")
      return
    }
    const parsedQuantity = quantity.trim() === "" ? null : Number(quantity.replace(/,/g, ""))
    if (parsedQuantity != null && (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0)) {
      setFormError("보유 수량은 0보다 큰 숫자여야 합니다.")
      return
    }
    const parsedUnit = numberOrNull(unitPrice)
    if (unitPrice.trim() !== "" && parsedUnit == null) {
      setFormError("현재 단가는 숫자로 입력해 주세요.")
      return
    }
    onSubmit({
      name: name.trim(),
      kind,
      category: kind === "LIABILITY" ? "LIABILITY" : category,
      amount: parsedAmount,
      profitRate: null,
      custodian: custodian.trim() || null,
      acquisitionAmount: parsedAcquisition,
      quantity: parsedQuantity,
      unitPrice: parsedUnit,
      memo: memo.trim() || null,
    })
  }

  return (
    <PortfolioDialog title="자산 등록" subtitle="직접 입력부터 시작하고, CSV·증권사 연동은 나중에 붙일 수 있습니다." onClose={onClose} wide>
      <p className="text-sm font-semibold text-slate-800">자산 종류</p>
      {kind === "ASSET" ? (
        <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {TYPES.map((item) => {
            const Icon = item.icon
            const selected = category === item.value
            return (
              <button
                key={item.value}
                type="button"
                onClick={() => setCategory(item.value)}
                className={`flex flex-col items-center gap-2 border px-3 py-4 text-sm font-medium ${selected ? "border-blue-500 bg-blue-50 text-blue-700" : "border-neutral-300 bg-white text-slate-700"}`}
              >
                <Icon className="h-5 w-5" strokeWidth={1.7} aria-hidden />
                {item.label}
              </button>
            )
          })}
        </div>
      ) : (
        <p className="mt-3 border border-neutral-300 px-3 py-3 text-sm text-slate-600">부채로 등록합니다. 평가금액만 순자산에서 빠집니다.</p>
      )}
      <button type="button" onClick={() => setKind((current) => (current === "ASSET" ? "LIABILITY" : "ASSET"))} className="mt-3 text-sm font-medium text-slate-600 underline">
        {kind === "ASSET" ? "부채로 등록" : "자산으로 등록"}
      </button>
      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <label className="block text-sm font-medium text-slate-700">
          자산명 *
          <input value={name} onChange={(event) => setName(event.target.value)} maxLength={80} className={fieldClass} placeholder="KODEX 미국S&P500" />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          계좌/보관처
          <input value={custodian} onChange={(event) => setCustodian(event.target.value)} maxLength={80} className={fieldClass} placeholder="미래에셋 ISA" />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          평가금액 *
          <input value={amount} onChange={(event) => setAmount(event.target.value)} inputMode="numeric" className={fieldClass} placeholder="14300000" />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          매수원금 / 취득가
          <input value={acquisition} onChange={(event) => setAcquisition(event.target.value)} inputMode="numeric" className={fieldClass} placeholder="11800000" />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          보유 수량
          <input value={quantity} onChange={(event) => setQuantity(event.target.value)} inputMode="decimal" className={fieldClass} placeholder="120" />
        </label>
        <label className="block text-sm font-medium text-slate-700">
          현재 단가
          <input value={unitPrice} onChange={(event) => setUnitPrice(event.target.value)} inputMode="numeric" className={fieldClass} placeholder="119167" />
        </label>
      </div>
      <label className="mt-4 block text-sm font-medium text-slate-700">
        메모
        <textarea value={memo} onChange={(event) => setMemo(event.target.value)} maxLength={500} rows={3} className={fieldClass} placeholder="장기 투자 · ISA" />
      </label>
      <div className="mt-5 border border-neutral-200 bg-slate-50 px-4 py-4">
        <p className="text-xs text-slate-500">등록 미리보기</p>
        <div className="mt-2 flex items-end justify-between gap-3">
          <div>
            <p className="text-base font-bold text-slate-900">{name.trim() || "자산명"}</p>
            <p className="mt-1 text-xs text-slate-500">{categoryLabel} · {custodian.trim() || "보관처 없음"}</p>
          </div>
          <div className="text-right">
            <p className="text-lg font-bold text-slate-900">{parsedAmount == null ? "0원" : `${parsedAmount.toLocaleString("ko-KR")}원`}</p>
            {profit != null ? <p className={`text-xs font-semibold ${profit >= 0 ? "text-emerald-600" : "text-red-600"}`}>{profit >= 0 ? "+" : ""}{profit.toFixed(1)}%</p> : null}
          </div>
        </div>
      </div>
      <p className="mt-4 border border-sky-100 bg-sky-50 px-3 py-3 text-xs leading-5 text-slate-600">등록한 자산은 기본 PRIVATE입니다. 커뮤니티에는 사용자가 공유하기를 눌러 만든 Snapshot만 노출됩니다.</p>
      {formError ? <p className="mt-3 text-sm text-red-600">{formError}</p> : null}
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" onClick={onClose} className="border border-neutral-300 bg-white px-4 py-2 text-sm text-slate-700">취소</button>
        <button type="button" disabled={pending} onClick={submit} className="border border-blue-700 bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">
          {pending ? "저장 중…" : "자산 저장"}
        </button>
      </div>
    </PortfolioDialog>
  )
}

function numberOrNull(value: string): number | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  const parsed = Number(trimmed.replace(/,/g, ""))
  return Number.isFinite(parsed) ? parsed : null
}

function profitRate(amount: number | null, acquisition: number | null): number | null {
  if (amount == null || acquisition == null || acquisition <= 0) return null
  return ((amount - acquisition) * 100) / acquisition
}
