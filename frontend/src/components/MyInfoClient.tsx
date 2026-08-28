"use client"

import { useEffect, useId, useRef, useState, type FormEvent } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Eye, EyeOff, User } from "lucide-react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { authProviderLabel } from "@/lib/authSession"
import {
  changePassword,
  fetchPasswordStatus,
  fetchUserProfile,
  fetchWatchlist,
  updateProfile,
  updateWatchlist,
  uploadProfileImage,
  type PasswordStatus,
} from "@/lib/myAccount"
import { withdrawSelf } from "@/lib/adminUsers"
import {
  validateEmail,
  validateNickname,
  validatePassword,
  WATCHLIST_CATEGORIES,
  type TargetCategory,
} from "@/lib/registration"
import { FINSIGHT_FORCE_PASSWORD_KEY } from "@/lib/finsightToken"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2.5 text-[15px] text-gray-900 placeholder:text-gray-400 outline-none transition focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

function MyInfoAvatar({ src }: { src: string | null }) {
  const [broken, setBroken] = useState(false)
  const showPhoto = Boolean(src) && !broken

  if (showPhoto) {
    return (
      <img
        src={src as string}
        alt=""
        className="h-20 w-20 rounded-full object-cover bg-gray-100"
        onError={() => setBroken(true)}
      />
    )
  }

  return (
    <div className="flex h-20 w-20 items-center justify-center rounded-full bg-finsight-primary text-white">
      <User className="h-10 w-10" strokeWidth={1.8} aria-hidden />
    </div>
  )
}

