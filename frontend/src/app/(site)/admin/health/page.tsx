import type { Metadata } from "next"
import AdminHealthClient from "@/components/admin/AdminHealthClient"

export const metadata: Metadata = {
  title: "서버상황 | finsight",
  description: "finsight 시스템 헬스·메트릭·외부 서비스 상태",
}

export default function AdminHealthPage() {
  return <AdminHealthClient />
}
