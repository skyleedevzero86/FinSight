import type { Metadata } from "next"
import { Suspense } from "react"
import LoginForm from "@/components/LoginForm"

export const metadata: Metadata = {
  title: "로그인 | finsight",
  description: "finsight 로그인",
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-[40vh]" />}>
      <LoginForm />
    </Suspense>
  )
}
