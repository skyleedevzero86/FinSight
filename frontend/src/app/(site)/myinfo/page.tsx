import type { Metadata } from "next"
import MyInfoClient from "@/components/MyInfoClient"

export const metadata: Metadata = {
  title: "내정보 | finsight",
  description: "finsight 내정보",
}

export default function MyInfoPage() {
  return <MyInfoClient />
}
