import type { Metadata } from "next"
import AdminUsersClient from "@/components/admin/AdminUsersClient"

export const metadata: Metadata = {
  title: "사용자 관리 | finsight",
  description: "finsight 사용자 관리",
}

export default function AdminUsersPage() {
  return <AdminUsersClient />
}
