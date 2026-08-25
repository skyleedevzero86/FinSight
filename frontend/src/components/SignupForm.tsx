"use client"

import type { FormEvent } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Eye, EyeOff } from "lucide-react"
import { useCallback, useEffect, useId, useMemo, useState } from "react"
import SignupAgreementModal from "@/components/SignupAgreementModal"
import BrandLogo from "@/components/BrandLogo"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { requestEmailVerification, saveSignupDraft, savePendingVerification, loadSignupDraft, isEmailMarkedVerified } from "@/lib/emailVerification"
import {
  postRegister,
  validateEmail,
  validateNickname,
  validatePassword,
  validateSignupForm,
  validateUsername,
  WATCHLIST_CATEGORIES,
  type FieldErrors,
  type TargetCategory,
} from "@/lib/registration"

const baseInputClass =
  "w-full rounded border bg-white px-3 py-2.5 text-[15px] text-gray-900 placeholder:text-gray-400 outline-none transition focus:ring-1"

function inputClassName(error?: string) {
  return [
    baseInputClass,
    error
      ? "border-red-500 focus:border-red-500 focus:ring-red-200/60"
      : "border-gray-300 focus:border-finsight-secondary focus:ring-finsight-secondary/40",
  ].join(" ")
}

type SignupFormProps = {
  termsModalTitle: string
  termsModalBodyHtml: string
  privacyModalTitle: string
  privacyModalBodyHtml: string
}

