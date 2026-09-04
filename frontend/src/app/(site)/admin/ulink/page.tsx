import type { Metadata } from "next"
import AdminUlinkClient from "@/components/admin/AdminUlinkClient"

export const metadata: Metadata = {
  title: "통합링크 관리 | finsight",
  description: "finsight 유링크(통합링크) CMS",
}

export default function AdminUlinkPage() {
  return <AdminUlinkClient />
}
