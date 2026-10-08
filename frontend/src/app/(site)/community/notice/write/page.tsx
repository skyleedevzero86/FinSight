import type { Metadata } from "next"
import { redirect } from "next/navigation"

export const metadata: Metadata = {
  title: "공지 작성 | finsight",
  description: "공지사항은 관리자 메뉴에서 작성합니다.",
}

export default function CommunityNoticeWritePage() {
  redirect("/admin/notifications")
}
