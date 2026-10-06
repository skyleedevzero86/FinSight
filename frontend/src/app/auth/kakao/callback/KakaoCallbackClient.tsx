"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { waitForSessionUser } from "@/lib/authSession"
import {
  runOAuthExchangeOnce,
  storeAuthSession,
  writeAuthHintCookie,
  type AuthProvider,
} from "@/lib/finsightToken"

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

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object" && "message" in data) {
    const message = String((data as { message?: string }).message ?? "").trim()
    if (message) return message
  }
  return fallback
}

function isBusinessFailure(data: unknown): boolean {
  return Boolean(data && typeof data === "object" && (data as { success?: boolean }).success === false)
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

    const savedState = sessionStorage.getItem("kakao_oauth_state")
    if (savedState && state && savedState !== state) {
      setMessage("카카오 로그인 상태 값이 일치하지 않습니다.")
      return
    }

    void (async () => {
      try {
        const res = await runOAuthExchangeOnce(`kakao:${code}`, () =>
          fetch("/api/v1/auth/oauth/kakao", {
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
        if (!res.ok || isBusinessFailure(data)) {
          setMessage(readErrorMessage(data, "카카오 로그인에 실패했습니다."))
          return
        }

        writeAuthHintCookie()
        storeAuthSession({ authProvider: extractProvider(data) })
        sessionStorage.removeItem("kakao_oauth_state")
        setMessage("세션 확인 중...")
        const ok = await waitForSessionUser()
        setMessage(
          ok
            ? "카카오 로그인에 성공했습니다. 이동 중..."
            : "로그인은 됐지만 세션 확인에 실패했습니다. 홈에서 새로고침해 주세요.",
        )
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
