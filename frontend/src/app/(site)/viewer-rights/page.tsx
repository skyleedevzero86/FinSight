import type { Metadata } from "next"
import fs from "node:fs"
import path from "node:path"
import LegalHtmlDocument from "@/components/LegalHtmlDocument"

export const metadata: Metadata = {
  title: "시청자권익보호 | finsight",
  description: "시청자권익보호에 관한 방송사업자의 의무 및 정보공개 절차 안내",
}

export default function ViewerRightsPage() {
  const htmlPath = path.join(process.cwd(), "public", "viewer-rights.html")
  const html = fs.readFileSync(htmlPath, "utf-8")
  return <LegalHtmlDocument html={html} />
}
