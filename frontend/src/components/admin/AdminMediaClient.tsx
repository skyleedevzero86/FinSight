"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { isAdminRole } from "@/lib/adminUsers"
import AdminMediaSourcePanel from "@/components/admin/AdminMediaSourcePanel"
import AdminMediaVideoPanel from "@/components/admin/AdminMediaVideoPanel"
import { FcbManageShell, FcbTabList } from "@/components/community/FcbManageShell"

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
    <FcbManageShell
      title="영상 관리"
      description="수집한 영상과 소스를 게시·숨김·동기화합니다."
    >
      <FcbTabList
        label="영상 관리 탭"
        activeKey={tab}
        onSelect={(key) => setTab(key as "videos" | "sources")}
        items={[
          { key: "videos", label: "영상" },
          { key: "sources", label: "수집 소스" },
        ]}
      />
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
    </FcbManageShell>
  )
}
