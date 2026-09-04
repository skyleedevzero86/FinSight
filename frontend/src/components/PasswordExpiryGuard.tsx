"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { fetchPasswordStatus } from "@/lib/myAccount"
import { FINSIGHT_FORCE_PASSWORD_KEY } from "@/lib/finsightToken"

export default function PasswordExpiryGuard() {
  const { user, ready } = useAuthSession()
  const pathname = usePathname()
  const router = useRouter()

  useEffect(() => {
    if (!ready || !user) return
    if (user.authProvider !== "WEB") return
    if (pathname === "/login" || pathname === "/signup") return

    let cancelled = false
    void (async () => {
      try {
        const forced =
          typeof window !== "undefined" &&
          sessionStorage.getItem(FINSIGHT_FORCE_PASSWORD_KEY) === "1"
        const status = await fetchPasswordStatus()
        if (cancelled) return
        const required = forced || Boolean(status?.changeRequired)
        if (!required) return
        try {
          sessionStorage.setItem(FINSIGHT_FORCE_PASSWORD_KEY, "1")
        } catch {
          void 0
        }
        if (!pathname.startsWith("/myinfo")) {
          router.replace("/myinfo?password=required")
        }
      } catch {
        void 0
      }
    })()

    return () => {
      cancelled = true
    }
  }, [ready, user, pathname, router])

  return null
}
