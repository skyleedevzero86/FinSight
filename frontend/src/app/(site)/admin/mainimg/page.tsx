import type { Metadata } from "next"
import AdminMainimgClient from "@/components/admin/AdminMainimgClient"

export const metadata: Metadata = {
  title: "메인이미지 관리 | finsight",
  description: "finsight 메인 히어로 이미지 CMS",
}

export default function AdminMainimgPage() {
  return <AdminMainimgClient />
}
