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
    return <div className="min-h-full w-full flex-1 bg-white" />
  }

  const home = pathname === "/myinfo" || pathname === "/myinfo/activity"

  return (
    <div className="flex min-h-full w-full flex-1 bg-white">
      <section
        className={
          home
            ? "flex min-h-full min-w-0 flex-1 flex-col bg-[#f4f7fb]"
            : "flex min-h-full min-w-0 flex-1 flex-col bg-white"
        }
        aria-label="나의 메뉴 본문"
      >
        {children}
      </section>
      <MyInfoSidebar />
    </div>
  )
}
