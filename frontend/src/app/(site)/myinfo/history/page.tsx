import type { Metadata } from "next"
import MyHistoryClient from "@/components/MyHistoryClient"
import "@/styles/finsight-live-vod.css"

export const metadata: Metadata = {
  title: "시청 기록 | finsight",
  description: "최근에 본 게시물 히스토리",
}

export default function MyHistoryPage() {
  return <MyHistoryClient />
}
