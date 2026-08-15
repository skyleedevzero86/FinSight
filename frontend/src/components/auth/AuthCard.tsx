import type { ReactNode } from "react"
import BrandLogo from "@/components/BrandLogo"

type AuthCardProps = {
  title: string
  topBanner?: ReactNode
  children: ReactNode
}

export default function AuthCard({ title, topBanner, children }: AuthCardProps) {
  return (
    <div className="mx-auto w-full max-w-md rounded-lg border border-gray-200 bg-white px-6 py-8 shadow-sm">
      <div className="mb-6 flex flex-col items-center">
        <BrandLogo variant="auth" />
        <h1 className="text-center text-xl font-bold text-gray-900">{title}</h1>
      </div>
      {topBanner}
      {children}
    </div>
  )
}
