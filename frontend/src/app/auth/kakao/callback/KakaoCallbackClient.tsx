"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import {
  consumeOAuthCode,
  storeAuthSession,
  type AuthProvider,
} from "@/lib/finsightToken"

function extractToken(data: unknown): string | null {
  if (!data || typeof data !== "object") return null
  const o = data as Record<string, unknown>
  if (typeof o.accessToken === "string") return o.accessToken
  const token = o.token
  if (token && typeof token === "object") {
    const t = token as Record<string, unknown>
    if (typeof t.accessToken === "string") return t.accessToken
  }
  const inner = o.data
  if (inner && typeof inner === "object") {
    return extractToken(inner)
  }
  return null
}

function extractProvider(data: unknown): AuthProvider {
  if (!data || typeof data !== "object") return "KAKAO"
  const o = data as Record<string, unknown>
  if (typeof o.authProvider === "string") {
    const p = o.authProvider
    if (p === "WEB" || p === "KAKAO" || p === "NAVER" || p === "GOOGLE") return p
  }
  const inner = o.data
  if (inner && typeof inner === "object") {
    return extractProvider(inner)
  }
  return "KAKAO"
}

export default function KakaoCallbackClient() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [message, setMessage] = useState("카카오 로그인 처리 중...")

  useEffect(() => {
    const code = searchParams.get("code")
    const state = searchParams.get("state")
    const error = searchParams.get("error")

    if (error) {
      setMessage("카카오 로그인이 취소되었습니다.")
      return
    }
    if (!code) {
      setMessage("카카오 인가 코드가 없습니다.")
      return
    }
    if (!consumeOAuthCode(code)) {
      return
    }

    const savedState = sessionStorage.getItem("kakao_oauth_state")
    if (savedState && state && savedState !== state) {
      setMessage("카카오 로그인 상태 값이 일치하지 않습니다.")
      return
    }

    void (async () => {
      try {
        const res = await fetch("/api/v1/auth/oauth/kakao", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify({ code, state }),
        })
        const data = await res.json().catch(() => null)
        if (!res.ok) {
          setMessage(
            (data && typeof data === "object" && "message" in data
              ? String((data as { message?: string }).message)
              : null) || "카카오 로그인에 실패했습니다.",
          )
          return
        }

        const token = extractToken(data)
        const provider = extractProvider(data)
        if (!token) {
          setMessage("로그인 토큰을 받지 못했습니다.")
          return
        }

        storeAuthSession({ accessToken: token, authProvider: provider })
        sessionStorage.removeItem("kakao_oauth_state")
        router.replace("/")
        router.refresh()
      } catch {
        setMessage("카카오 로그인 처리 중 오류가 발생했습니다.")
      }
    })()
  }, [router, searchParams])

  return (
    <section className="flex min-h-[50vh] items-center justify-center px-4">
      <p className="text-sm text-gray-700">{message}</p>
    </section>
  )
}
