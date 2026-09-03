"use client"

import Link from "next/link"
import { useAuthSession } from "@/components/AuthSessionProvider"
import { canManageUsers } from "@/lib/adminUsers"

type Props = {
  href: string
  requireAdmin?: boolean
  label?: string
}

export default function CommunityWriteButton({
  href,
  requireAdmin = false,
  label = "글쓰기",
}: Props) {
  const { user, ready } = useAuthSession()

  if (!ready) return null
  if (requireAdmin && !canManageUsers(user?.role)) return null

  if (!user) {
    const next = encodeURIComponent(href)
    return (
      <Link
        href={`/login?next=${next}`}
        className="inline-flex items-center justify-center rounded border border-finsight-primary bg-finsight-primary px-4 py-2 text-sm font-medium text-white hover:opacity-95"
      >
        {label}
      </Link>
    )
  }

  return (
    <Link
      href={href}
      className="inline-flex items-center justify-center rounded border border-finsight-primary bg-finsight-primary px-4 py-2 text-sm font-medium text-white hover:opacity-95"
    >
      {label}
    </Link>
  )
}
