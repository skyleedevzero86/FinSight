"use client"

import type { FormEvent } from "react"
import Link from "next/link"
import { useId, useState } from "react"
import AuthCard from "@/components/auth/AuthCard"
import { postForgotPassword } from "@/lib/authClient"

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2.5 text-[15px] text-gray-900 placeholder:text-gray-400 outline-none transition focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

export default function FindPasswordForm() {
  const id = useId()
  const [email, setEmail] = useState("")
  const [loading, setLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(null)
    const em = email.trim()
    if (!em) {
      setFormError("이메일을 입력해 주세요.")
      return
    }
    if (!EMAIL_RE.test(em)) {
      setFormError("올바른 이메일 형식이 아닙니다.")
      return
    }
    setLoading(true)
    const result = await postForgotPassword({ email: em })
    setLoading(false)

    if (!result.ok) {
      setFormError(result.message)
      return
    }
    setSuccess(
      "재설정 안내가 가능한 경우 등록된 이메일로 발송됩니다. 메일함을 확인해 주세요.",
    )
  }

  return (
    <AuthCard title="비밀번호 찾기">
      <p className="mb-6 text-center text-sm text-gray-600">
        가입 시 사용한 이메일을 입력해 주세요.
      </p>

      {formError && (
        <div
          role="alert"
          className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {formError}
        </div>
      )}
      {success && (
        <div
          role="status"
          className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900"
        >
          {success}
        </div>
      )}

      <form className="space-y-3" onSubmit={onSubmit} noValidate>
        <input
          id={`${id}-email`}
          type="email"
          autoComplete="email"
          placeholder="이메일을 입력하세요."
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={inputClass}
        />
        <button
          type="submit"
          disabled={loading}
          className="mt-4 w-full rounded py-3.5 text-[15px] font-semibold text-white transition enabled:hover:brightness-105 disabled:opacity-60"
          style={{ backgroundColor: "#B24DFF" }}
        >
          {loading ? "처리 중…" : "비밀번호 재설정 안내 받기"}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-gray-600">
        <Link href="/login" className="font-medium underline underline-offset-2 hover:text-finsight-primary">
          로그인으로 돌아가기
        </Link>
      </p>
    </AuthCard>
  )
}
