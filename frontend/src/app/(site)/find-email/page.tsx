import type { Metadata } from "next"
import FindEmailForm from "@/components/auth/FindEmailForm"

export const metadata: Metadata = {
  title: "아이디 찾기 | finsight",
  description: "finsight 아이디 찾기",
}

export default function FindEmailPage() {
  return <FindEmailForm />
}
