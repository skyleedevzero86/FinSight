"use client"

import { useAuthSession } from "@/components/AuthSessionProvider"
import AdminOpsHomeClient from "@/components/myinfo/AdminOpsHomeClient"
import MyInfoHomeClient from "@/components/myinfo/MyInfoHomeClient"
import { canManageUsers } from "@/lib/adminUsers"

export default function MyInfoHomeGate() {
  const { user, ready } = useAuthSession()
  if (!ready) {
    return <p className="px-5 py-16 text-center text-sm text-slate-400">화면을 준비하는 중입니다.</p>
  }
  if (canManageUsers(user?.role)) return <AdminOpsHomeClient />
  return <MyInfoHomeClient />
}
