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
}

export async function fetchPortfolioShares(page: number): Promise<PortfolioShareFeed> {
  const res = await fetch(`/api/v1/portfolio/shares?page=${page}&size=2`, {
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
