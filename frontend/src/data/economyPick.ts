export type EconomyMarketPill = {
  id: string
  label: string
  value: string
  change: string
  direction: "up" | "down" | "flat"
}

export const ECONOMY_MARKET_PILLS: EconomyMarketPill[] = [
  { id: "kospi", label: "코스피", value: "2,650.12", change: "+0.82%", direction: "up" },
  { id: "kosdaq", label: "코스닥", value: "820.45", change: "+1.12%", direction: "up" },
  { id: "usd", label: "달러/원", value: "1,382.50", change: "-0.15%", direction: "down" },
  { id: "gold", label: "금(현물)", value: "$2,685", change: "+0.31%", direction: "up" },
  { id: "wti", label: "WTI", value: "$71.20", change: "-0.48%", direction: "down" },
]

export const ECONOMY_SECTION_TABS = [
  { id: "all", label: "전체", href: "#" },
  { id: "market", label: "증시", href: "#" },
  { id: "macro", label: "거시", href: "#" },
  { id: "industry", label: "산업", href: "#" },
  { id: "realestate", label: "부동산", href: "#" },
] as const
