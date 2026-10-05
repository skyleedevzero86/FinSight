"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import {
  hasAuthSession,
  runOAuthExchangeOnce,
  storeAuthSession,
  writeAuthHintCookie,
  type AuthProvider,
} from "@/lib/finsightToken"

function extractProvider(data: unknown): AuthProvider {
  if (!data || typeof data !== "object") return "GOOGLE"
  const o = data as Record<string, unknown>
  if (typeof o.authProvider === "string") {
    const p = o.authProvider
    if (p === "WEB" || p === "KAKAO" || p === "NAVER" || p === "GOOGLE") return p
  }
  const inner = o.data
  if (inner && typeof inner === "object") {
    return extractProvider(inner)
  }
  return "GOOGLE"
}

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object" && "message" in data) {
    const message = String((data as { message?: string }).message ?? "").trim()
    if (message) return message
  }
  return fallback
}

export default function GoogleCallbackClient() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [message, setMessage] = useState("구글 로그인 처리 중...")

  useEffect(() => {
    const code = searchParams.get("code")
    const state = searchParams.get("state")
    const error = searchParams.get("error")

    if (error) {
      setMessage("구글 로그인이 취소되었습니다.")
      return
    }
    if (!code) {
      setMessage("구글 인가 코드가 없습니다.")
      return
    }

    const savedState = sessionStorage.getItem("google_oauth_state")
    if (savedState && state && savedState !== state) {
      setMessage("구글 로그인 상태 값이 일치하지 않습니다.")
      return
    }

    void (async () => {
      try {
        const res = await runOAuthExchangeOnce(`google:${code}`, () =>
          fetch("/api/v1/auth/oauth/google", {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
            },
            body: JSON.stringify({ code, state }),
            cache: "no-store",
          }),
        )
        const data = await res.json().catch(() => null)
        if (!res.ok) {
          setMessage(readErrorMessage(data, "구글 로그인에 실패했습니다."))
          return
        }

        writeAuthHintCookie()
        storeAuthSession({ authProvider: extractProvider(data) })
        sessionStorage.removeItem("google_oauth_state")

        if (!hasAuthSession()) {
          writeAuthHintCookie()
        }

        setMessage("구글 로그인에 성공했습니다. 이동 중...")
        router.replace("/")
        router.refresh()
      } catch {
        setMessage("구글 로그인 처리 중 오류가 발생했습니다.")
      }
    })()
  }, [router, searchParams])

  return (
    <section className="flex min-h-[50vh] items-center justify-center px-4">
      <p className="text-sm text-gray-700">{message}</p>
    </section>
  )
}
