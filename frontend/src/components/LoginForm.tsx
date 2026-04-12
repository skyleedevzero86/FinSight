"use client"

import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Eye, EyeOff } from "lucide-react"
import { useId, useState } from "react"
import AuthCard from "@/components/auth/AuthCard"
import SocialLoginRow from "@/components/auth/SocialLoginRow"
import { postLogin } from "@/lib/authClient"
import { FINSIGHT_ACCESS_TOKEN_KEY } from "@/lib/finsightToken"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2.5 text-[15px] text-gray-900 placeholder:text-gray-400 outline-none transition focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

function extractToken(data: unknown): string | null {
  if (!data || typeof data !== "object") return null
  const o = data as Record<string, unknown>
  if (typeof o.accessToken === "string") return o.accessToken
  if (typeof o.token === "string") return o.token
  const inner = o.data
  if (inner && typeof inner === "object") {
    const d = inner as Record<string, unknown>
    if (typeof d.accessToken === "string") return d.accessToken
    if (typeof d.token === "string") return d.token
  }
  return null
}

export default function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const id = useId()
  const registered = searchParams.get("registered") === "1"

  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPw, setShowPw] = useState(false)
  const [remember, setRemember] = useState(false)
  const [loading, setLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const em = email.trim()
    if (!em || !password) {
      setFormError("이메일과 비밀번호를 입력해 주세요.")
      return
    }
    setLoading(true)
    const result = await postLogin({
      username: em,
      password,
    })
    setLoading(false)

    if (!result.ok) {
      setFormError(result.message)
      return
    }

    const token = extractToken(result.data)
    try {
      if (token) {
        if (remember) {
          localStorage.setItem(FINSIGHT_ACCESS_TOKEN_KEY, token)
          sessionStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
        } else {
          sessionStorage.setItem(FINSIGHT_ACCESS_TOKEN_KEY, token)
          localStorage.removeItem(FINSIGHT_ACCESS_TOKEN_KEY)
        }
      }
    } catch {
      void 0
    }
    router.push("/")
    router.refresh()
  }

  return (
    <AuthCard
      title="로그인"
      topBanner={
        registered ? (
          <div
            role="status"
            className="mb-6 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900"
          >
            회원가입이 완료되었습니다. 로그인해 주세요.
          </div>
        ) : undefined
      }
    >
      {formError && (
        <div
          role="alert"
          className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {formError}
        </div>
      )}

      <form className="space-y-3" onSubmit={onSubmit} noValidate>
        <input
          id={`${id}-email`}
          type="email"
          name="email"
          autoComplete="email"
          placeholder="이메일을 입력하세요."
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={inputClass}
        />
        <div className="relative">
          <input
            id={`${id}-pw`}
            type={showPw ? "text" : "password"}
            name="password"
            autoComplete="current-password"
            placeholder="비밀번호를 입력하세요."
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={`${inputClass} pr-11`}
          />
          <button
            type="button"
            onClick={() => setShowPw((v) => !v)}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            aria-label={showPw ? "비밀번호 숨기기" : "비밀번호 보기"}
          >
            {showPw ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
          </button>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 pt-1 text-sm">
          <label className="flex cursor-pointer items-center gap-2 text-gray-700">
            <input
              type="checkbox"
              checked={remember}
              onChange={(e) => setRemember(e.target.checked)}
              className="signup-checkbox !mt-0"
            />
            <span>로그인 유지</span>
          </label>
          <div className="flex items-center gap-1.5 text-xs text-gray-500">
            <Link
              href="/find-email"
              className="hover:text-gray-800 hover:underline underline-offset-2"
            >
              이메일 찾기
            </Link>
            <span className="text-gray-300">|</span>
            <Link
              href="/find-password"
              className="hover:text-gray-800 hover:underline underline-offset-2"
            >
              비밀번호 찾기
            </Link>
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="mt-4 w-full rounded py-3.5 text-[15px] font-semibold text-white transition enabled:hover:brightness-105 enabled:active:brightness-95 disabled:opacity-60"
          style={{ backgroundColor: "#B24DFF" }}
        >
          {loading ? "처리 중…" : "이메일로 로그인"}
        </button>
      </form>

      <SocialLoginRow />

      <p className="mt-8 text-center text-sm text-gray-600">
        finsight에 처음이신가요?{" "}
        <Link
          href="/signup"
          className="font-semibold text-gray-900 underline underline-offset-2 hover:text-finsight-primary"
        >
          회원가입
        </Link>
      </p>

      <p className="mt-4 text-center text-[11px] leading-relaxed text-gray-400">
        • 로그인 유지 설정 시, 개인정보 유출 위험에 유의해 주세요.
      </p>
    </AuthCard>
  )
}
