"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import AuthCard from "@/components/auth/AuthCard"
import { disputeEmailVerification } from "@/lib/emailVerification"

export default function EmailDisputeForm({ token }: { token: string }) {
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [suspended, setSuspended] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    disputeEmailVerification(token).then((result) => {
      if (cancelled) return
      setLoading(false)
      if (!result.ok) {
        setError(result.message)
        return
      }
      setMessage(result.data.message)
      setSuspended(Boolean(result.data.accountSuspended))
    })
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <section className="w-full px-4 py-16 md:px-6 md:py-20">
      <AuthCard title="요청하지 않은 인증">
        {loading ? (
          <p className="text-center text-sm text-gray-600">처리 중…</p>
        ) : error ? (
          <div className="space-y-4 text-center">
            <p className="text-sm text-red-700">{error}</p>
            <Link href="/" className="inline-block text-sm font-medium text-finsight-secondary underline">
              홈으로
            </Link>
          </div>
        ) : (
          <div className="space-y-4 text-center">
            <p className="text-sm text-gray-800">{message}</p>
            {suspended && (
              <p className="text-xs text-gray-500">
                안내 메일을 발송했습니다. 계정 복구는 관리자에게 문의해 주세요.
              </p>
            )}
            <Link
              href="/login"
              className="inline-flex w-full items-center justify-center rounded py-3 text-[15px] font-semibold text-white"
              style={{ backgroundColor: "#B24DFF" }}
            >
              로그인 화면으로
            </Link>
          </div>
        )}
      </AuthCard>
    </section>
  )
}
