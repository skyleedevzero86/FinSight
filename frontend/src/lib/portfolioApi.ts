import { authHeadersJson } from "@/lib/finsightToken"

export type PortfolioSlice = {
  category: string
  label: string
  amount: number
  percentTenths: number
}

export type PortfolioTrend = {
  label: string
  netWorth: number
  heightPercent: number
}

export type PortfolioAsset = {
  id: number
  name: string
  kind: string
  category: string
  categoryLabel: string
  amount: number
  profitRate: number | null
  sharePercentTenths: number
}

export type PortfolioInsight = {
  tone: "GOOD" | "WARN" | string
  text: string
}

export type PortfolioDiagnosis = {
  insights: PortfolioInsight[]
  advice: string
  source: "LLAMA2" | "RULE" | string
}

export type PortfolioSummary = {
  totalAssets: number
  totalLiabilities: number
  netWorth: number
  goalAmount: number
  goalPercentTenths: number
  monthDelta: number | null
  yearDelta: number | null
  averageMonthlyGain: number | null
  expectedGoalLabel: string | null
  debtPercentTenths: number | null
  allocation: PortfolioSlice[]
  trend: PortfolioTrend[]
  assets: PortfolioAsset[]
  insights: PortfolioInsight[]
  advice: string
  recordedThisMonth: boolean
  history: PortfolioHistory[]
}

export type PortfolioHistory = {
  yearMonth: string
  netWorth: number
  delta: number | null
  status: string
}

export type PortfolioAssetInput = {
  name: string
  kind: "ASSET" | "LIABILITY"
  category: string
  amount: number
  profitRate: number | null
  custodian: string | null
  acquisitionAmount: number | null
  quantity: number | null
  unitPrice: number | null
  memo: string | null
}

type Json = Record<string, unknown>

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Json
  return null
}

function messageOf(body: unknown, fallback: string): string {
  const record = asRecord(body)
  const message = record && typeof record.message === "string" ? record.message.trim() : ""
  return message || fallback
}

async function parseSummary(res: Response, fallback: string): Promise<PortfolioSummary> {
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, fallback))
  }
  const record = asRecord(body)
  const data = asRecord(record?.data)
  if (!data || typeof data.netWorth !== "number") {
    throw new Error(messageOf(body, fallback))
  }
  return data as unknown as PortfolioSummary
}

export async function fetchPortfolioDiagnosis(): Promise<PortfolioDiagnosis> {
  const res = await fetch("/api/v1/portfolio/diagnosis", {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "자산 진단을 불러오지 못했습니다."))
  }
  const data = asRecord(asRecord(body)?.data)
  const insights = Array.isArray(data?.insights) ? data.insights : []
  const advice = typeof data?.advice === "string" ? data.advice : ""
  const source = typeof data?.source === "string" ? data.source : "RULE"
  return { insights: insights as PortfolioInsight[], advice, source }
}

