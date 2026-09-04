import type { Metadata } from "next"
import AdminStatsClient from "@/components/admin/AdminStatsClient"

export const metadata: Metadata = {
  title: "관리자 통계 | finsight",
  description: "finsight 관리자 통계",
}

export default function AdminStatsPage() {
  return <AdminStatsClient />
}
