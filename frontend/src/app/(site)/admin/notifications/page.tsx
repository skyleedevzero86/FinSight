import type { Metadata } from "next"
import AdminNotificationsClient from "@/components/admin/AdminNotificationsClient"

export const metadata: Metadata = {
  title: "알림 관리 | finsight",
  description: "finsight 공지 게시판 작성·알림 등록",
}

export default function AdminNotificationsPage() {
  return <AdminNotificationsClient />
}
