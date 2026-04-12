import type { Metadata } from "next"
import FindPasswordForm from "@/components/auth/FindPasswordForm"

export const metadata: Metadata = {
  title: "비밀번호 찾기 | finsight",
  description: "finsight 비밀번호 찾기",
}

export default function FindPasswordPage() {
  return <FindPasswordForm />
}
