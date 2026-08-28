import type { Metadata } from "next"
import EmailDisputeForm from "@/components/auth/EmailDisputeForm"

export const metadata: Metadata = {
  title: "요청하지 않은 인증 | finsight",
  description: "요청하지 않은 이메일 인증 신고",
}

export default async function EmailDisputePage({
  params,
}: {
  params: Promise<{ token: string }>
}) {
  const { token } = await params
  return <EmailDisputeForm token={decodeURIComponent(token)} />
}
