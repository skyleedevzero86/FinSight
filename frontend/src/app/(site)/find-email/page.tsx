import type { Metadata } from "next"
import FindEmailForm from "@/components/auth/FindEmailForm"

export const metadata: Metadata = {
  title: "이메일 찾기 | finsight",
  description: "finsight 이메일 찾기",
}

export default function FindEmailPage() {
  return <FindEmailForm />
}
