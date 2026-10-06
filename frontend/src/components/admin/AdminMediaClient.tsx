"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { isAdminRole } from "@/lib/adminUsers"
import AdminMediaSourcePanel from "@/components/admin/AdminMediaSourcePanel"
import AdminMediaVideoPanel from "@/components/admin/AdminMediaVideoPanel"

export type MediaNotice = { tone: "ok" | "error"; text: string } | null

export default function AdminMediaClient() {
  const router = useRouter()
  const { user, ready } = useAuthSession()
  const allowed = Boolean(user && isAdminRole(user.role))
  const [tab, setTab] = useState<"videos" | "sources">("videos")
  const [notice, setNotice] = useState<MediaNotice>(null)
  const [blocked, setBlocked] = useState(false)

  useEffect(() => {
    if (!ready) return
    if (!user) {
      router.replace("/login")
      return
    }
    if (!isAdminRole(user.role)) setBlocked(true)
  }, [ready, user, router])

  if (!ready || !user) {
    return <p className="px-4 py-6 text-sm text-gray-500">불러오는 중...</p>
  }

  if (blocked || !allowed) {
    return (
      <div className="px-4 py-6">
        <h1 className="text-lg font-semibold text-gray-900">영상 관리</h1>
        <p className="mt-3 border border-[#e7edf5] bg-white px-4 py-6 text-sm text-gray-700">
          이 메뉴는 관리자만 사용할 수 있습니다.
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:px-5 md:py-5">
      <h1 className="text-lg font-semibold text-gray-900">영상 관리</h1>
      <div className="flex gap-2 border-b border-[#e7edf5]" role="tablist" aria-label="영상 관리 탭">
        <TabButton active={tab === "videos"} onClick={() => setTab("videos")}>
          영상
        </TabButton>
        <TabButton active={tab === "sources"} onClick={() => setTab("sources")}>
          수집 소스
        </TabButton>
      </div>
      {notice ? (
        <p
          className={[
            "border px-3 py-2 text-sm",
            notice.tone === "error" ? "border-red-200 bg-white text-red-700" : "border-[#e7edf5] bg-white text-gray-800",
          ].join(" ")}
          role="status"
        >
          {notice.text}
        </p>
      ) : null}
      {tab === "videos" ? (
        <AdminMediaVideoPanel onNotice={setNotice} onForbidden={() => setBlocked(true)} />
      ) : (
        <AdminMediaSourcePanel onNotice={setNotice} onForbidden={() => setBlocked(true)} />
      )}
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      className={[
        "border-b-2 px-3 py-2 text-sm",
        active ? "border-[#1a1f2e] font-semibold text-gray-900" : "border-transparent text-gray-500",
      ].join(" ")}
      onClick={onClick}
    >
      {children}
    </button>
  )
}
