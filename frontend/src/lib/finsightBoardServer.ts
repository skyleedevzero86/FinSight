import type {
  BoardDetail,
  BoardListItem,
  BoardPagination,
  BoardTypeCode,
} from "@/lib/boardApi"
import { unwrapApiData } from "@/lib/boardApi"
import type { BoardRow } from "@/data/communityBoardData"

function getServerBase(): string | null {
  const base = process.env.FINSIGHT_API_BASE_URL?.replace(/\/$/, "")
  return base || null
}

export type BoardListFetchInput = {
  boardType: BoardTypeCode
  page: number
  size: number
  keyword?: string
}

export async function fetchBoardListServer(
  input: BoardListFetchInput,
): Promise<BoardPagination | null> {
  const base = getServerBase()
  if (!base) return null
  const params = new URLSearchParams()
  params.set("boardType", input.boardType)
  params.set("page", String(Math.max(0, input.page)))
  params.set("size", String(Math.min(100, Math.max(1, input.size))))
  const kw = input.keyword?.trim()
  if (kw) params.set("keyword", kw)
  const res = await fetch(`${base}/api/v1/boards?${params.toString()}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: 0 },
  })
  if (!res.ok) return null
  let json: unknown
  try {
    json = await res.json()
  } catch {
    return null
  }
  return unwrapApiData<BoardPagination>(json)
}

export async function fetchBoardDetailServer(
  boardId: number,
): Promise<BoardDetail | null> {
  const base = getServerBase()
  if (!base) return null
  const res = await fetch(`${base}/api/v1/boards/${boardId}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: 0 },
  })
  if (!res.ok) return null
  let json: unknown
  try {
    json = await res.json()
  } catch {
    return null
  }
  return unwrapApiData<BoardDetail>(json)
}

export function mapListToBoardRows(
  items: BoardListItem[],
  basePath: string,
): BoardRow[] {
  return items.map((b) => ({
    id: b.id,
    num: b.id,
    title: b.title,
    href: `${basePath}/${b.id}`,
    author: formatAuthorServer(b.authorEmail),
    date: formatDateServer(b.createdAt),
    hits: b.viewCount,
    hasFile: false,
  }))
}

function formatAuthorServer(email: string): string {
  if (!email) return ""
  const at = email.indexOf("@")
  if (at <= 0) return email
  const local = email.slice(0, at)
  if (local.length <= 2) return `${local[0] ?? ""}*@${email.slice(at + 1)}`
  return `${local.slice(0, 2)}***@${email.slice(at + 1)}`
}

function formatDateServer(iso: string): string {
  if (!iso) return ""
  return iso.slice(0, 10).replace(/-/g, ".").slice(2)
}
