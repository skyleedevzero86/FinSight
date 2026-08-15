"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { User } from "lucide-react"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { authProviderLabel } from "@/lib/authSession"
import type { AuthProvider } from "@/lib/finsightToken"

function MyInfoAvatar({
  src,
  provider,
}: {
  src: string | null
  provider: AuthProvider
}) {
  const [broken, setBroken] = useState(false)
  const useSnsPhoto = provider !== "WEB" && Boolean(src) && !broken

  if (useSnsPhoto) {
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
  const { user, ready, logout } = useAuthSession()

  useEffect(() => {
    if (ready && !user) {
      router.replace("/login")
    }
  }, [ready, user, router])

  if (!ready || !user) {
    return <div className="min-h-[40vh]" />
  }

  async function onLogout() {
    await logout()
    router.replace("/")
  }

  return (
    <section className="w-full px-4 py-12 md:px-8 md:py-16">
      <div className="mx-auto w-full max-w-3xl">
        <h1 className="mb-8 text-2xl font-bold text-gray-900">내정보</h1>
        <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm md:p-8">
          <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <MyInfoAvatar
              src={user.profileImageUrl}
              provider={user.authProvider}
            />
            <div className="min-w-0">
              <p className="truncate text-xl font-semibold text-gray-900">{user.nickname}</p>
              <p className="mt-1 truncate text-sm text-gray-500">{user.email}</p>
            </div>
          </div>

          <dl className="mt-8 divide-y divide-gray-100 border-t border-gray-100">
            <div className="flex items-center justify-between gap-4 py-4">
              <dt className="text-sm text-gray-500">이름</dt>
              <dd className="truncate text-sm font-medium text-gray-900">{user.nickname}</dd>
            </div>
            <div className="flex items-center justify-between gap-4 py-4">
              <dt className="text-sm text-gray-500">이메일</dt>
              <dd className="truncate text-sm font-medium text-gray-900">{user.email || "-"}</dd>
            </div>
            <div className="flex items-center justify-between gap-4 py-4">
              <dt className="text-sm text-gray-500">로그인</dt>
              <dd className="text-sm font-medium text-gray-900">
                {authProviderLabel(user.authProvider)}
              </dd>
            </div>
          </dl>

          <button
            type="button"
            onClick={() => void onLogout()}
            className="mt-6 w-full rounded-md border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-800 hover:bg-gray-50"
          >
            로그아웃
          </button>
        </div>
      </div>
    </section>
  )
}
