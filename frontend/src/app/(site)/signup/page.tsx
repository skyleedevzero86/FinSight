import type { Metadata } from "next"
import fs from "node:fs"
import path from "node:path"
import SignupForm from "@/components/SignupForm"
import { parseSignupModalSource } from "@/lib/parseSignupModalSource"

export const metadata: Metadata = {
  title: "회원가입 | finsight",
  description: "finsight 회원가입",
}

export default function SignupPage() {
  const dir = path.join(process.cwd(), "content", "signup")
  const termsRaw = fs.readFileSync(path.join(dir, "terms.md"), "utf-8")
  const privacyRaw = fs.readFileSync(
    path.join(dir, "privacy-collection.md"),
    "utf-8",
  )
  const terms = parseSignupModalSource(termsRaw)
  const privacy = parseSignupModalSource(privacyRaw)

  return (
    <SignupForm
      termsModalTitle={terms.title}
      termsModalBodyHtml={terms.bodyHtml}
      privacyModalTitle={privacy.title}
      privacyModalBodyHtml={privacy.bodyHtml}
    />
  )
}
