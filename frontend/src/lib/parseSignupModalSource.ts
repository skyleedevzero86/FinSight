export function parseSignupModalSource(raw: string): {
  title: string
  bodyHtml: string
} {
  const trimmed = raw.trim()
  const titleMatch = trimmed.match(/<div class="modal-header"><h2>([^<]*)<\/h2>/)
  const title =
    titleMatch?.[1]?.replace(/\s+/g, " ").trim() ?? "약관"

  const closeToken = '<button class="react-responsive-modal-closeButton"'
  const closeIdx = trimmed.indexOf(closeToken)
  const bodyStart = trimmed.indexOf('<div class="modal-body')
  if (bodyStart === -1 || closeIdx === -1 || closeIdx <= bodyStart) {
    return { title, bodyHtml: trimmed }
  }

  const contentStart = trimmed.indexOf(">", bodyStart) + 1
  const bodyHtml = trimmed.slice(contentStart, closeIdx).trim()
  return { title, bodyHtml }
}
