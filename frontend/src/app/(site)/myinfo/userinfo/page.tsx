import type { Metadata } from "next"
import { Suspense } from "react"
import MyInfoClient from "@/components/MyInfoClient"

export const metadata: Metadata = {
  title: "나의 정보 | finsight",
  description: "finsight 사용자 정보 변경",
}

export default function MyInfoUserInfoPage() {
  return (
    <Suspense fallback={null}>
      <MyInfoClient />
    </Suspense>
  )
}
