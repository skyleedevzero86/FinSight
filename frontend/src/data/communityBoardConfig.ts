import type { BoardTypeCode } from "@/lib/boardApi"

export const COMMUNITY_SECTION_BOARD_TYPE: Record<
  "notice" | "free" | "qna",
  BoardTypeCode
> = {
  notice: "NOTICE",
  free: "FREE",
  qna: "QNA",
}

export type CommunityBoardSection = keyof typeof COMMUNITY_SECTION_BOARD_TYPE

export function boardListHref(
  basePath: string,
  opts: {
    page?: number
    search_type?: string
    search_value?: string
    extra?: Record<string, string | undefined>
  },
): string {
  const p = new URLSearchParams()
  if (opts.page && opts.page > 1) p.set("page", String(opts.page))
  if (opts.search_type) p.set("search_type", opts.search_type)
  if (opts.search_value) p.set("search_value", opts.search_value)
  if (opts.extra) {
    for (const [k, v] of Object.entries(opts.extra)) {
      if (v) p.set(k, v)
    }
  }
  const q = p.toString()
  return q ? `${basePath}?${q}` : basePath
}

export function pageWindow(current: number, total: number, width: number): number[] {
  if (total <= 0) return [1]
  const half = Math.floor(width / 2)
  let start = Math.max(1, current - half)
  const end = Math.min(total, start + width - 1)
  start = Math.max(1, end - width + 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}
