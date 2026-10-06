import type { Metadata } from "next"
import AdminMediaClient from "@/components/admin/AdminMediaClient"

export const metadata: Metadata = {
  title: "영상 관리 | finsight",
  description: "YouTube 영상 초안, AI 보강, 게시와 수집 소스 관리",
}

export default function AdminMediaPage() {
  return <AdminMediaClient />
}