export default function MyInfoClient() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const id = useId()
  const { user, ready, refresh, logout } = useAuthSession()
  const fileRef = useRef<HTMLInputElement>(null)
  const forcePassword =
    searchParams.get("password") === "required" ||
    (typeof window !== "undefined" &&
      sessionStorage.getItem(FINSIGHT_FORCE_PASSWORD_KEY) === "1")

  const [nickname, setNickname] = useState("")
  const [email, setEmail] = useState("")
  const [watchlist, setWatchlist] = useState<TargetCategory[]>([])
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [imageFile, setImageFile] = useState<File | null>(null)

  const [oldPassword, setOldPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("")
  const [showOld, setShowOld] = useState(false)
  const [showNew, setShowNew] = useState(false)

  const [passwordStatus, setPasswordStatus] = useState<PasswordStatus | null>(null)
  const [saving, setSaving] = useState(false)
  const [withdrawing, setWithdrawing] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [formOk, setFormOk] = useState<string | null>(null)

  useEffect(() => {
    if (ready && !user) {
      router.replace("/login")
    }
  }, [ready, user, router])

  useEffect(() => {
    if (!user) return
    setNickname(user.nickname)
    setEmail(user.email)
    setPreviewUrl(user.profileImageUrl)
    void (async () => {
      const [profile, list, status] = await Promise.all([
        fetchUserProfile(),
        fetchWatchlist(),
        user.authProvider === "WEB" ? fetchPasswordStatus() : Promise.resolve(null),
      ])
      if (profile) {
        setNickname(profile.nickname || user.nickname)
        setEmail(profile.email || user.email)
        setPreviewUrl(profile.profileImageUrl ?? user.profileImageUrl)
      }
      setWatchlist(list)
      setPasswordStatus(status)
    })()
  }, [user])

  if (!ready || !user) {
    return <div className="min-h-[40vh]" />
  }

  const isSns = user.authProvider !== "WEB"
  const passwordLocked = Boolean(forcePassword || passwordStatus?.changeRequired)

  async function onLogout() {
    try {
      sessionStorage.removeItem(FINSIGHT_FORCE_PASSWORD_KEY)
    } catch {
      void 0
    }
    await logout()
    router.replace("/")
  }

  async function onWithdraw() {
    if (
      !window.confirm(
        "탈퇴하면 같은 계정으로 로그인할 수 없습니다. 정말 탈퇴할까요?",
      )
    ) {
      return
    }
    setFormError(null)
    setWithdrawing(true)
    const result = await withdrawSelf()
    setWithdrawing(false)
    if (!result.ok) {
      setFormError(result.message)
      return
    }
    await onLogout()
  }

  function toggleWatch(value: TargetCategory) {
    const previous = watchlist
    const next = previous.includes(value)
      ? previous.filter((v) => v !== value)
      : [...previous, value]
    setWatchlist(next)
    setFormError(null)
    setFormOk(null)
    void (async () => {
      setSaving(true)
      try {
        const result = await updateWatchlist(next)
        if (!result.ok) {
          setWatchlist(previous)
          setFormError(result.message)
          return
        }
        setFormOk("관심 카테고리가 저장되었습니다.")
      } finally {
        setSaving(false)
      }
    })()
  }

  function onPickImage(file: File | undefined) {
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      setFormError("프로필 사진은 2MB 이하여야 합니다.")
      return
    }
    setImageFile(file)
    setPreviewUrl(URL.createObjectURL(file))
  }

  async function onSaveSns(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setFormOk(null)
    const nickErr = validateNickname(nickname)
    if (!nickname.trim()) {
      setFormError("SNS 이름을 입력해 주세요.")
      return
    }
    if (nickErr) {
      setFormError(nickErr)
      return
    }
    setSaving(true)
    try {
      if (imageFile) {
        const uploaded = await uploadProfileImage(imageFile)
        if (!uploaded.ok) {
          setFormError(uploaded.message)
          return
        }
        if (uploaded.url) setPreviewUrl(uploaded.url)
      }
      const saved = await updateProfile({ nickname: nickname.trim() })
      if (!saved.ok) {
        setFormError(saved.message)
        return
      }
      await refresh()
      setFormOk("프로필이 저장되었습니다.")
      setImageFile(null)
    } finally {
      setSaving(false)
    }
  }

  async function onSaveWeb(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setFormOk(null)

    const emailErr = validateEmail(email)
    if (emailErr) {
      setFormError(emailErr)
      return
    }

    const wantsPasswordChange = Boolean(oldPassword || newPassword || newPasswordConfirm)
    if (passwordLocked && !wantsPasswordChange) {
      setSaving(true)
      try {
        const watched = await updateWatchlist(watchlist)
        if (!watched.ok) {
          setFormError(watched.message)
          return
        }
        const saved = await updateProfile({ email: email.trim() })
        if (!saved.ok) {
          setFormError(saved.message)
          return
        }
        await refresh()
        setFormOk("관심 카테고리와 이메일이 저장되었습니다. 계속 이용하려면 비밀번호도 변경해 주세요.")
      } finally {
        setSaving(false)
      }
      return
    }

    if (wantsPasswordChange || passwordLocked) {
      const pwErr = validatePassword(newPassword)
      if (pwErr) {
        setFormError(pwErr)
        return
      }
      if (!oldPassword) {
        setFormError("현재 비밀번호를 입력해 주세요.")
        return
      }
      if (newPassword !== newPasswordConfirm) {
        setFormError("새 비밀번호 확인이 일치하지 않습니다.")
        return
      }
    }

    setSaving(true)
    try {
      if (wantsPasswordChange || passwordLocked) {
        const changed = await changePassword({
          oldPassword,
          newPassword,
          newPasswordConfirm,
        })
        if (!changed.ok) {
          setFormError(changed.message)
          return
        }
        setOldPassword("")
        setNewPassword("")
        setNewPasswordConfirm("")
        try {
          sessionStorage.removeItem(FINSIGHT_FORCE_PASSWORD_KEY)
        } catch {
          void 0
        }
        const status = await fetchPasswordStatus()
        setPasswordStatus(status)
        if (forcePassword) {
          router.replace("/my")
        }
      }

      const watched = await updateWatchlist(watchlist)
      if (!watched.ok) {
        setFormError(watched.message)
        return
      }

      const saved = await updateProfile({ email: email.trim() })
      if (!saved.ok) {
        setFormError(saved.message)
        return
      }
      await refresh()
      setFormOk("내정보가 저장되었습니다.")
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="w-full px-4 py-12 md:px-8 md:py-16">
      <div className="mx-auto w-full max-w-3xl">
        <h1 className="mb-8 text-2xl font-bold text-gray-900">내정보</h1>
        <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm md:p-8">
          {passwordLocked && !isSns ? (
            <div
              role="alert"
              className="mb-6 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-950"
            >
              비밀번호를 변경한 지 90일이 지났습니다. 계속 이용하려면 비밀번호를 바꿔 주세요.
            </div>
          ) : null}
          {passwordStatus?.changeRecommended && !passwordLocked && !isSns ? (
            <div
              role="status"
              className="mb-6 rounded-md border border-sky-200 bg-sky-50 px-3 py-2 text-sm text-sky-950"
            >
              {passwordStatus.statusMessage ||
                "비밀번호 변경 시기가 다가왔습니다. 내정보에서 비밀번호를 바꿔 주세요."}
            </div>
          ) : null}

          <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <MyInfoAvatar src={previewUrl} />
            <div className="min-w-0">
              <p className="truncate text-xl font-semibold text-gray-900">{user.nickname}</p>
              <p className="mt-1 truncate text-sm text-gray-500">{user.email}</p>
              <p className="mt-1 text-xs text-gray-400">
                {authProviderLabel(user.authProvider)} 로그인
              </p>
            </div>
          </div>

          {formError ? (
            <p className="mt-6 text-sm text-red-600" role="alert">
              {formError}
            </p>
          ) : null}
          {formOk ? (
            <p className="mt-6 text-sm text-emerald-700" role="status">
              {formOk}
            </p>
          ) : null}

          {isSns ? (
            <form className="mt-8 space-y-6" onSubmit={(e) => void onSaveSns(e)}>
              <section className="space-y-2">
                <label htmlFor={`${id}-photo`} className="block text-sm font-medium text-gray-800">
                  프로필 사진
                </label>
                <input
                  ref={fileRef}
                  id={`${id}-photo`}
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  className="block w-full text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-2 file:text-sm file:font-medium file:text-gray-800 hover:file:bg-gray-200"
                  onChange={(e) => onPickImage(e.target.files?.[0])}
                />
                <p className="text-xs text-gray-400">PNG, JPG, WEBP, GIF · 2MB 이하</p>
              </section>
              <section className="space-y-2">
                <label htmlFor={`${id}-nickname`} className="block text-sm font-medium text-gray-800">
                  SNS 이름
                </label>
                <input
                  id={`${id}-nickname`}
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  maxLength={50}
                  className={inputClass}
                />
              </section>
              <button
                type="submit"
                disabled={saving}
                className="w-full rounded-md bg-finsight-primary px-4 py-2.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-60"
              >
                {saving ? "저장 중..." : "저장"}
              </button>
            </form>
          ) : (
            <form className="mt-8 space-y-6" onSubmit={(e) => void onSaveWeb(e)}>
              <section className="space-y-2">
                <label htmlFor={`${id}-email`} className="block text-sm font-medium text-gray-800">
                  이메일
                </label>
                <input
                  id={`${id}-email`}
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={inputClass}
                />
              </section>

              <section className="space-y-3">
                <p className="text-sm font-medium text-gray-800">비밀번호</p>
                <p className="text-xs text-gray-500">90일마다 변경해야 합니다. 영문·숫자·특수문자 포함 8자 이상.</p>
                <div className="relative">
                  <input
                    type={showOld ? "text" : "password"}
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    placeholder="현재 비밀번호"
                    className={inputClass}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    onClick={() => setShowOld((v) => !v)}
                    aria-label="현재 비밀번호 표시"
                  >
                    {showOld ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <div className="relative">
                  <input
                    type={showNew ? "text" : "password"}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="새 비밀번호"
                    className={inputClass}
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    onClick={() => setShowNew((v) => !v)}
                    aria-label="새 비밀번호 표시"
                  >
                    {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <input
                  type={showNew ? "text" : "password"}
                  value={newPasswordConfirm}
                  onChange={(e) => setNewPasswordConfirm(e.target.value)}
                  placeholder="새 비밀번호 확인"
                  className={inputClass}
                  autoComplete="new-password"
                />
              </section>

              <section className="space-y-2">
                <span className="block text-sm font-medium text-gray-800">관심 타깃 카테고리</span>
                <p className="text-xs text-gray-500">선택하면 바로 저장됩니다.</p>
                <div className="flex flex-wrap gap-2">
                  {WATCHLIST_CATEGORIES.map(({ value, label }) => {
                    const on = watchlist.includes(value)
                    return (
                      <button
                        key={value}
                        type="button"
                        disabled={saving}
                        onClick={() => toggleWatch(value)}
                        className={[
                          "rounded-full border px-3 py-1.5 text-xs font-medium transition disabled:opacity-60",
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

              <button
                type="submit"
                disabled={saving}
                className="w-full rounded-md bg-finsight-primary px-4 py-2.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-60"
              >
                {saving ? "저장 중..." : "저장"}
              </button>
            </form>
          )}

          <button
            type="button"
            onClick={() => void onWithdraw()}
            disabled={withdrawing || user.role === "ADMIN"}
            className="mt-3 w-full rounded-md border border-red-200 px-4 py-2.5 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
          >
            {withdrawing ? "탈퇴 처리 중..." : "회원 탈퇴"}
          </button>
          <button
            type="button"
            onClick={() => void onLogout()}
            className="mt-3 w-full rounded-md border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-800 hover:bg-gray-50"
          >
            로그아웃
          </button>
        </div>
      </div>
    </section>
  )
}
