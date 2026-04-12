import type { Metadata } from "next"
import fs from "node:fs"
import path from "node:path"
import LegalHtmlDocument from "@/components/LegalHtmlDocument"

export const metadata: Metadata = {
  title: "이용약관 | finsight",
  description: "finsight 서비스 이용약관",
}

function escapeHtml(s: string) {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
}

function lineToParagraph(line: string): string {
  const parts = line.split(/\*\*/)
  let out = ""
  for (let j = 0; j < parts.length; j++) {
    const seg = escapeHtml(parts[j])
    out += j % 2 === 1 ? `<strong>${seg}</strong>` : seg
  }
  return `<p>${out}</p>`
}

function applyFinsightBranding(body: string): string {
  return body
    .replace(/제이티비씨\(주\)/g, "핀사이트 주식회사")
    .replace(/제이티비씨㈜/g, "핀사이트 주식회사")
    .replace(/\bJTBC\b/g, "finsight")
}

function markdownBodyToPolicyHtml(body: string): string {
  const normalized = applyFinsightBranding(body)
  const lines = normalized.split(/\r?\n/)
  const blocks: string[] = []
  for (const line of lines) {
    const t = line.trim()
    if (!t) continue
    blocks.push(lineToParagraph(t))
  }
  return blocks.join("")
}

function wrapPolicyDocument(innerHtml: string) {
  const header = `<div class="board__inner"><div class="board__box"><div class="fr-element fr-view"><p style="text-align: center;"><strong><u>finsight 이용약관</u></strong></p><p><br></p><p><br></p><p><br></p>`
  const footer = `</div></div></div>`
  return `${header}${innerHtml}${footer}`
}

function loadTermsHtml(): string {
  const pub = path.join(process.cwd(), "public")
  const htmlPath = path.join(pub, "terms-of-service.html")
  if (fs.existsSync(htmlPath)) {
    const raw = fs.readFileSync(htmlPath, "utf-8").trim()
    if (raw.length > 200) return raw
  }

  const mdPath = path.join(pub, "terms-of-service.md")
  const raw = fs.readFileSync(mdPath, "utf-8")
  const body = raw.includes("Markdown Content:")
    ? raw.split("Markdown Content:")[1].trim()
    : raw.trim()

  const inner = markdownBodyToPolicyHtml(body)
  return wrapPolicyDocument(inner)
}

export default function TermsPage() {
  const html = loadTermsHtml()

  return <LegalHtmlDocument html={html} />
}
