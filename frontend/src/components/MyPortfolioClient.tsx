"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"

export default function MyPortfolioClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent("/myinfo/portfolio")}`)
    }
  }, [ready, user, router])

  if (!ready || !user) {
    return (
      <div className="mx-auto max-w-[960px] px-4 py-10 md:px-6">
        <p className="text-sm text-gray-500">로그인 확인 중…</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-[960px] px-4 py-8 md:px-6">
      <header className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900">나의 포트폴리오</h1>
        <p className="mt-2 text-sm text-slate-600">
          보유·관심 종목과 자산 구성을 한곳에서 관리하는 공간입니다.
        </p>
      </header>

      <div className="rounded-xl border border-slate-200 bg-slate-50 px-5 py-12 text-center">
        <p className="text-base font-semibold text-slate-800">준비 중인 메뉴입니다</p>
        <p className="mt-2 text-sm text-slate-600">
          곧 포트폴리오 등록·조회·관심 종목 연동 기능을 제공할 예정입니다.
        </p>
      </div>
    </div>
  )
}
