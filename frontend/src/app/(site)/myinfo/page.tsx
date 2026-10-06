import type { Metadata } from "next"
import MyInfoHomeGate from "@/components/myinfo/MyInfoHomeGate"

export const metadata: Metadata = {
  title: "나의 메뉴 | finsight",
  description: "finsight 나의 메뉴",
}

export default function MyInfoPage() {
  return <MyInfoHomeGate />
}
