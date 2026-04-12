export type LiveVodNavItem = {
  label: string
  tab: string
  href: string
}

export const LIVE_VOD_NAV_ITEMS: LiveVodNavItem[] = [
  { label: "전체", tab: "ALL", href: "/live-vod" },
  { label: "시장브리핑", tab: "MARKET", href: "/live-vod?tab=MARKET" },
  { label: "테마분석", tab: "THEME", href: "/live-vod?tab=THEME" },
  { label: "종목분석", tab: "STOCK", href: "/live-vod?tab=STOCK" },
  { label: "실적/기업이슈", tab: "CORP", href: "/live-vod?tab=CORP" },
  { label: "투자상식", tab: "KNOWLEDGE", href: "/live-vod?tab=KNOWLEDGE" },
  { label: "글로벌매크로", tab: "MACRO", href: "/live-vod?tab=MACRO" },
]
