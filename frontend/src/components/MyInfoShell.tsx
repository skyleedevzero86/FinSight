"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import MyInfoSidebar from "@/components/MyInfoSidebar"

export default function MyInfoShell({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const { user, ready } = useAuthSession()

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace(`/login?next=${encodeURIComponent(pathname || "/myinfo")}`)
    }
  }, [ready, user, router, pathname])

  if (!ready || !user) {
    return <div className="min-h-[50vh] w-full flex-1 bg-white" />
  }

  return (
    <div className="flex min-h-[calc(100dvh-10.5rem)] w-full flex-1 bg-white">
      <section className="min-h-full min-w-0 flex-1 bg-white" aria-label="나의 메뉴 본문">
        {children}
      </section>
      <MyInfoSidebar />
    </div>
  )
}
