import { sentimentExpression, sentimentLabel } from "@/lib/publicNews"

type Tone = "positive" | "negative" | "neutral"

const FACE: Record<Tone, string> = {
  positive: "bg-[#2b8a3e]",
  negative: "bg-[#e03131]",
  neutral: "bg-[#3b5bdb]",
}

const WORD: Record<Tone, string> = {
  positive: "text-[#2b8a3e]",
  negative: "text-[#e03131]",
  neutral: "text-[#3b5bdb]",
}

function toneOf(sentiment: string | null): Tone | null {
  if (sentiment === "POSITIVE") return "positive"
  if (sentiment === "NEGATIVE") return "negative"
  if (sentiment === "NEUTRAL") return "neutral"
  return null
}

function MoodFace({ tone }: { tone: Tone }) {
  const mouth = tone === "positive" ? "M6.5 13.2c1.4 1.8 3 2.6 5.5 2.6s4.1-.8 5.5-2.6" : tone === "negative" ? "M6.5 16.2c1.4-1.8 3-2.6 5.5-2.6s4.1.8 5.5 2.6" : "M7 14.5h10"
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-full w-full">
      <circle cx="9" cy="10" r="1.25" fill="#fff" />
      <circle cx="15" cy="10" r="1.25" fill="#fff" />
      <path d={mouth} fill="none" stroke="#fff" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  )
}

export default function SentimentBadge({ sentiment, compact = false }: { sentiment: string | null; compact?: boolean }) {
  const tone = toneOf(sentiment)
  const label = sentimentLabel(sentiment)
  if (!label) return null
  if (!tone) {
    return (
      <span
        className={`pointer-events-none absolute bottom-1.5 right-1.5 rounded bg-white/95 px-1 font-bold leading-none text-gray-500 ${compact ? "text-[9px]" : "text-[11px]"}`}
        aria-label={label}
      >
        {label}
      </span>
    )
  }
  const face = compact ? "h-[22px] w-[22px]" : "h-7 w-7"
  const word = compact ? "text-[9px]" : "text-[11px]"
  return (
    <span
      className="pointer-events-none absolute bottom-1.5 right-1.5 flex flex-col items-center"
      aria-label={sentimentExpression(sentiment) ?? label}
    >
      <span className={`inline-flex items-center justify-center rounded-full shadow-sm ${face} ${FACE[tone]}`}>
        <MoodFace tone={tone} />
      </span>
      <span className={`mt-0.5 rounded bg-white/95 px-1 font-bold leading-none ${word} ${WORD[tone]}`}>{label}</span>
    </span>
  )
}
