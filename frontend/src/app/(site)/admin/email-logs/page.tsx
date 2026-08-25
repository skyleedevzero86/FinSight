import type { Metadata } from "next"
import AdminEmailLogsClient from "@/components/admin/AdminEmailLogsClient"

export const metadata: Metadata = {
  title: "메일 발송 이력 | finsight",
  description: "관리자 메일 발송 이력",
}

export default function AdminEmailLogsPage() {
  return <AdminEmailLogsClient />
}
