"use client"

import type { ReactNode } from "react"
import { Apple } from "lucide-react"
import { useState } from "react"

type SocialProvider = "KAKAO" | "NAVER" | "GOOGLE" | "APPLE"

type SocialButton = {
  label: string
  provider: SocialProvider
  bgClass: string
  content: ReactNode
}

const SOCIAL_LABEL: Record<"NAVER" | "KAKAO" | "GOOGLE", string> = {
  NAVER: "네이버",
  KAKAO: "카카오",
  GOOGLE: "구글",
}

const SOCIAL_STATE_KEY: Record<"NAVER" | "KAKAO" | "GOOGLE", string> = {
  NAVER: "naver_oauth_state",
  KAKAO: "kakao_oauth_state",
  GOOGLE: "google_oauth_state",
}

function GoogleMark() {
  return (
    <svg viewBox="0 0 24 24" className="h-7 w-7" aria-hidden>
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
      />
      <path
        fill="#EA4335"
        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      />
    </svg>
  )
}

const socialButtons: SocialButton[] = [
  {
    label: "카카오 로그인",
    provider: "KAKAO",
    bgClass: "bg-[#FEE500] text-[#3C1E1E]",
    content: <span className="text-base font-black">톡</span>,
  },
  {
    label: "네이버 로그인",
    provider: "NAVER",
    bgClass: "bg-[#03C75A] text-white",
    content: <span className="text-2xl font-black leading-none">N</span>,
  },
  {
    label: "구글 로그인",
    provider: "GOOGLE",
    bgClass: "border border-gray-200 bg-white",
    content: <GoogleMark />,
  },
  {
    label: "애플 로그인",
    provider: "APPLE",
    bgClass: "bg-black text-white",
    content: <Apple className="h-7 w-7 fill-current" strokeWidth={2.3} />,
  },
]

export default function SocialLoginRow() {
  const [loadingProvider, setLoadingProvider] = useState<string | null>(null)

  async function startSocialLogin(provider: "NAVER" | "KAKAO" | "GOOGLE") {
    setLoadingProvider(provider)
    const label = SOCIAL_LABEL[provider]
    const stateKey = SOCIAL_STATE_KEY[provider]
    try {
      const res = await fetch(`/api/v1/auth/oauth/${provider.toLowerCase()}/url`, {
        method: "GET",
        headers: { Accept: "application/json" },
      })
      const payload = await res.json().catch(() => null)
      if (!res.ok) {
        window.alert(
          (payload && typeof payload === "object" && "message" in payload
            ? String((payload as { message?: string }).message)
            : null) || `${label} 로그인 URL을 가져오지 못했습니다.`,
        )
        return
      }

      const data =
        payload && typeof payload === "object" && "data" in payload
          ? (payload as { data?: Record<string, string> }).data
          : null
      const authorizeUrl = data?.authorizeUrl
      const state = data?.state
      if (!authorizeUrl) {
        window.alert(`${label} 로그인 URL이 비어 있습니다. 설정을 확인해 주세요.`)
        return
      }
      if (state) {
        sessionStorage.setItem(stateKey, state)
      }
      window.location.href = authorizeUrl
    } catch {
      window.alert(`${label} 로그인을 시작하지 못했습니다.`)
    } finally {
      setLoadingProvider(null)
    }
  }

  async function handleClick(provider: SocialProvider) {
    if (provider === "NAVER" || provider === "KAKAO" || provider === "GOOGLE") {
      await startSocialLogin(provider)
      return
    }
    window.alert("서비스 준비중입니다.")
  }

  return (
    <div className="mt-10">
      <div className="relative flex items-center justify-center py-2">
        <div className="absolute left-0 right-0 top-1/2 h-px bg-gray-200" aria-hidden />
        <span className="relative bg-white px-4 text-sm text-gray-500">
          또는 간편로그인
        </span>
      </div>

      <div className="mt-5 flex items-center justify-center gap-5">
        {socialButtons.map((button) => (
          <button
            key={button.label}
            type="button"
            aria-label={button.label}
            title={button.label}
            disabled={loadingProvider === button.provider}
            onClick={() => void handleClick(button.provider)}
            className={`flex h-13 w-13 items-center justify-center rounded-full shadow-sm transition hover:scale-[1.03] disabled:opacity-60 ${button.bgClass}`}
          >
            {button.content}
          </button>
        ))}
      </div>
    </div>
  )
}
