export type LiveVodHistoryItem = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  tab: string | null
  watchUrl: string
  watchedAt: string
}

const STORAGE_KEY = "finsight.liveVod.history.v1"
const MAX_ITEMS = 60
const CHANGED_EVENT = "finsight:live-vod-history-changed"

function canUseStorage(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined"
}

function youtubeWatchUrl(videoId: string): string {
  return `https://www.youtube.com/watch?v=${videoId}`
}

function readAll(): LiveVodHistoryItem[] {
  if (!canUseStorage()) return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((row) => {
        if (!row || typeof row !== "object") return null
        const o = row as Record<string, unknown>
        if (typeof o.videoId !== "string" || !o.videoId) return null
        return {
          videoId: o.videoId,
          title: typeof o.title === "string" ? o.title : "VOD",
          channelTitle: typeof o.channelTitle === "string" ? o.channelTitle : null,
          thumbnailUrl:
            typeof o.thumbnailUrl === "string" && o.thumbnailUrl
              ? o.thumbnailUrl
              : `https://i.ytimg.com/vi/${o.videoId}/mqdefault.jpg`,
          tab: typeof o.tab === "string" ? o.tab : null,
          watchUrl:
            typeof o.watchUrl === "string" && o.watchUrl
              ? o.watchUrl
              : youtubeWatchUrl(o.videoId),
          watchedAt: typeof o.watchedAt === "string" ? o.watchedAt : new Date().toISOString(),
        } satisfies LiveVodHistoryItem
      })
      .filter((v): v is LiveVodHistoryItem => v != null)
  } catch {
    return []
  }
}

function writeAll(items: LiveVodHistoryItem[]) {
  if (!canUseStorage()) return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items.slice(0, MAX_ITEMS)))
  window.dispatchEvent(new CustomEvent(CHANGED_EVENT))
}

export function listLiveVodHistory(): LiveVodHistoryItem[] {
  return readAll().sort((a, b) => (a.watchedAt < b.watchedAt ? 1 : -1))
}

export function recordLiveVodWatch(input: {
  videoId: string
  title: string
  channelTitle?: string | null
  thumbnailUrl?: string | null
  tab?: string | null
  watchUrl?: string | null
}): void {
  const videoId = input.videoId.trim()
  if (!videoId) return
  const next: LiveVodHistoryItem = {
    videoId,
    title: input.title || "VOD",
    channelTitle: input.channelTitle ?? null,
    thumbnailUrl:
      input.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    tab: input.tab ?? null,
    watchUrl: input.watchUrl || youtubeWatchUrl(videoId),
    watchedAt: new Date().toISOString(),
  }
  const rest = readAll().filter((item) => item.videoId !== videoId)
  writeAll([next, ...rest])
}

export function removeLiveVodHistory(videoId: string) {
  writeAll(readAll().filter((item) => item.videoId !== videoId))
}

export function clearLiveVodHistory() {
  writeAll([])
}

export const LIVE_VOD_HISTORY_CHANGED_EVENT = CHANGED_EVENT
