"use client"

import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Eye, EyeOff } from "lucide-react"
import { useId, useState, useEffect } from "react"
import AuthCard from "@/components/auth/AuthCard"
import SocialLoginRow from "@/components/auth/SocialLoginRow"
import { postLogin } from "@/lib/authClient"
import { useAuthSession } from "@/components/AuthSessionProvider"
import {
  storeAuthSession,
  clearAuthSession,
  FINSIGHT_FORCE_PASSWORD_KEY,
  type AuthProvider,
} from "@/lib/finsightToken"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2.5 text-[15px] text-gray-900 placeholder:text-gray-400 outline-none transition focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

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

function extractPasswordChangeRequired(data: unknown): boolean {
  if (!data || typeof data !== "object") return false
  const o = data as Record<string, unknown>
  if (o.passwordChangeRequired === true || o.changeRequired === true) return true
  const inner = o.data
  if (inner && typeof inner === "object") {
    return extractPasswordChangeRequired(inner)
  }
  return false
}

function extractProvider(data: unknown): AuthProvider {
  if (!data || typeof data !== "object") return "WEB"
  const o = data as Record<string, unknown>
  if (typeof o.authProvider === "string") {
    const p = o.authProvider
    if (p === "WEB" || p === "KAKAO" || p === "NAVER" || p === "GOOGLE") return p
  }
  const inner = o.data
  if (inner && typeof inner === "object") {
    return extractProvider(inner)
  }
  return "WEB"
}

export default function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const id = useId()
  const registered = searchParams.get("registered") === "1"
  const resetDone = searchParams.get("reset") === "1"
  const accountParam = searchParams.get("account") ?? ""
  const nextPathRaw = searchParams.get("next")?.trim() || ""
  const nextPath =
    nextPathRaw.startsWith("/") && !nextPathRaw.startsWith("//") ? nextPathRaw : ""
  const { user, ready, logout } = useAuthSession()
  const [switchAccount, setSwitchAccount] = useState(false)

  const [email, setEmail] = useState(accountParam)
  const [password, setPassword] = useState("")
  const [showPw, setShowPw] = useState(false)
  const [remember, setRemember] = useState(false)
  const [loading, setLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const alreadySignedIn = ready && Boolean(user) && !switchAccount

  useEffect(() => {
    if (accountParam) setEmail(accountParam)
  }, [accountParam])

  async function onUseOtherAccount() {
    setFormError(null)
    setSwitchAccount(true)
    try {
      await logout()
    } catch {
      clearAuthSession()
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const em = email.trim()
    if (!em || !password) {
      setFormError("이메일 또는 아이디와 비밀번호를 입력해 주세요.")
      return
    }
    setLoading(true)
    try {
      const result = await postLogin({
        email: em,
        password,
      })

      if (!result.ok) {
        setFormError(result.message)
        return
      }

      const token = extractToken(result.data)
      const provider = extractProvider(result.data)
      const passwordRequired = extractPasswordChangeRequired(result.data)
      if (!token) {
        setFormError(
          "로그인 응답이 올바르지 않습니다. 잠시 후 다시 시도해 주세요.",
        )
        return
      }
      try {
        storeAuthSession({
          accessToken: token,
          authProvider: provider,
          remember,
        })
        if (passwordRequired && provider === "WEB") {
          sessionStorage.setItem(FINSIGHT_FORCE_PASSWORD_KEY, "1")
        }
      } catch {
        void 0
      }
      router.push(
        passwordRequired && provider === "WEB"
          ? "/myinfo?password=required"
          : nextPath || "/",
      )
      router.refresh()
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="w-full px-4 py-16 md:px-6 md:py-20 lg:py-24">
      <div className="mx-auto flex w-full max-w-6xl justify-center">
        <AuthCard
      title="로그인"
      topBanner={
        registered ? (
          <div
            role="status"
            className="mb-6 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900"
          >
            회원가입이 완료되었습니다. 가입 축하 메일을 발송했습니다. 로그인해 주세요.
          </div>
        ) : resetDone ? (
          <div
            role="status"
            className="mb-6 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900"
          >
            비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.
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

      {alreadySignedIn ? (
        <div className="space-y-4">
          <div
            role="status"
            className="rounded-md border border-gray-200 bg-gray-50 px-3 py-3 text-sm text-gray-800"
          >
            <p className="font-medium">이미 로그인되어 있습니다.</p>
            <p className="mt-1 text-gray-600">
              {user?.email
                ? `${user.email} 계정으로 접속 중입니다.`
                : "저장된 세션으로 접속 중입니다."}
            </p>
            <p className="mt-2 text-xs text-gray-500">
              「로그인 유지」 또는 같은 탭의 이전 세션이 남아 있는 경우입니다.
            </p>
          </div>
          <button
            type="button"
            onClick={() => router.push(nextPath || "/")}
            className="w-full rounded py-3.5 text-[15px] font-semibold text-white transition hover:brightness-105 active:brightness-95"
            style={{ backgroundColor: "#B24DFF" }}
          >
            홈으로 이동
          </button>
          <button
            type="button"
            onClick={() => void onUseOtherAccount()}
            className="w-full rounded border border-gray-300 bg-white py-3 text-[15px] font-medium text-gray-800 transition hover:bg-gray-50"
          >
            다른 계정으로 로그인
          </button>
        </div>
      ) : (
        <>
      <form className="space-y-3" onSubmit={onSubmit} noValidate>
        <input
          id={`${id}-email`}
          type="text"
          name="username"
          autoComplete="username"
          placeholder="이메일 또는 아이디"
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
              아이디 찾기
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
        </>
      )}
        </AuthCard>
      </div>
    </section>
  )
}
