import type { Metadata } from "next"
import fs from "node:fs"
import path from "node:path"
import LegalPageBody from "@/components/LegalPageBody"

export const metadata: Metadata = {
  title: "개인정보처리방침 | finsight",
  description: "finsight 개인정보처리방침",
}

export default function PrivacyPage() {
  const htmlPath = path.join(process.cwd(), "public", "privacy-policy.html")
  const html = fs.readFileSync(htmlPath, "utf-8")

  return (
    <div className="px-4 py-8 md:px-6 md:py-10">
      <LegalPageBody>
        <div
          className="privacy-doc max-w-none overflow-x-auto [&_.board__inner]:text-gray-800 [&_table]:max-w-full [&_table]:text-[14px]"
          dangerouslySetInnerHTML={{ __html: html }}
        />
      </LegalPageBody>
    </div>
  )
}
