import type { Metadata } from "next"
import EmailVerifyCodeForm from "@/components/auth/EmailVerifyCodeForm"

export const metadata: Metadata = {
  title: "이메일 인증 | finsight",
  description: "finsight 이메일 검증 코드 입력",
}

export default async function EmailVerifyPage({
  params,
}: {
  params: Promise<{ token: string }>
}) {
  const { token } = await params
  return <EmailVerifyCodeForm token={decodeURIComponent(token)} />
}
