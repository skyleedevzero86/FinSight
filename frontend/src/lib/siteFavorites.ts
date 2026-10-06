export type SiteFavoriteCategory = "NEWS" | "ECONOMY_PICK" | "LIVE_VOD" | "COMMUNITY"

export type SiteFavoriteItem = {
  key: string
  category: SiteFavoriteCategory
  href: string
  title: string
  subtitle: string | null
  thumbnailUrl: string
  savedAt: string
}

const STORAGE_KEY = "finsight.site.favorites.v1"
const CHANGED_EVENT = "finsight:site-favorites-changed"

export const SITE_FAVORITE_PLACEHOLDER =
  "data:image/svg+xml," +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="480" height="270" viewBox="0 0 480 270">` +
      `<rect fill="#e8eef0" width="480" height="270"/>` +
      `<rect fill="#d2dde0" x="168" y="78" width="144" height="114" rx="8"/>` +
      `<rect fill="#f7fafa" x="186" y="96" width="108" height="10" rx="2"/>` +
      `<rect fill="#f7fafa" x="186" y="116" width="88" height="8" rx="2"/>` +
      `<rect fill="#f7fafa" x="186" y="134" width="96" height="8" rx="2"/>` +
      `</svg>`,
  )

function canUseStorage(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined"
}

function normalize(row: unknown): SiteFavoriteItem | null {
  if (!row || typeof row !== "object") return null
  const o = row as Record<string, unknown>
  if (typeof o.key !== "string" || !o.key) return null
  if (typeof o.href !== "string" || !o.href) return null
  if (typeof o.title !== "string" || !o.title) return null
  const category = o.category
  if (
    category !== "NEWS" &&
    category !== "ECONOMY_PICK" &&
    category !== "LIVE_VOD" &&
    category !== "COMMUNITY"
  ) {
    return null
  }
  return {
    key: o.key,
    category,
    href: o.href,
    title: o.title,
    subtitle: typeof o.subtitle === "string" ? o.subtitle : null,
    thumbnailUrl:
      typeof o.thumbnailUrl === "string" && o.thumbnailUrl
        ? o.thumbnailUrl
        : SITE_FAVORITE_PLACEHOLDER,
    savedAt: typeof o.savedAt === "string" ? o.savedAt : new Date().toISOString(),
  }
}

function readAll(): SiteFavoriteItem[] {
  if (!canUseStorage()) return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.map(normalize).filter((v): v is SiteFavoriteItem => v != null)
  } catch {
    return []
  }
}

function writeAll(items: SiteFavoriteItem[]) {
  if (!canUseStorage()) return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  window.dispatchEvent(new CustomEvent(CHANGED_EVENT))
}

export function listSiteFavorites(category?: SiteFavoriteCategory): SiteFavoriteItem[] {
  const all = readAll().sort((a, b) => (a.savedAt < b.savedAt ? 1 : -1))
  if (!category) return all
  return all.filter((item) => item.category === category)
}

export function removeSiteFavorite(key: string) {
  writeAll(readAll().filter((item) => item.key !== key))
}

export const SITE_FAVORITES_CHANGED_EVENT = CHANGED_EVENT
