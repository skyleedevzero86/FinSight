export type LiveVodFavorite = {
  videoId: string
  title: string
  channelTitle: string | null
  thumbnailUrl: string
  tab: string | null
  savedAt: string
}

const STORAGE_KEY = "finsight.liveVod.favorites.v1"

function canUseStorage(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined"
}

function readAll(): LiveVodFavorite[] {
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
          savedAt: typeof o.savedAt === "string" ? o.savedAt : new Date().toISOString(),
        } satisfies LiveVodFavorite
      })
      .filter((v): v is LiveVodFavorite => v != null)
  } catch {
    return []
  }
}

function writeAll(items: LiveVodFavorite[]) {
  if (!canUseStorage()) return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  window.dispatchEvent(new CustomEvent("finsight:live-vod-favorites-changed"))
}

export function listLiveVodFavorites(): LiveVodFavorite[] {
  return readAll().sort((a, b) => (a.savedAt < b.savedAt ? 1 : -1))
}

export function isLiveVodFavorite(videoId: string): boolean {
  if (!videoId) return false
  return readAll().some((item) => item.videoId === videoId)
}

export function toggleLiveVodFavorite(input: {
  videoId: string
  title: string
  channelTitle?: string | null
  thumbnailUrl?: string | null
  tab?: string | null
}): boolean {
  const videoId = input.videoId.trim()
  if (!videoId) return false
  const current = readAll()
  const exists = current.some((item) => item.videoId === videoId)
  if (exists) {
    writeAll(current.filter((item) => item.videoId !== videoId))
    return false
  }
  writeAll([
    {
      videoId,
      title: input.title || "VOD",
      channelTitle: input.channelTitle ?? null,
      thumbnailUrl:
        input.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg`,
      tab: input.tab ?? null,
      savedAt: new Date().toISOString(),
    },
    ...current,
  ])
  return true
}

export function removeLiveVodFavorite(videoId: string) {
  writeAll(readAll().filter((item) => item.videoId !== videoId))
}
