import type { Metadata } from "next"
import AdminNotificationsClient from "@/components/admin/AdminNotificationsClient"

export const metadata: Metadata = {
  title: "알림 관리 | finsight",
  description: "finsight 인앱 알림 등록·수신 설정",
}

export default function AdminNotificationsPage() {
  return <AdminNotificationsClient />
}
