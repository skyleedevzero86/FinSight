"use client"

import type { FormEvent } from "react"
import { useEffect, useMemo, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import AuthCard from "@/components/auth/AuthCard"
import {
  confirmEmailVerification,
  fetchEmailChallenge,
  loadPendingVerification,
  markEmailVerified,
  resetPasswordAfterVerification,
  type EmailVerificationChallenge,
  type EmailVerificationPurpose,
} from "@/lib/emailVerification"
import { validatePassword } from "@/lib/registration"

const CODE_LEN = 6
const inputClass =
  "w-full rounded border border-gray-300 px-3 py-2.5 text-[15px] outline-none focus:border-finsight-secondary"

function purposeTitle(purpose?: EmailVerificationPurpose) {
  if (purpose === "FIND_EMAIL") return "아이디 찾기"
  if (purpose === "FIND_PASSWORD") return "비밀번호 찾기"
  return "이메일 인증"
}

export default function EmailVerifyCodeForm({
  token,
  expectedPurpose,
}: {
  token: string
  expectedPurpose?: EmailVerificationPurpose
}) {
  const router = useRouter()
  const [challenge, setChallenge] = useState<EmailVerificationChallenge | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [digits, setDigits] = useState<string[]>(Array(CODE_LEN).fill(""))
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [foundUsername, setFoundUsername] = useState<string | null>(null)
  const [foundEmail, setFoundEmail] = useState<string | null>(null)
  const [canResetPassword, setCanResetPassword] = useState(false)
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [passwordConfirm, setPasswordConfirm] = useState("")
  const inputs = useRef<Array<HTMLInputElement | null>>([])

  useEffect(() => {
    let cancelled = false
    fetchEmailChallenge(token).then((result) => {
      if (cancelled) return
      if (!result.ok) {
        setLoadError(result.message)
        return
      }
      if (expectedPurpose && result.data.purpose !== expectedPurpose) {
        setLoadError("이 인증 링크는 현재 페이지와 맞지 않습니다. 처음부터 다시 진행해 주세요.")
        return
      }
      setChallenge(result.data)
    })
    return () => {
      cancelled = true
    }
  }, [token, expectedPurpose])

  const code = useMemo(() => digits.join(""), [digits])

  function setDigit(index: number, value: string) {
    const next = value.replace(/\D/g, "").slice(-1)
    setDigits((prev) => {
      const copy = [...prev]
      copy[index] = next
      return copy
    })
    if (next && index < CODE_LEN - 1) {
      inputs.current[index + 1]?.focus()
    }
  }

  function onKeyDown(index: number, key: string) {
    if (key === "Backspace" && !digits[index] && index > 0) {
      inputs.current[index - 1]?.focus()
    }
  }

  function onPaste(text: string) {
    const nums = text.replace(/\D/g, "").slice(0, CODE_LEN).split("")
    if (!nums.length) return
    setDigits(Array.from({ length: CODE_LEN }, (_, i) => nums[i] ?? ""))
    const focusAt = Math.min(nums.length, CODE_LEN - 1)
    inputs.current[focusAt]?.focus()
  }

  async function onSubmitCode(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (code.length !== CODE_LEN) {
      setFormError("인증 코드 6자리를 입력해 주세요.")
      return
    }
    setSubmitting(true)
    const result = await confirmEmailVerification(token, code)
    setSubmitting(false)
    if (!result.ok) {
      setFormError(result.message)
      return
    }
    markEmailVerified(loadPendingVerification()?.email ?? "", result.data.purpose)
    if (result.data.purpose === "SIGNUP") {
      router.replace("/signup?verified=1")
      return
    }
    if (result.data.purpose === "FIND_EMAIL") {
      setFoundUsername(result.data.username ?? "")
      setFoundEmail(result.data.email ?? "")
      return
    }
    setCanResetPassword(Boolean(result.data.canResetPassword))
    setFoundEmail(result.data.email ?? "")
  }

  async function onResetPassword(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (!username.trim()) {
      setFormError("아이디를 입력해 주세요.")
      return
    }
    const pwErr = validatePassword(password)
    if (pwErr) {
      setFormError(pwErr)
      return
    }
    if (password !== passwordConfirm) {
      setFormError("비밀번호가 일치하지 않습니다.")
      return
    }
    setSubmitting(true)
    const result = await resetPasswordAfterVerification(token, username, password)
    setSubmitting(false)
    if (!result.ok) {
      setFormError(result.message)
      return
    }
    const loginId = foundEmail || username.trim()
    router.replace(`/login?reset=1&account=${encodeURIComponent(loginId)}`)
  }

  function goLogin(account?: string | null) {
    const q = account ? `?account=${encodeURIComponent(account)}` : ""
    router.push(`/login${q}`)
  }

  if (loadError) {
    return (
      <section className="w-full px-4 py-16 md:px-6 md:py-20">
        <AuthCard title="이메일 인증">
          <p className="text-center text-sm text-red-700">{loadError}</p>
        </AuthCard>
      </section>
    )
  }

  return (
    <section className="w-full px-4 py-16 md:px-6 md:py-20">
      <AuthCard title={purposeTitle(challenge?.purpose ?? expectedPurpose)}>
        {foundUsername !== null ? (
          <div className="space-y-4 text-center">
            <p className="text-sm text-gray-700">인증이 완료되었습니다. 가입 아이디입니다.</p>
            <label className="block text-left text-sm font-medium text-gray-800">아이디</label>
            <input
              readOnly
              value={foundUsername || ""}
              className={`${inputClass} bg-gray-50 font-semibold`}
            />
            {foundEmail && (
              <p className="text-xs text-gray-500">이메일: {foundEmail}</p>
            )}
            <button
              type="button"
              onClick={() => goLogin(foundEmail || foundUsername)}
              className="w-full rounded py-3 text-[15px] font-semibold text-white"
              style={{ backgroundColor: "#B24DFF" }}
            >
              로그인하러 가기
            </button>
          </div>
        ) : canResetPassword ? (
          <form className="space-y-4" onSubmit={onResetPassword}>
            <p className="text-center text-sm text-gray-600">
              가입한 아이디와 새 비밀번호를 입력해 주세요.
            </p>
            {formError && (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
                {formError}
              </div>
            )}
            <input
              autoComplete="username"
              placeholder="아이디"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={inputClass}
            />
            <input
              type="password"
              autoComplete="new-password"
              placeholder="새 비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClass}
            />
            <input
              type="password"
              autoComplete="new-password"
              placeholder="새 비밀번호 확인"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              className={inputClass}
            />
            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded py-3.5 text-[15px] font-semibold text-white disabled:opacity-60"
              style={{ backgroundColor: "#B24DFF" }}
            >
              {submitting ? "처리 중…" : "비밀번호 변경"}
            </button>
          </form>
        ) : (
          <form className="space-y-5" onSubmit={onSubmitCode}>
            <p className="text-center text-sm text-gray-600">
              {challenge?.maskedEmail ?? "이메일"}로 보낸 6자리 코드를 입력하세요.
              <br />
              계정을 보호하기 위해 이 코드를 공유하지 마세요.
            </p>
            {formError && (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
                {formError}
              </div>
            )}
            <div className="flex justify-center gap-2">
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => {
                    inputs.current[i] = el
                  }}
                  inputMode="numeric"
                  maxLength={1}
                  value={d}
                  onChange={(e) => setDigit(i, e.target.value)}
                  onKeyDown={(e) => onKeyDown(i, e.key)}
                  onPaste={(e) => {
                    e.preventDefault()
                    onPaste(e.clipboardData.getData("text"))
                  }}
                  className="h-12 w-10 rounded border border-gray-300 text-center text-lg font-semibold outline-none focus:border-finsight-secondary md:h-14 md:w-12"
                  aria-label={`인증 코드 ${i + 1}번째 자리`}
                />
              ))}
            </div>
            <button
              type="submit"
              disabled={submitting || challenge?.expired}
              className="w-full rounded py-3.5 text-[15px] font-semibold text-white disabled:opacity-60"
              style={{ backgroundColor: "#B24DFF" }}
            >
              {submitting ? "확인 중…" : "확인"}
            </button>
          </form>
        )}
      </AuthCard>
    </section>
  )
}
