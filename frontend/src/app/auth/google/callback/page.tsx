import { Suspense } from "react"
import GoogleCallbackPage from "./GoogleCallbackClient"

export default function Page() {
  return (
    <Suspense fallback={<div className="flex min-h-[50vh] items-center justify-center text-sm text-gray-600">처리 중...</div>}>
      <GoogleCallbackPage />
    </Suspense>
  )
}
