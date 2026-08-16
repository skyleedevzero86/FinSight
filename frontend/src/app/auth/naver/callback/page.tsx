import { Suspense } from "react"
import NaverCallbackPage from "./NaverCallbackClient"

export default function Page() {
  return (
    <Suspense fallback={<div className="flex min-h-[50vh] items-center justify-center text-sm text-gray-600">처리 중...</div>}>
      <NaverCallbackPage />
    </Suspense>
  )
}