export async function fetchPortfolio(): Promise<PortfolioSummary> {
  const res = await fetch("/api/v1/portfolio", {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  return parseSummary(res, "포트폴리오를 불러오지 못했습니다.")
}

export async function registerPortfolioAsset(input: PortfolioAssetInput): Promise<PortfolioSummary> {
  const res = await fetch("/api/v1/portfolio/assets", {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify(input),
    cache: "no-store",
  })
  return parseSummary(res, "자산을 등록하지 못했습니다.")
}

export async function deletePortfolioAsset(assetId: number): Promise<PortfolioSummary> {
  const res = await fetch(`/api/v1/portfolio/assets/${assetId}`, {
    method: "DELETE",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  return parseSummary(res, "자산을 삭제하지 못했습니다.")
}

export type PortfolioShareCard = {
  id: number
  authorName: string
  message: string
  goalLabel: string
  amountLabel: string
  progressPercent: number
  monthRateLabel: string | null
  goalRateLabel: string | null
  showAsset: boolean
  showDebt: boolean
  cheerCount: number
  sharedAt: string
  moderationStatus?: "OPEN" | "WARN" | "BLIND" | "REMOVED" | string
  likeCount?: number
  dislikeCount?: number
  reportCount?: number
}

export type PortfolioShareAssetLine = {
  category: string
  name: string
  amountLabel: string
  acquisitionLabel: string
  quantityLabel: string
  unitPriceLabel: string
  profitLabel: string
}

export type PortfolioShareHistoryLine = {
  month: string
  netWorthLabel: string
  deltaLabel: string
  statusLabel: string
}

export type PortfolioShareView = {
  showNetWorth: boolean
  showMonthRate: boolean
  showAllocation: boolean
  showGoal: boolean
  showExactNames: boolean
  showDebt: boolean
  showPrincipal: boolean
  showProfit: boolean
  amountMode: string
  visibility: string
  message: string
  goalLabel: string
  amountLabel: string
  progressPercent: number
  monthRateLabel: string | null
  recordMonth: string
  totalAssetsLabel: string
  totalLiabilitiesLabel: string
  netWorthLabel: string
  assets: PortfolioShareAssetLine[]
  history: PortfolioShareHistoryLine[]
}

export type PortfolioShareDetail = {
  card: PortfolioShareCard
  myReaction: "LIKE" | "DISLIKE" | null
  reportedByMe: boolean
  view: PortfolioShareView
}

export type PortfolioShareComment = {
  id: number
  authorName: string
  content: string
  createdAt: string
}

export type PortfolioShareGoal = {
  label: string
  participantCount: number
}

export type PortfolioShareFeed = {
  cards: PortfolioShareCard[]
  goals: PortfolioShareGoal[]
  hasNext: boolean
}

export type PortfolioShareRequest = {
  message: string
  visibility: "PUBLIC" | "FOLLOWERS" | "PRIVATE"
  amountMode: "BAND" | "EXACT" | "RATIO"
  showNetWorth: boolean
  showMonthRate: boolean
  showAllocation: boolean
  showGoal: boolean
  showDebt: boolean
  showExactNames: boolean
  showPrincipal: boolean
  showProfit: boolean
}

export async function fetchPortfolioShares(page: number, size = 2): Promise<PortfolioShareFeed> {
  const res = await fetch(`/api/v1/portfolio/shares?page=${page}&size=${size}`, {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "포트폴리오 공유를 불러오지 못했습니다."))
  }
  const data = asRecord(asRecord(body)?.data)
  const cards = Array.isArray(data?.cards) ? (data.cards as PortfolioShareCard[]) : []
  const goals = Array.isArray(data?.goals) ? (data.goals as PortfolioShareGoal[]) : []
  return { cards, goals, hasNext: data?.hasNext === true }
}

function parseShareDetail(body: unknown): PortfolioShareDetail {
  const data = asRecord(asRecord(body)?.data)
  const card = asRecord(data?.card)
  if (!card || typeof card.id !== "number") {
    throw new Error(messageOf(body, "포트폴리오 공유를 불러오지 못했습니다."))
  }
  const reaction = data?.myReaction
  const parsed = card as unknown as PortfolioShareCard
  return {
    card: parsed,
    myReaction: reaction === "LIKE" || reaction === "DISLIKE" ? reaction : null,
    reportedByMe: data?.reportedByMe === true,
    view: parseShareView(data?.view, parsed),
  }
}

export async function fetchPortfolioShare(shareId: number): Promise<PortfolioShareDetail> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}`, {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "포트폴리오 공유를 불러오지 못했습니다."))
  }
  return parseShareDetail(body)
}

export async function reactToPortfolioShare(shareId: number, type: "LIKE" | "DISLIKE"): Promise<PortfolioShareDetail> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}/reactions`, {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify({ type }),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "공유 반응을 저장하지 못했습니다."))
  }
  return parseShareDetail(body)
}

export const PORTFOLIO_REPORT_REASONS = [
  "오픈채팅 URL",
  "과장 수익문구",
  "리딩방 패턴",
  "투자 사기",
  "개인정보",
  "광고",
] as const

