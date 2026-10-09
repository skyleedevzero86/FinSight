const TITLE_STOPWORDS = new Set([
  "더미",
  "안내",
  "공지",
  "공지사항",
  "관련",
  "기준",
  "내용",
  "게시물",
  "전망",
  "점검",
  "배경",
  "요약",
  "시즌",
  "마감",
  "브리핑",
  "결정",
  "업종",
  "이번",
  "오늘",
  "현재",
])

export type ViewedKeywordSource = {
  title: string
  viewCount: number
  hashtags?: string[]
}

export function keywordsFromViewedPosts(posts: ViewedKeywordSource[], limit = 10): string[] {
  const scores = new Map<string, number>()
  for (const post of posts) {
    if (post.viewCount <= 0) continue
    const weight = post.viewCount
    for (const tag of post.hashtags ?? []) addKeyword(scores, tag, weight * 2)
    for (const token of titleTokens(post.title)) addKeyword(scores, token, weight)
  }
  return [...scores.entries()]
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0], "ko"))
    .slice(0, Math.max(1, limit))
    .map(([word]) => word)
}

function addKeyword(scores: Map<string, number>, raw: string, weight: number) {
  const word = normalizeKeyword(raw)
  if (!word) return
  scores.set(word, (scores.get(word) ?? 0) + weight)
}

function titleTokens(title: string): string[] {
  return title
    .replace(/\[[^\]]*\]/g, " ")
    .split(/[^0-9A-Za-z가-힣]+/)
    .filter((token) => token.length > 0)
}

export function promotePlaceholderLabel(text: string): string {
  return text.replaceAll("테스트입니다", "강력추천").replaceAll("더미", "강력추천")
}

function normalizeKeyword(raw: string): string | null {
  const word = raw.trim()
  if (word.length < 2 || word.length > 20) return null
  if (TITLE_STOPWORDS.has(word)) return null
  if (/^\d+$/.test(word)) return null
  if (/^(.)\1+$/.test(word)) return null
  if (!/[가-힣]/.test(word) && !/^[A-Za-z][A-Za-z0-9]{1,}$/.test(word)) return null
  return word
}
