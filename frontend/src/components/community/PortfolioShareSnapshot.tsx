import type { PortfolioShareView } from "@/lib/portfolioApi"

const AMOUNT_LABEL: Record<string, string> = {
  EXACT: "정확한 금액",
  BAND: "금액 구간",
  RATIO: "비율만",
}

const SCOPE_LABEL: Record<string, string> = {
  PUBLIC: "전체 공개",
  FOLLOWERS: "팔로워",
  PRIVATE: "나만 보기",
}

export default function PortfolioShareSnapshot({ view }: { view: PortfolioShareView }) {
  const scope = SCOPE_LABEL[view.visibility] ?? "전체 공개"
  const progress = Math.max(0, Math.min(view.progressPercent, 100))
  return (
    <div className="fcb-md-preview__content">
      <section className="px-6 py-5">
        <h2 className="text-lg font-bold text-slate-900">포트폴리오 공유</h2>
        <p className="mt-1 text-sm text-slate-500">현재 자산을 Snapshot으로 고정해 공유합니다.</p>
        <p className="mt-5 text-sm font-semibold text-slate-800">공개할 정보</p>
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          <Flag label="순자산" checked={view.showNetWorth} />
          <Flag label="전월 대비 증가율" checked={view.showMonthRate} />
          <Flag label="자산 구성" checked={view.showAllocation} />
          <Flag label="목표 달성률" checked={view.showGoal} />
          <Flag label="정확한 종목" checked={view.showExactNames} />
          <Flag label="부채" checked={view.showDebt} />
          <Flag label="투자원금" checked={view.showPrincipal} />
          <Flag label="수익금" checked={view.showProfit} />
        </div>
        <p className="mt-5 text-sm font-semibold text-slate-800">금액 표시</p>
        <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-700">
          {Object.entries(AMOUNT_LABEL).map(([value, label]) => (
            <label key={value} className="inline-flex items-center gap-2">
              <input type="radio" checked={view.amountMode === value} readOnly disabled />
              {label}
            </label>
          ))}
        </div>
        <p className="mt-5 text-sm font-semibold text-slate-800">공개 범위</p>
        <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-700">
          {Object.entries(SCOPE_LABEL).map(([value, label]) => (
            <label key={value} className="inline-flex items-center gap-2">
              <input type="radio" checked={view.visibility === value} readOnly disabled />
              {label}
            </label>
          ))}
        </div>
        <label className="mt-5 block text-sm font-medium text-slate-700">
          공유 메시지
          <textarea readOnly rows={3} value={view.message} className="mt-1 w-full border border-neutral-300 px-3 py-2 text-sm" />
        </label>
        <div className="mt-5 border border-neutral-200 bg-slate-50 px-4 py-4">
          <p className="text-xs text-slate-500">공유 카드 미리보기</p>
          <p className="mt-2 text-xs text-slate-500">{view.goalLabel} · {scope}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{view.netWorthLabel}</p>
          <p className="mt-2 text-xs text-slate-600">
            {view.showMonthRate && view.monthRateLabel ? `이번 달 ${view.monthRateLabel} ` : ""}
            {view.showGoal ? `목표 ${progress}%` : ""}
          </p>
        </div>
      </section>
      <section className="border-t border-neutral-200 px-6 py-5">
        <h2 className="text-lg font-bold text-slate-900">자산 등록</h2>
        <p className="mt-1 text-sm text-slate-500">공유 Snapshot에 포함된 자산만 보여 줍니다. 계좌와 보관처는 제외됩니다.</p>
        {view.assets.length === 0 ? (
          <p className="mt-4 text-sm text-slate-500">자산 구성은 공개하지 않았습니다.</p>
        ) : (
          <ul className="mt-4 grid gap-3">
            {view.assets.map((asset, index) => (
              <li key={`${asset.category}-${index}`} className="border border-neutral-200 bg-slate-50 px-4 py-4">
                <div className="flex items-end justify-between gap-3">
                  <div>
                    <p className="text-base font-bold text-slate-900">{asset.name || "종목 비공개"}</p>
                    <p className="mt-1 text-xs text-slate-500">{asset.category} · 보관처 비공개</p>
                  </div>
                  <p className="text-lg font-bold text-slate-900">{asset.amountLabel}</p>
                </div>
                <dl className="mt-3 grid gap-2 text-xs text-slate-600 sm:grid-cols-2">
                  <div>매수원금 / 취득가 {asset.acquisitionLabel || "비공개"}</div>
                  <div>보유 수량 {asset.quantityLabel || "비공개"}</div>
                  <div>현재 단가 {asset.unitPriceLabel || "비공개"}</div>
                  <div>수익금 {asset.profitLabel || "비공개"}</div>
                </dl>
              </li>
            ))}
          </ul>
        )}
      </section>
      <section className="border-t border-neutral-200 px-6 py-5">
        <h2 className="text-lg font-bold text-slate-900">이번 달 자산 기록</h2>
        <p className="mt-1 text-sm text-slate-500">공유 시점의 월간 Snapshot입니다. 이후 자산을 수정해도 이 기록은 유지됩니다.</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-medium text-slate-700">
            기록 월
            <div className="mt-1 border border-neutral-300 px-3 py-2 text-sm text-slate-900">{view.recordMonth || "-"}</div>
          </label>
          <label className="block text-sm font-medium text-slate-700">
            기록 상태
            <div className="mt-1 border border-neutral-300 px-3 py-2 text-sm text-slate-900">확정</div>
          </label>
        </div>
        <div className="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Metric label="총자산" value={view.totalAssetsLabel} />
          <Metric label="총부채" value={view.totalLiabilitiesLabel} />
          <Metric label="순자산" value={view.netWorthLabel} />
          <Metric label="전월 대비" value={view.monthRateLabel || "-"} />
        </div>
        <label className="mt-4 block text-sm font-medium text-slate-700">
          이번 달 메모
          <textarea readOnly rows={3} value={view.message} className="mt-1 w-full border border-neutral-300 px-3 py-2 text-sm" />
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
            {view.history.length === 0 ? (
              <tr>
                <td colSpan={4} className="border-t border-neutral-200 py-4 text-slate-500">저장된 월 기록이 없습니다.</td>
              </tr>
            ) : view.history.map((row) => (
              <tr key={`${row.month}-${row.netWorthLabel}`}>
                <td className="border-t border-neutral-200 py-3">{row.month}</td>
                <td className="border-t border-neutral-200 py-3">{row.netWorthLabel}</td>
                <td className="border-t border-neutral-200 py-3">{row.deltaLabel}</td>
                <td className="border-t border-neutral-200 py-3">{row.statusLabel}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}

function Flag({ label, checked }: { label: string; checked: boolean }) {
  return (
    <label className="flex items-center gap-2 border border-neutral-300 px-3 py-3 text-sm text-slate-800">
      <input type="checkbox" checked={checked} readOnly disabled />
      {label}
    </label>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="border border-neutral-200 bg-slate-50 px-3 py-3">
      <p className="text-xs text-slate-400">{label}</p>
      <p className="mt-1 text-base font-bold text-slate-900">{value}</p>
    </div>
  )
}
