export type BoardTypeCode = "NOTICE" | "FREE" | "QNA" | "COMMUNITY" | "MEDIA"

export type BoardListItem = {
  id: number
  title: string
  authorEmail: string
  boardType: BoardTypeCode
  status: string
  viewCount: number
  likeCount: number
  dislikeCount: number
  commentCount: number
  hashtags: string[]
  timeAgo: string
  createdAt: string
  updatedAt: string
}

export type BoardPagination = {
  content: BoardListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext?: boolean
  hasPrevious?: boolean
}

export type BoardDetail = {
  id: number
  title: string
  content: string
  renderedHtml: string
  plainTextPreview: string
  authorEmail: string
  boardType: BoardTypeCode
  status: string
  viewCount: number
  likeCount: number
  dislikeCount: number
  commentCount: number
  reportCount: number
  hashtags: string[]
  files: unknown[]
  createdAt: string
  updatedAt: string
  navigation: {
    previous: {
      id: number
      title: string
      authorEmail: string
      createdAt: string
    } | null
    next: {
      id: number
      title: string
      authorEmail: string
      createdAt: string
    } | null
  } | null
}

export type ApiEnvelope<T> = {
  success?: boolean
  data?: T
  message?: string
}

export function unwrapApiData<T>(payload: unknown): T | null {
  if (!payload || typeof payload !== "object") return null
  const o = payload as ApiEnvelope<T>
  if (o.data === undefined || o.data === null) return null
  return o.data
}

export function formatBoardDate(iso: string): string {
  if (!iso) return ""
  const d = iso.slice(0, 10).replace(/-/g, ".")
  if (d.length >= 8) return d.slice(2)
  return d
}

export function formatAuthor(email: string): string {
  if (!email) return ""
  const at = email.indexOf("@")
  if (at <= 0) return email
  const local = email.slice(0, at)
  if (local.length <= 2) return `${local[0] ?? ""}*@${email.slice(at + 1)}`
  return `${local.slice(0, 2)}***@${email.slice(at + 1)}`
}
