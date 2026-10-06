import type { Metadata } from "next"
import MyActivityClient from "@/components/myinfo/MyActivityClient"

export const metadata: Metadata = {
  title: "내 활동 | finsight",
  description: "시청, 즐겨찾기, 게시글, 알림을 한눈에 확인합니다.",
}

export default function MyActivityPage() {
  return <MyActivityClient />
}
