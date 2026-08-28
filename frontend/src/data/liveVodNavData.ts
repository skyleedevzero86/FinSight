export type LiveVodNavItem = {
  label: string
  tab: string
  href: string
}

export const LIVE_VOD_NAV_ITEMS: LiveVodNavItem[] = [
  { label: "전체", tab: "ALL", href: "/live-vod" },
  { label: "라이브", tab: "LIVE", href: "/live-vod?tab=LIVE" },
  { label: "시장브리핑", tab: "MARKET", href: "/live-vod?tab=MARKET" },
  { label: "박곰희 TV", tab: "GOMHEE", href: "/live-vod?tab=GOMHEE" },
  { label: "부티플", tab: "BOOTYFUL", href: "/live-vod?tab=BOOTYFUL" },
  { label: "테마분석", tab: "THEME", href: "/live-vod?tab=THEME" },
  { label: "글로벌매크로", tab: "MACRO", href: "/live-vod?tab=MACRO" },
  { label: "슈카월드", tab: "SYUKA", href: "/live-vod?tab=SYUKA" },
]
