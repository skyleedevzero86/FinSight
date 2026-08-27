import type { Metadata } from "next"
import MyHistoryClient from "@/components/MyHistoryClient"
import "@/styles/finsight-live-vod.css"

export const metadata: Metadata = {
  title: "나의 LIVE/VOD 히스토리 | finsight",
  description: "finsight 최근 시청·즐겨찾기 히스토리",
}

export default function MyHistoryPage() {
  return <MyHistoryClient />
}