export default function SignupForm({
  termsModalTitle,
  termsModalBodyHtml,
  privacyModalTitle,
  privacyModalBodyHtml,
}: SignupFormProps) {
  const router = useRouter()
  const id = useId()
  const { user, ready } = useAuthSession()
  const [username, setUsername] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [passwordConfirm, setPasswordConfirm] = useState("")
  const [nickname, setNickname] = useState("")
  const [watchlist, setWatchlist] = useState<TargetCategory[]>([])

  const [showPw, setShowPw] = useState(false)
  const [showPw2, setShowPw2] = useState(false)
  const [agreeTerms, setAgreeTerms] = useState(false)
  const [agreePrivacy, setAgreePrivacy] = useState(false)
  const [agreeMarketing, setAgreeMarketing] = useState(false)
  const [openModal, setOpenModal] = useState<null | "terms" | "privacy">(null)

  const [touched, setTouched] = useState<Record<string, boolean>>({})
  const [submitAttempted, setSubmitAttempted] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    if (ready && user) {
      router.replace("/")
    }
  }, [ready, user, router])
  const [loading, setLoading] = useState(false)

  const [emailVerified, setEmailVerified] = useState(false)
  const [verifyLoading, setVerifyLoading] = useState(false)
  const [verifyHint, setVerifyHint] = useState<string | null>(null)
  const [verifyReminderOpen, setVerifyReminderOpen] = useState(false)

  useEffect(() => {
    const draft = loadSignupDraft()
    if (!draft) return
    setUsername(draft.username)
    setEmail(draft.email)
    setNickname(draft.nickname)
    if (isEmailMarkedVerified(draft.email, "SIGNUP")) {
      setEmailVerified(true)
      setVerifyHint("이메일 인증이 완료되었습니다. 회원가입을 진행해 주세요.")
    }
  }, [])

  const allChecked = agreeTerms && agreePrivacy && agreeMarketing

  const passwordHints = useMemo(() => {
    return {
      len: password.length >= 8,
      letter: /[A-Za-z]/.test(password),
      digit: /\d/.test(password),
      special: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>\/?]/.test(password),
    }
  }, [password])

  const toggleAll = useCallback(() => {
    const next = !allChecked
    setAgreeTerms(next)
    setAgreePrivacy(next)
    setAgreeMarketing(next)
  }, [allChecked])

  const blur = (name: string) => {
    setTouched((t) => ({ ...t, [name]: true }))
    setFieldErrors((f) => {
      const next = { ...f }
      if (name === "username")
        next.username = validateUsername(username) ?? undefined
      if (name === "email") next.email = validateEmail(email) ?? undefined
      if (name === "password") {
        next.password = validatePassword(password) ?? undefined
        next.passwordConfirm =
          password !== passwordConfirm
            ? "비밀번호가 일치하지 않습니다."
            : undefined
      }
      if (name === "passwordConfirm") {
        next.passwordConfirm =
          password !== passwordConfirm
            ? "비밀번호가 일치하지 않습니다."
            : undefined
      }
      if (name === "nickname")
        next.nickname = validateNickname(nickname) ?? undefined
      return next
    })
  }

  const onChangeUsername = (v: string) => {
    setUsername(v)
    if (touched.username || submitAttempted) {
      const e = validateUsername(v)
      setFieldErrors((f) => ({ ...f, username: e ?? undefined }))
    }
  }
  const onChangeEmail = (v: string) => {
    setEmail(v)
    if (isEmailMarkedVerified(v, "SIGNUP")) {
      setEmailVerified(true)
      setVerifyHint("이메일 인증이 완료되었습니다. 회원가입을 진행해 주세요.")
    } else {
      setEmailVerified(false)
      setVerifyHint(null)
    }
    if (touched.email || submitAttempted) {
      const e = validateEmail(v)
      setFieldErrors((f) => ({ ...f, email: e ?? undefined }))
    }
  }
  const onChangePassword = (v: string) => {
    setPassword(v)
    if (touched.password || submitAttempted) {
      const e = validatePassword(v)
      setFieldErrors((f) => ({
        ...f,
        password: e ?? undefined,
        passwordConfirm:
          v !== passwordConfirm ? "비밀번호가 일치하지 않습니다." : undefined,
      }))
    }
  }
  const onChangePasswordConfirm = (v: string) => {
    setPasswordConfirm(v)
    if (touched.passwordConfirm || submitAttempted) {
      setFieldErrors((f) => ({
        ...f,
        passwordConfirm:
          password !== v ? "비밀번호가 일치하지 않습니다." : undefined,
      }))
    }
  }
  const onChangeNickname = (v: string) => {
    setNickname(v)
    if (touched.nickname || submitAttempted) {
      const e = validateNickname(v)
      setFieldErrors((f) => ({ ...f, nickname: e ?? undefined }))
    }
  }

  function toggleWatch(cat: TargetCategory) {
    setWatchlist((w) =>
      w.includes(cat) ? w.filter((x) => x !== cat) : [...w, cat],
    )
  }

  const agreementsOk = agreeTerms && agreePrivacy
  const canSubmit = agreementsOk && !loading

  useEffect(() => {
    if (!verifyReminderOpen) return
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key === "Escape") setVerifyReminderOpen(false)
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [verifyReminderOpen])

  async function handleEmailVerify() {
    setFormError(null)
    setVerifyHint(null)
    const em = email.trim()
    const err = validateEmail(em)
    if (err) {
      setFieldErrors((f) => ({ ...f, email: err }))
      return
    }
    setVerifyLoading(true)
    saveSignupDraft({
      username,
      email: em,
      nickname,
    })
    savePendingVerification(em, "SIGNUP")
    const result = await requestEmailVerification(em, "SIGNUP")
    setVerifyLoading(false)
    if (!result.ok) {
      setEmailVerified(false)
      setFormError(result.message ?? "이메일 인증에 실패했습니다.")
      return
    }
    router.push(`/verify/${encodeURIComponent(result.data.challengeToken)}`)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitAttempted(true)
    setFormError(null)
    const nextErrors = validateSignupForm({
      username,
      email,
      password,
      passwordConfirm,
      nickname,
    })
    setFieldErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return
    if (!agreementsOk) {
      setFormError("필수 약관에 동의해 주세요.")
      return
    }
    if (!emailVerified) {
      setVerifyReminderOpen(true)
      return
    }

    setLoading(true)
    const result = await postRegister({
      username: username.trim(),
      email: email.trim(),
      password,
      nickname: nickname.trim() || undefined,
      watchlist: watchlist.length ? watchlist : undefined,
    })
    setLoading(false)

    if (result.ok) {
      router.push("/login?registered=1")
      return
    }
    setFormError(result.message)
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col items-center px-4 py-8 md:py-12">
      <SignupAgreementModal
        open={openModal === "terms"}
        title={termsModalTitle}
        bodyHtml={termsModalBodyHtml}
        onClose={() => setOpenModal(null)}
      />
      <SignupAgreementModal
        open={openModal === "privacy"}
        title={privacyModalTitle}
        bodyHtml={privacyModalBodyHtml}
        onClose={() => setOpenModal(null)}
      />

      {verifyReminderOpen && (
        <div
          className="fixed inset-0 z-[250] flex items-center justify-center p-4"
          role="presentation"
        >
          <button
            type="button"
            className="absolute inset-0 bg-black/50"
            aria-label="닫기"
            onClick={() => setVerifyReminderOpen(false)}
          />
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="verify-reminder-title"
            className="relative z-10 w-full max-w-sm rounded-lg bg-white px-6 py-6 shadow-xl"
          >
            <p
              id="verify-reminder-title"
              className="text-center text-[15px] leading-relaxed text-gray-900"
            >
              이메일 인증을 먼저 완료해 주세요.
              <br />
              <span className="mt-2 block text-sm text-gray-600">
                <strong className="text-gray-800">인증하기</strong> 버튼을 누르면
                메일로 검증 코드가 발송되고, 코드 입력 페이지로 이동합니다.
              </span>
            </p>
            <button
              type="button"
              onClick={() => setVerifyReminderOpen(false)}
              className="mt-5 w-full rounded-md bg-finsight-primary py-2.5 text-sm font-semibold text-white transition hover:opacity-95"
            >
              확인
            </button>
          </div>
        </div>
      )}

      <div className="w-full max-w-[460px] rounded-lg border border-gray-200/80 bg-white px-5 py-8 shadow-sm md:px-8 md:py-10">
        <div className="mb-8 flex flex-col items-center">
          <BrandLogo variant="auth" className="mb-6" />
          <h1 className="text-center text-xl font-bold text-gray-900 md:text-2xl">
            회원가입
          </h1>
        </div>

        {formError && (
          <div
            role="alert"
            className="mb-6 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
          >
            {formError}
          </div>
        )}

        <form className="space-y-7" onSubmit={onSubmit} noValidate>
          <section className="space-y-1.5">
            <label
              htmlFor={`${id}-username`}
              className="block text-sm font-medium text-gray-800"
            >
              사용자명 <span className="text-red-500">*</span>
            </label>
            <input
              id={`${id}-username`}
              name="username"
              autoComplete="username"
              placeholder="3~50자 (로그인 ID)"
              value={username}
              onChange={(e) => onChangeUsername(e.target.value)}
              onBlur={() => blur("username")}
              aria-invalid={Boolean(fieldErrors.username)}
              className={inputClassName(fieldErrors.username)}
            />
            {fieldErrors.username && (
              <p className="text-xs text-red-600">{fieldErrors.username}</p>
            )}
          </section>

          <section className="space-y-1.5">
            <label
              htmlFor={`${id}-email`}
              className="block text-sm font-medium text-gray-800"
            >
              이메일 <span className="text-red-500">*</span>
            </label>
            <input
              id={`${id}-email`}
              name="email"
              type="email"
              autoComplete="email"
              placeholder="이메일을 입력하세요."
              value={email}
              onChange={(e) => onChangeEmail(e.target.value)}
              onBlur={() => blur("email")}
              aria-invalid={Boolean(fieldErrors.email)}
              className={inputClassName(fieldErrors.email)}
            />
            {fieldErrors.email && (
              <p className="text-xs text-red-600">{fieldErrors.email}</p>
            )}
            <button
              type="button"
              onClick={handleEmailVerify}
              disabled={verifyLoading || emailVerified}
              className="mt-2 w-full rounded border border-gray-300 bg-white py-2.5 text-sm font-medium text-gray-600 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {emailVerified ? "인증 완료" : verifyLoading ? "요청 중…" : "인증하기"}
            </button>
            {emailVerified && verifyHint && (
              <p className="mt-2 text-xs font-medium text-emerald-700">
                {verifyHint}
              </p>
            )}
          </section>

          <section className="space-y-2">
            <label className="block text-sm font-medium text-gray-800">
              비밀번호 <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <input
                type={showPw ? "text" : "password"}
                autoComplete="new-password"
                placeholder="최소 8자, 영문·숫자·특수문자 포함"
                value={password}
                onChange={(e) => onChangePassword(e.target.value)}
                onBlur={() => blur("password")}
                aria-invalid={Boolean(fieldErrors.password)}
                className={`${inputClassName(fieldErrors.password)} pr-11`}
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
            {fieldErrors.password && (
              <p className="text-xs text-red-600">{fieldErrors.password}</p>
            )}
            <ul className="flex flex-wrap gap-x-3 gap-y-1 text-[11px] text-gray-500">
              <li className={passwordHints.len ? "text-emerald-600" : ""}>
                {passwordHints.len ? "✓" : "○"} 8자 이상
              </li>
              <li className={passwordHints.letter ? "text-emerald-600" : ""}>
                {passwordHints.letter ? "✓" : "○"} 영문
              </li>
              <li className={passwordHints.digit ? "text-emerald-600" : ""}>
                {passwordHints.digit ? "✓" : "○"} 숫자
              </li>
              <li className={passwordHints.special ? "text-emerald-600" : ""}>
                {passwordHints.special ? "✓" : "○"} 특수문자
              </li>
            </ul>
            <div className="relative pt-1">
              <input
                type={showPw2 ? "text" : "password"}
                autoComplete="new-password"
                placeholder="비밀번호를 재입력하세요."
                value={passwordConfirm}
                onChange={(e) => onChangePasswordConfirm(e.target.value)}
                onBlur={() => blur("passwordConfirm")}
                aria-invalid={Boolean(fieldErrors.passwordConfirm)}
                className={`${inputClassName(fieldErrors.passwordConfirm)} pr-11`}
              />
              <button
                type="button"
                onClick={() => setShowPw2((v) => !v)}
                className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                aria-label={showPw2 ? "비밀번호 숨기기" : "비밀번호 보기"}
              >
                {showPw2 ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
            </div>
            {fieldErrors.passwordConfirm && (
              <p className="text-xs text-red-600">{fieldErrors.passwordConfirm}</p>
            )}
          </section>

          <section className="space-y-1.5">
            <label
              htmlFor={`${id}-nickname`}
              className="block text-sm font-medium text-gray-800"
            >
              닉네임 <span className="text-gray-400">(선택)</span>
            </label>
            <input
              id={`${id}-nickname`}
              name="nickname"
              autoComplete="nickname"
              placeholder="최대 50자"
              value={nickname}
              onChange={(e) => onChangeNickname(e.target.value)}
              onBlur={() => blur("nickname")}
              aria-invalid={Boolean(fieldErrors.nickname)}
              className={inputClassName(fieldErrors.nickname)}
            />
            {fieldErrors.nickname && (
              <p className="text-xs text-red-600">{fieldErrors.nickname}</p>
            )}
          </section>

          <section className="space-y-2">
            <span className="block text-sm font-medium text-gray-800">
              관심 타깃 카테고리 <span className="text-gray-400">(선택)</span>
            </span>
            <div className="flex flex-wrap gap-2">
              {WATCHLIST_CATEGORIES.map(({ value, label }) => {
                const on = watchlist.includes(value)
                return (
                  <button
                    key={value}
                    type="button"
                    onClick={() => toggleWatch(value)}
                    className={[
                      "rounded-full border px-3 py-1.5 text-xs font-medium transition",
                      on
                        ? "border-finsight-secondary bg-finsight-secondary/15 text-finsight-primary"
                        : "border-gray-200 bg-gray-50 text-gray-600 hover:border-gray-300",
                    ].join(" ")}
                  >
                    {label}
                  </button>
                )
              })}
            </div>
          </section>

          <section className="space-y-4">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="checkbox"
                checked={allChecked}
                onChange={toggleAll}
                className="signup-checkbox mt-0.5"
              />
              <span className="text-[15px] font-medium text-gray-900">
                모두 동의합니다.
              </span>
            </label>

            <ul className="ml-1 space-y-3 border-l-2 border-gray-100 pl-4">
              <li className="flex items-start justify-between gap-2">
                <label className="flex flex-1 cursor-pointer items-start gap-3">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(e) => setAgreeTerms(e.target.checked)}
                    className="signup-checkbox mt-0.5"
                  />
                  <span className="text-[14px] leading-snug text-gray-800">
                    [필수] 이용약관 동의
                  </span>
                </label>
                <button
                  type="button"
                  onClick={() => setOpenModal("terms")}
                  className="shrink-0 text-xs text-gray-500 underline underline-offset-2 hover:text-gray-800"
                >
                  내용보기
                </button>
              </li>
              <li className="flex items-start justify-between gap-2">
                <label className="flex flex-1 cursor-pointer items-start gap-3">
                  <input
                    type="checkbox"
                    checked={agreePrivacy}
                    onChange={(e) => setAgreePrivacy(e.target.checked)}
                    className="signup-checkbox mt-0.5"
                  />
                  <span className="text-[14px] leading-snug text-gray-800">
                    [필수] 개인정보 수집 및 활용 동의
                  </span>
                </label>
                <button
                  type="button"
                  onClick={() => setOpenModal("privacy")}
                  className="shrink-0 text-xs text-gray-500 underline underline-offset-2 hover:text-gray-800"
                >
                  내용보기
                </button>
              </li>
              <li>
                <label className="flex cursor-pointer items-start gap-3">
                  <input
                    type="checkbox"
                    checked={agreeMarketing}
                    onChange={(e) => setAgreeMarketing(e.target.checked)}
                    className="signup-checkbox mt-0.5"
                  />
                  <span className="text-[14px] leading-snug text-gray-800">
                    [선택] 마케팅 수신 동의
                  </span>
                </label>
              </li>
            </ul>

            <p className="text-xs text-gray-500">*14세 이상만 가입 가능합니다.</p>
          </section>

          {!emailVerified && (
            <p className="text-center text-xs text-amber-800/90">
              이메일 <strong>인증하기</strong> 완료 후 회원가입이 가능합니다.
            </p>
          )}

          <button
            type="submit"
            disabled={!canSubmit}
            className="w-full rounded py-3.5 text-[15px] font-semibold text-white transition enabled:hover:brightness-105 enabled:active:brightness-95 disabled:cursor-not-allowed disabled:opacity-50"
            style={{ backgroundColor: "#B24DFF" }}
          >
            {loading ? "처리 중…" : "회원가입"}
          </button>
        </form>

        <p className="mt-8 text-center text-sm text-gray-600">
          이미 아이디가 있으신가요?{" "}
          <Link
            href="/login"
            className="font-semibold text-gray-900 underline underline-offset-2 hover:text-finsight-primary"
          >
            로그인
          </Link>
        </p>
      </div>
    </div>
  )
}
