import type { Metadata } from "next"
import fs from "node:fs"
import path from "node:path"
import LegalPageBody from "@/components/LegalPageBody"

export const metadata: Metadata = {
  title: "청소년보호정책 | finsight",
  description: "finsight 청소년 보호 정책",
}

export default function YouthPolicyPage() {
  const htmlPath = path.join(process.cwd(), "public", "youth-policy.html")
  const html = fs.readFileSync(htmlPath, "utf-8")

  return (
    <div className="px-4 py-8 md:px-6 md:py-10">
      <LegalPageBody>
        <div
          className="youth-policy-doc max-w-none overflow-x-auto [&_.board__inner]:text-gray-800"
          dangerouslySetInnerHTML={{ __html: html }}
        />
      </LegalPageBody>
    </div>
  )
}
