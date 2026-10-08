import type { Metadata } from "next"
import { redirect } from "next/navigation"

export const metadata: Metadata = {
  title: "공지 수정 | finsight",
  description: "공지사항은 관리자 메뉴에서 수정합니다.",
}

export default function CommunityNoticeEditPage() {
  redirect("/admin/notifications")
}