export type PortfolioDetectionChip = {
  label: string
  count: number
}

export type PortfolioDetection = {
  source: string
  note: string
  chips: PortfolioDetectionChip[]
}

export async function fetchPortfolioDetections(): Promise<PortfolioDetection> {
  const res = await fetch("/api/v1/admin/portfolio/detections", {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "공유 자동 탐지를 불러오지 못했습니다."))
  }
  const data = asRecord(asRecord(body)?.data)
  const chips = Array.isArray(data?.chips) ? data.chips : []
  return {
    source: typeof data?.source === "string" ? data.source : "RULE",
    note: typeof data?.note === "string" ? data.note : "공개 공유 글의 문구와 신고 사유로 집계했습니다.",
    chips: chips.flatMap((item) => {
      const row = asRecord(item)
      const label = typeof row?.label === "string" ? row.label : ""
      const count = typeof row?.count === "number" ? row.count : 0
      return label && count > 0 ? [{ label, count }] : []
    }),
  }
}

export async function reportPortfolioShare(shareId: number, reason: string, note: string): Promise<PortfolioShareDetail> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}/reports`, {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify({ reason, note }),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "공유 게시물을 신고하지 못했습니다."))
  }
  return parseShareDetail(body)
}

export async function moderatePortfolioShare(
  shareId: number,
  status: "WARN" | "BLIND" | "REMOVED",
): Promise<PortfolioShareCard> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}/moderation`, {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify({ status }),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "공유 게시물을 처리하지 못했습니다."))
  }
  const data = asRecord(asRecord(body)?.data)
  if (!data || typeof data.id !== "number") {
    throw new Error(messageOf(body, "공유 게시물을 처리하지 못했습니다."))
  }
  return data as unknown as PortfolioShareCard
}

export async function publishPortfolioShare(request: PortfolioShareRequest): Promise<PortfolioSummary> {
  const res = await fetch("/api/v1/portfolio/shares", {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify(request),
    cache: "no-store",
  })
  return parseSummary(res, "포트폴리오를 공유하지 못했습니다.")
}

