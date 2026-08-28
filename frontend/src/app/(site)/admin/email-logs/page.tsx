import type { Metadata } from "next"
import { Suspense } from "react"
import AdminEmailLogsClient from "@/components/admin/AdminEmailLogsClient"

export const metadata: Metadata = {
  title: "메일 발송 이력 | finsight",
  description: "관리자 메일 발송 이력",
}

export default function AdminEmailLogsPage() {
  return (
    <Suspense
      fallback={
        <div className="mx-auto max-w-6xl px-4 py-16 text-sm text-gray-600">
          불러오는 중…
        </div>
      }
    >
      <AdminEmailLogsClient />
    </Suspense>
  )
}
