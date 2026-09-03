export type MarkdownCommandId =
  | "heading1"
  | "heading2"
  | "heading3"
  | "bold"
  | "italic"
  | "quote"
  | "link"
  | "image"
  | "code"

export type ToolbarAction = {
  id: MarkdownCommandId
  label: string
  title: string
}

export const toolbarActions: ToolbarAction[] = [
  { id: "heading1", label: "H1", title: "큰 제목" },
  { id: "heading2", label: "H2", title: "중간 제목" },
  { id: "heading3", label: "H3", title: "작은 제목" },
  { id: "bold", label: "B", title: "굵게" },
  { id: "italic", label: "I", title: "기울임" },
  { id: "quote", label: '"', title: "인용문" },
  { id: "link", label: "Link", title: "링크 삽입" },
  { id: "image", label: "Image", title: "이미지 마크다운" },
  { id: "code", label: "</>", title: "코드 블록" },
]

type SelectionRange = {
  start: number
  end: number
}

type CommandResult = {
  value: string
  selectionStart: number
  selectionEnd: number
}

export type HeadingDefinition = {
  id: string
  level: number
  text: string
  line: number
}

const blockPrefixes: Record<"heading1" | "heading2" | "heading3" | "quote", string> = {
  heading1: "# ",
  heading2: "## ",
  heading3: "### ",
  quote: "> ",
}

const commandRegistry: Record<
  MarkdownCommandId,
  (source: string, selection: SelectionRange) => CommandResult
> = {
  heading1: (source, selection) => prependToCurrentLine(source, selection, blockPrefixes.heading1),
  heading2: (source, selection) => prependToCurrentLine(source, selection, blockPrefixes.heading2),
  heading3: (source, selection) => prependToCurrentLine(source, selection, blockPrefixes.heading3),
  quote: (source, selection) => prependToCurrentLine(source, selection, blockPrefixes.quote),
  bold: (source, selection) => wrapSelection(source, selection, "**", "**", "강조할 텍스트"),
  italic: (source, selection) => wrapSelection(source, selection, "_", "_", "기울일 텍스트"),
  link: (source, selection) =>
    wrapSelection(source, selection, "[", "](https://example.com)", "링크 텍스트"),
  image: (source, selection) =>
    wrapSelection(source, selection, "![", "](https://via.placeholder.com/800x450)", "이미지 설명"),
  code: (source, selection) =>
    wrapSelection(source, selection, "```ts\n", "\n```", "const status = 'ACTIVE';"),
}

export function applyMarkdownCommand(
  command: MarkdownCommandId,
  source: string,
  selection: SelectionRange
): CommandResult {
  return commandRegistry[command](source, selection)
}

export function normalizeTags(tags: string[]): string[] {
  return Array.from(
    new Set(
      tags
        .map((tag) => tag.trim().replace(/^#+/, "").replace(/,/g, ""))
        .filter(Boolean)
    )
  )
}

export function splitTagInput(input: string): string[] {
  return normalizeTags(input.split(/[,\n]/g))
}

export function createHeadingDefinitions(markdown: string): HeadingDefinition[] {
  const headingIds = new Map<string, number>()
  const headings: HeadingDefinition[] = []
  const lines = markdown.replace(/\r\n/g, "\n").split("\n")
  let inCodeBlock = false

  for (const [index, line] of lines.entries()) {
    const trimmedLine = line.trim()

    if (trimmedLine.startsWith("```")) {
      inCodeBlock = !inCodeBlock
      continue
    }

    if (inCodeBlock) continue

    const match = /^(#{1,6})\s+(.*)$/.exec(trimmedLine)
    if (!match) continue

    const level = match[1].length
    const text = cleanupHeadingText(match[2])
    if (!text) continue

    const baseId = createHeadingSlug(text)
    const duplicatedCount = headingIds.get(baseId) ?? 0
    headingIds.set(baseId, duplicatedCount + 1)

    headings.push({
      id: duplicatedCount === 0 ? baseId : `${baseId}-${duplicatedCount + 1}`,
      level,
      text,
      line: index + 1,
    })
  }

  return headings
}

export function escapeCssIdentifier(value: string): string {
  if (typeof CSS !== "undefined" && typeof CSS.escape === "function") {
    return CSS.escape(value)
  }
  return value.replace(/[^a-zA-Z0-9_-]/g, "\\$&")
}

function prependToCurrentLine(
  source: string,
  selection: SelectionRange,
  prefix: string
): CommandResult {
  const safeSelection = ensureSelection(selection)
  const lineStart = source.lastIndexOf("\n", safeSelection.start - 1) + 1
  const value = `${source.slice(0, lineStart)}${prefix}${source.slice(lineStart)}`
  const offset = prefix.length

  return {
    value,
    selectionStart: safeSelection.start + offset,
    selectionEnd: safeSelection.end + offset,
  }
}

function wrapSelection(
  source: string,
  selection: SelectionRange,
  prefix: string,
  suffix: string,
  fallback: string
): CommandResult {
  const safeSelection = ensureSelection(selection)
  const selectedText = source.slice(safeSelection.start, safeSelection.end) || fallback
  const replacement = `${prefix}${selectedText}${suffix}`
  const value = `${source.slice(0, safeSelection.start)}${replacement}${source.slice(safeSelection.end)}`
  const fallbackStart = safeSelection.start + prefix.length

  return {
    value,
    selectionStart: fallbackStart,
    selectionEnd: fallbackStart + selectedText.length,
  }
}

function ensureSelection(selection: SelectionRange): SelectionRange {
  return {
    start: Math.max(0, selection.start),
    end: Math.max(selection.start, selection.end),
  }
}

function cleanupHeadingText(value: string): string {
  return value
    .replace(/\s+#+$/g, "")
    .replace(/!\[([^\]]*)]\([^)]+\)/g, "$1")
    .replace(/\[([^\]]+)]\([^)]+\)/g, "$1")
    .replace(/[`*_~]/g, "")
    .replace(/<[^>]+>/g, "")
    .trim()
}

function createHeadingSlug(value: string): string {
  const slug = value
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[^\p{Letter}\p{Number}\s-]/gu, "")
    .trim()
    .replace(/\s+/g, "-")

  return slug || "section"
}
