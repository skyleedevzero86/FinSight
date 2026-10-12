export type EconomyPickBrief = {
  source: "OLLAMA" | "RULE" | string
  headline: string
  body: string
  points: string[]
}

type Json = Record<string, unknown>

function asRecord(value: unknown): Json | null {
  if (value && typeof value === "object" && !Array.isArray(value)) return value as Json
  return null
}

export function briefSourceLabel(source: string): string {
  return source === "OLLAMA" ? "Llama 추천" : "섹터 규칙 추천"
}

export async function fetchEconomyPickBrief(name: string, symbol: string): Promise<EconomyPickBrief | null> {
  try {
    const query = new URLSearchParams({ name, symbol })
    const res = await fetch(`/api/v1/economy-pick/brief?${query}`, { cache: "no-store" })
    if (!res.ok) return null
    const data = asRecord(asRecord(await res.json())?.data)
    if (!data || typeof data.headline !== "string") return null
    const points = Array.isArray(data.points)
      ? data.points.filter((item): item is string => typeof item === "string")
      : []
    return {
      source: typeof data.source === "string" ? data.source : "RULE",
      headline: data.headline,
      body: typeof data.body === "string" ? data.body : "",
      points,
    }
  } catch {
    return null
  }
}
