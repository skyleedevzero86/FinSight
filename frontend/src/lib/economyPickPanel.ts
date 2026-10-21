import { authHeadersJson } from "@/lib/finsightToken"

export type EconomyPickAssets = {
  source: "MINE" | "SAMPLE" | string
  equityPercent: number
  debtPercent: number
  note: string
}

export type EconomyPickSalesSlice = {
  label: string
  percent: number
  sign: "plus" | "minus" | string
}

export type EconomyPickSales = {
  symbol: string
  name: string
  note: string
  slices: EconomyPickSalesSlice[]
}

type Json = Record<string, unknown>

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Json
  return null
}

function numberOf(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0
}

export function percentText(value: number): string {
  return `${value.toFixed(1)}%`
}

export function donutBackground(stops: { color: string; percent: number }[]): string {
  let cursor = 0
  const parts = stops.map((stop) => {
    const start = cursor
    cursor += Math.max(0, stop.percent)
    return `${stop.color} ${start}% ${cursor}%`
  })
  return `conic-gradient(${parts.join(", ")})`
}

type MixBook = { headline: string; labels: string[]; weights: number[] }

const SPACE: MixBook = {
  headline: "우주관광, 발사, 정부 수주 순으로 매출이 나뉩니다.",
  labels: ["우주관광", "발사 서비스", "정부·위성 수주", "기타"],
  weights: [28, 34, 26, 12],
}
const DEFENSE: MixBook = {
  headline: "항공체계와 미사일 비중이 큰 방산 매출로 추정합니다.",
  labels: ["항공체계", "미사일·화력", "우주·위성", "정비 서비스"],
  weights: [32, 28, 22, 18],
}
const CHIP: MixBook = {
  headline: "데이터센터 수요가 매출의 중심으로 추정됩니다.",
  labels: ["데이터센터", "소비자 기기", "자동차·산업", "기타"],
  weights: [46, 22, 18, 14],
}
const FINANCE: MixBook = {
  headline: "이자이익과 수수료가 매출의 대부분으로 추정됩니다.",
  labels: ["이자이익", "수수료", "트레이딩", "기타"],
  weights: [44, 28, 16, 12],
}
const ENERGY: MixBook = {
  headline: "원유·가스 생산이 매출의 중심입니다.",
  labels: ["원유·가스", "정제", "화학", "기타"],
  weights: [48, 24, 16, 12],
}
const DISPLAY: MixBook = {
  headline: "TV·모니터, 메모리, OLED 순으로 매출이 나뉩니다.",
  labels: ["TV, 모니터 [DX부문]", "DRAM, NAND [DS부문]", "스마트폰용 OLED [SDC]", "부문간 내부거래 [기타]"],
  weights: [66, 26, 12, 9],
}
const GENERAL: MixBook = {
  headline: "주력 제품과 용역 중심으로 매출이 나뉩니다.",
  labels: ["주력 제품", "용역", "수출", "기타"],
  weights: [48, 27, 18, 7],
}

function containsAny(text: string, words: string[]): boolean {
  return words.some((word) => text.includes(word))
}

function mixBook(text: string): MixBook {
  if (containsAny(text, ["버진", "갤럭틱", "spce", "우주", "로켓", "rklb", "asts", "lunr", "astr", "spcx", "arkx", "rdw"])) return SPACE
  if (containsAny(text, ["방산", "록히드", "lmt", "noc", "rtx", "boeing", "보잉", "레이시온"])) return DEFENSE
  if (containsAny(text, ["반도체", "엔비디아", "nvda", "하이닉스", "mu", "tsm", "amd", "avgo", "asml", "qcom"])) return CHIP
  if (containsAny(text, ["금융", "은행", "jpm", "모건"])) return FINANCE
  if (containsAny(text, ["석유", "xom", "엑슨"])) return ENERGY
  if (containsAny(text, ["삼성", "005930", "samsung"])) return DISPLAY
  return GENERAL
}

function scaleWeights(weights: number[]): number[] {
  const sum = weights.reduce((total, weight) => total + Math.max(weight, 0), 0)
  const scaled = weights.slice(0, -1).map((weight) => Math.round((Math.max(weight, 0) * 100) / sum))
  const used = scaled.reduce((total, weight) => total + weight, 0)
  return [...scaled, Math.max(0, 100 - used)]
}

export function sampleAssets(): EconomyPickAssets {
  const debt = 12 + Math.floor(Math.random() * 36)
  return {
    source: "SAMPLE",
    equityPercent: 100 - debt,
    debtPercent: debt,
    note: "보유 자산이 없어 예시 비율을 보여 줍니다.",
  }
}

export function predictSales(name: string, symbol: string): EconomyPickSales {
  const label = name.trim() || symbol.trim() || "종목"
  const book = mixBook(`${label} ${symbol}`.toLowerCase())
  const weights = scaleWeights(book.weights)
  return {
    symbol,
    name: label,
    note: `${label} 선택 시 ${book.headline}`,
    slices: book.labels.map((item, index) => ({
      label: item,
      percent: weights[index] ?? 0,
      sign: "plus",
    })),
  }
}

export async function fetchEconomyPickAssets(): Promise<EconomyPickAssets | null> {
  try {
    const res = await fetch("/api/v1/economy-pick/assets", {
      credentials: "include",
      headers: authHeadersJson(),
      cache: "no-store",
    })
    if (!res.ok) return null
    const data = asRecord(asRecord(await res.json())?.data)
    if (!data) return null
    return {
      source: typeof data.source === "string" ? data.source : "SAMPLE",
      equityPercent: numberOf(data.equityPercent),
      debtPercent: numberOf(data.debtPercent),
      note: typeof data.note === "string" ? data.note : "",
    }
  } catch {
    return null
  }
}

export async function fetchEconomyPickSales(name: string, symbol: string): Promise<EconomyPickSales | null> {
  try {
    const query = new URLSearchParams({ name, symbol })
    const res = await fetch(`/api/v1/economy-pick/sales?${query}`, { cache: "no-store" })
    if (!res.ok) return null
    const data = asRecord(asRecord(await res.json())?.data)
    if (!data || !Array.isArray(data.slices)) return null
    const slices = data.slices.flatMap((row) => {
      const record = asRecord(row)
      if (!record || typeof record.label !== "string") return []
      return [{
        label: record.label,
        percent: numberOf(record.percent),
        sign: record.sign === "minus" ? "minus" : "plus",
      }]
    })
    if (slices.length === 0) return null
    return {
      symbol: typeof data.symbol === "string" ? data.symbol : symbol,
      name: typeof data.name === "string" ? data.name : name,
      note: typeof data.note === "string" ? data.note : "",
      slices,
    }
  } catch {
    return null
  }
}
