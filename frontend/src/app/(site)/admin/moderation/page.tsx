import type { Metadata } from "next"
import AdminModerationClient from "@/components/admin/AdminModerationClient"

export const metadata: Metadata = {
  title: "신고 관리 | finsight",
  description: "finsight 게시글 신고·모더레이션",
}

export default function AdminModerationPage() {
  return <AdminModerationClient />
}
