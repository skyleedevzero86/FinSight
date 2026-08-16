import type { Metadata } from "next"
import EmailVerifyCodeForm from "@/components/auth/EmailVerifyCodeForm"

export const metadata: Metadata = {
  title: "비밀번호 찾기 인증 | finsight",
}

export default async function FindPasswordVerifyPage({
  params,
}: {
  params: Promise<{ token: string }>
}) {
  const { token } = await params
  return (
    <EmailVerifyCodeForm
      token={decodeURIComponent(token)}
      expectedPurpose="FIND_PASSWORD"
    />
  )
}
