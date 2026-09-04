import type { Metadata } from "next"
import AdminPopupClient from "@/components/admin/AdminPopupClient"

export const metadata: Metadata = {
  title: "팝업 관리 | finsight",
  description: "finsight 레이어 팝업 CMS",
}

export default function AdminPopupPage() {
  return <AdminPopupClient />
}
