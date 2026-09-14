const DUMMY_HOST_MARKER = "dummy.finsight.test"

const MAINIMG_PLACEHOLDERS = [
  "https://images.unsplash.com/photo-1516307365426-bea591f05011?w=1920&h=600&fit=crop",
  "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=1920&h=600&fit=crop",
  "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=1920&h=600&fit=crop",
  "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=1920&h=600&fit=crop",
  "https://images.unsplash.com/photo-1495020689067-958852a7765e?w=1920&h=600&fit=crop",
  "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=1920&h=600&fit=crop",
] as const

const POPUP_PLACEHOLDERS = [
  "https://images.unsplash.com/photo-1557804506-669a67965ba0?w=640&h=800&fit=crop",
  "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=640&h=800&fit=crop",
  "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=640&h=800&fit=crop",
] as const

function isDummyAssetUrl(url: string): boolean {
  return url.includes(DUMMY_HOST_MARKER)
}

function placeholderFromList(list: readonly string[], index: number): string {
  if (list.length === 0) return ""
  const safe = ((index % list.length) + list.length) % list.length
  return list[safe]
}

export function rewriteDummyAssetUrl(url: string | null | undefined): string {
  const trimmed = (url || "").trim()
  if (!trimmed) return ""
  if (!isDummyAssetUrl(trimmed) && !trimmed.startsWith("mainimg/") && !trimmed.startsWith("popup/")) {
    return trimmed
  }

  const mainimgMatch = trimmed.match(/(?:mainimg\/|\/mainimg\/)(\d+)\.jpe?g/i)
  if (mainimgMatch) {
    const n = Number(mainimgMatch[1])
    return placeholderFromList(MAINIMG_PLACEHOLDERS, Number.isFinite(n) ? n - 1 : 0)
  }

  const popupMatch = trimmed.match(/(?:popup\/|\/popup\/)(?:.*?)(\d+)/i)
  if (popupMatch || trimmed.includes("/popup/") || trimmed.startsWith("popup/")) {
    const n = popupMatch ? Number(popupMatch[1]) : 1
    return placeholderFromList(POPUP_PLACEHOLDERS, Number.isFinite(n) ? n - 1 : 0)
  }

  if (isDummyAssetUrl(trimmed)) {
    return MAINIMG_PLACEHOLDERS[0]
  }

  return trimmed
}

export function rewriteDummyLinkUrl(url: string | null | undefined): string {
  const trimmed = (url || "").trim()
  if (!trimmed) return ""
  if (isDummyAssetUrl(trimmed)) return ""
  return trimmed
}
