import type { Metadata } from "next"
import AdminSmsClient from "@/components/admin/AdminSmsClient"

export const metadata: Metadata = {
  title: "SMS 알림 | finsight",
  description: "finsight Solapi SMS 발송·설정·통계",
}

export default function AdminSmsPage() {
  return <AdminSmsClient />
}
