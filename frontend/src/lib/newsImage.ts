const FALLBACK_NEWS_IMAGE = "/finsight-logo.png"

export function newsImageSrc(imageUrl: string | null): string {
  const value = imageUrl?.trim() ?? ""
  if (!value) return FALLBACK_NEWS_IMAGE
  if (value.startsWith("/")) return value
  return `/api/news-image?url=${encodeURIComponent(value)}`
}

export function publicNewsImageUrl(raw: string): URL | null {
  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    return null
  }
  if (parsed.protocol !== "https:" && parsed.protocol !== "http:") return null
  if (parsed.username || parsed.password) return null
  if (isBlockedHost(parsed.hostname)) return null
  return parsed
}

function isBlockedHost(host: string): boolean {
  const name = host.toLowerCase().replace(/^\[|\]$/g, "")
  if (name === "localhost" || name.endsWith(".localhost") || name.endsWith(".local")) return true
  if (name === "::1" || name === "0.0.0.0" || name.includes(":")) return true
  const parts = name.split(".")
  if (parts.length !== 4 || parts.some((part) => !/^\d{1,3}$/.test(part))) return false
  const numbers = parts.map(Number)
  if (numbers.some((n) => n > 255)) return true
  const [a, b] = numbers
  if (a === 10 || a === 127 || a === 0) return true
  if (a === 169 && b === 254) return true
  if (a === 192 && b === 168) return true
  return a === 172 && b >= 16 && b <= 31
}
