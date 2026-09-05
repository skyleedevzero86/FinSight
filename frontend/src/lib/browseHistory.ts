export type BrowseHistoryKind = "LIVE_VOD" | "BOARD"

export type BrowseHistoryItem = {
  key: string
  kind: BrowseHistoryKind
  href: string
  title: string
  subtitle: string | null
  thumbnailUrl: string
  viewedAt: string
}

const STORAGE_KEY = "finsight.browse.history.v1"
const LEGACY_LIVE_VOD_KEY = "finsight.liveVod.history.v1"
const MAX_ITEMS = 120
const CHANGED_EVENT = "finsight:browse-history-changed"

export const BOARD_HISTORY_PLACEHOLDER =
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

function boardTypeLabel(boardType: string | null | undefined): string {
  const t = (boardType || "").toUpperCase()
  if (t === "NOTICE") return "공지사항"
  if (t === "QNA") return "Q&A"
  if (t === "FREE") return "포트폴리오"
  if (t === "COMMUNITY") return "커뮤니티"
  if (t === "MEDIA") return "미디어"
  return "게시물"
}

export function extractContentThumbnail(content: string | null | undefined): string | null {
  if (!content) return null
  const md = content.match(/!\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/)
  if (md?.[1] && !md[1].startsWith("#")) return md[1]
  const html = content.match(/<img[^>]+src=["']([^"']+)["']/i)
  if (html?.[1]) return html[1]
  return null
}

function normalizeItem(row: unknown): BrowseHistoryItem | null {
  if (!row || typeof row !== "object") return null
  const o = row as Record<string, unknown>
  if (typeof o.key !== "string" || !o.key) return null
  if (typeof o.href !== "string" || !o.href) return null
  if (typeof o.title !== "string" || !o.title) return null
  const kind: BrowseHistoryKind = o.kind === "BOARD" ? "BOARD" : "LIVE_VOD"
  return {
    key: o.key,
    kind,
    href: o.href,
    title: o.title,
    subtitle: typeof o.subtitle === "string" ? o.subtitle : null,
    thumbnailUrl:
      typeof o.thumbnailUrl === "string" && o.thumbnailUrl
        ? o.thumbnailUrl
        : BOARD_HISTORY_PLACEHOLDER,
    viewedAt: typeof o.viewedAt === "string" ? o.viewedAt : new Date().toISOString(),
  }
}

function migrateLegacyLiveVod(): BrowseHistoryItem[] {
  if (!canUseStorage()) return []
  try {
    const raw = window.localStorage.getItem(LEGACY_LIVE_VOD_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((row) => {
        if (!row || typeof row !== "object") return null
        const o = row as Record<string, unknown>
        if (typeof o.videoId !== "string" || !o.videoId) return null
        const videoId = o.videoId
        return {
          key: `live-vod:${videoId}`,
          kind: "LIVE_VOD" as const,
          href: `/live-vod/watch/${encodeURIComponent(videoId)}?tab=HISTORY`,
          title: typeof o.title === "string" ? o.title : "VOD",
          subtitle: typeof o.channelTitle === "string" ? o.channelTitle : null,
          thumbnailUrl:
            typeof o.thumbnailUrl === "string" && o.thumbnailUrl
              ? o.thumbnailUrl
              : `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
          viewedAt: typeof o.watchedAt === "string" ? o.watchedAt : new Date().toISOString(),
        } satisfies BrowseHistoryItem
      })
      .filter((v): v is BrowseHistoryItem => v != null)
  } catch {
    return []
  }
}

function readAll(): BrowseHistoryItem[] {
  if (!canUseStorage()) return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      const legacy = migrateLegacyLiveVod()
      if (legacy.length > 0) writeAll(legacy)
      return legacy
    }
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed.map(normalizeItem).filter((v): v is BrowseHistoryItem => v != null)
  } catch {
    return []
  }
}

function writeAll(items: BrowseHistoryItem[]) {
  if (!canUseStorage()) return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items.slice(0, MAX_ITEMS)))
  window.dispatchEvent(new CustomEvent(CHANGED_EVENT))
}

export function listBrowseHistory(): BrowseHistoryItem[] {
  return readAll().sort((a, b) => (a.viewedAt < b.viewedAt ? 1 : -1))
}

export function recordBrowseView(input: {
  key: string
  kind: BrowseHistoryKind
  href: string
  title: string
  subtitle?: string | null
  thumbnailUrl?: string | null
}): void {
  const key = input.key.trim()
  const href = input.href.trim()
  if (!key || !href) return
  const next: BrowseHistoryItem = {
    key,
    kind: input.kind,
    href,
    title: input.title || "게시물",
    subtitle: input.subtitle ?? null,
    thumbnailUrl: input.thumbnailUrl || BOARD_HISTORY_PLACEHOLDER,
    viewedAt: new Date().toISOString(),
  }
  const rest = readAll().filter((item) => item.key !== key)
  writeAll([next, ...rest])
}

export function recordLiveVodBrowseView(input: {
  videoId: string
  title: string
  channelTitle?: string | null
  thumbnailUrl?: string | null
  tab?: string | null
}): void {
  const videoId = input.videoId.trim()
  if (!videoId) return
  const tab =
    input.tab && input.tab !== "FAVORITES" && input.tab !== "HISTORY" ? input.tab : "HISTORY"
  recordBrowseView({
    key: `live-vod:${videoId}`,
    kind: "LIVE_VOD",
    href: `/live-vod/watch/${encodeURIComponent(videoId)}?tab=${encodeURIComponent(tab)}`,
    title: input.title || "VOD",
    subtitle: input.channelTitle ?? null,
    thumbnailUrl: input.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
  })
}

export function recordBoardBrowseView(input: {
  boardId: number
  title: string
  boardType?: string | null
  basePath: string
  content?: string | null
  thumbnailUrl?: string | null
}): void {
  if (!Number.isFinite(input.boardId) || input.boardId <= 0) return
  const base = input.basePath.replace(/\/$/, "")
  recordBrowseView({
    key: `board:${input.boardId}`,
    kind: "BOARD",
    href: `${base}/${input.boardId}`,
    title: input.title || "게시물",
    subtitle: boardTypeLabel(input.boardType),
    thumbnailUrl:
      input.thumbnailUrl || extractContentThumbnail(input.content) || BOARD_HISTORY_PLACEHOLDER,
  })
}

export function removeBrowseHistory(key: string) {
  writeAll(readAll().filter((item) => item.key !== key))
}

export function clearBrowseHistory() {
  writeAll([])
}

export const BROWSE_HISTORY_CHANGED_EVENT = CHANGED_EVENT