export async function fetchPortfolioShareComments(shareId: number): Promise<PortfolioShareComment[]> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}/comments`, {
    method: "GET",
    credentials: "include",
    headers: authHeadersJson(),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "공유 댓글을 불러오지 못했습니다."))
  }
  const data = asRecord(body)?.data
  return Array.isArray(data) ? data.flatMap(parseShareComment) : []
}

export async function addPortfolioShareComment(shareId: number, content: string): Promise<PortfolioShareComment[]> {
  const res = await fetch(`/api/v1/portfolio/shares/${shareId}/comments`, {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify({ content }),
    cache: "no-store",
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new Error(messageOf(body, "댓글을 등록하지 못했습니다."))
  }
  const data = asRecord(body)?.data
  return Array.isArray(data) ? data.flatMap(parseShareComment) : []
}

function parseShareComment(item: unknown): PortfolioShareComment[] {
  const row = asRecord(item)
  if (!row || typeof row.id !== "number" || typeof row.content !== "string") {
    return []
  }
  return [{
    id: row.id,
    authorName: typeof row.authorName === "string" ? row.authorName : "회*",
    content: row.content,
    createdAt: typeof row.createdAt === "string" ? row.createdAt : "",
  }]
}

function parseShareView(raw: unknown, card: PortfolioShareCard): PortfolioShareView {
  const view = asRecord(raw)
  if (!view) {
    return fallbackShareView(card)
  }
  const assets = Array.isArray(view.assets) ? view.assets.flatMap(parseAssetLine) : []
  const history = Array.isArray(view.history) ? view.history.flatMap(parseHistoryLine) : []
  const amount = textOf(view.amountLabel, card.amountLabel || "비율만")
  return {
    showNetWorth: view.showNetWorth === true,
    showMonthRate: view.showMonthRate === true,
    showAllocation: view.showAllocation === true,
    showGoal: view.showGoal === true,
    showExactNames: view.showExactNames === true,
    showDebt: view.showDebt === true,
    showPrincipal: view.showPrincipal === true,
    showProfit: view.showProfit === true,
    amountMode: textOf(view.amountMode, "BAND"),
    visibility: textOf(view.visibility, "PUBLIC"),
    message: textOf(view.message, card.message),
    goalLabel: textOf(view.goalLabel, card.goalLabel),
    amountLabel: amount,
    progressPercent: typeof view.progressPercent === "number" ? view.progressPercent : card.progressPercent,
    monthRateLabel: typeof view.monthRateLabel === "string" ? view.monthRateLabel : card.monthRateLabel,
    recordMonth: textOf(view.recordMonth, ""),
    totalAssetsLabel: textOf(view.totalAssetsLabel, amount),
    totalLiabilitiesLabel: textOf(view.totalLiabilitiesLabel, "비공개"),
    netWorthLabel: textOf(view.netWorthLabel, amount),
    assets,
    history,
  }
}

function parseAssetLine(item: unknown): PortfolioShareAssetLine[] {
  const row = asRecord(item)
  if (!row) {
    return []
  }
  return [{
    category: textOf(row.category, "기타"),
    name: textOf(row.name, ""),
    amountLabel: textOf(row.amountLabel, "비율만"),
    acquisitionLabel: textOf(row.acquisitionLabel, ""),
    quantityLabel: textOf(row.quantityLabel, ""),
    unitPriceLabel: textOf(row.unitPriceLabel, ""),
    profitLabel: textOf(row.profitLabel, ""),
  }]
}

function parseHistoryLine(item: unknown): PortfolioShareHistoryLine[] {
  const row = asRecord(item)
  if (!row) {
    return []
  }
  return [{
    month: textOf(row.month, ""),
    netWorthLabel: textOf(row.netWorthLabel, "비율만"),
    deltaLabel: textOf(row.deltaLabel, "-"),
    statusLabel: textOf(row.statusLabel, "확정"),
  }]
}

function fallbackShareView(card: PortfolioShareCard): PortfolioShareView {
  const amount = card.amountLabel || "비율만"
  return {
    showNetWorth: Boolean(card.amountLabel),
    showMonthRate: Boolean(card.monthRateLabel),
    showAllocation: card.showAsset,
    showGoal: Boolean(card.goalRateLabel),
    showExactNames: false,
    showDebt: card.showDebt,
    showPrincipal: false,
    showProfit: false,
    amountMode: amount.includes("비율") ? "RATIO" : amount.includes("약") || amount.includes("만원") ? "BAND" : "EXACT",
    visibility: "PUBLIC",
    message: card.message,
    goalLabel: card.goalLabel,
    amountLabel: amount,
    progressPercent: card.progressPercent,
    monthRateLabel: card.monthRateLabel,
    recordMonth: card.sharedAt,
    totalAssetsLabel: card.showAsset ? amount : "비공개",
    totalLiabilitiesLabel: card.showDebt ? "공개" : "비공개",
    netWorthLabel: amount,
    assets: card.showAsset
      ? [{ category: "자산 구성", name: "", amountLabel: amount, acquisitionLabel: "", quantityLabel: "", unitPriceLabel: "", profitLabel: "" }]
      : [],
    history: [{ month: card.sharedAt, netWorthLabel: amount, deltaLabel: card.monthRateLabel || "-", statusLabel: "확정" }],
  }
}

function textOf(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim() ? value : fallback
}

export async function recordPortfolioMonth(memo: string): Promise<PortfolioSummary> {
  const res = await fetch("/api/v1/portfolio/snapshots", {
    method: "POST",
    credentials: "include",
    headers: authHeadersJson(),
    body: JSON.stringify({ memo }),
    cache: "no-store",
  })
  return parseSummary(res, "이번 달 기록을 저장하지 못했습니다.")
}
