import type { ReactNode } from "react"

type AuthCardProps = {
  title: string
  topBanner?: ReactNode
  children: ReactNode
}

export default function AuthCard({ title, topBanner, children }: AuthCardProps) {
  return (
    <div className="mx-auto w-full max-w-md rounded-lg border border-gray-200 bg-white px-6 py-8 shadow-sm">
      <h1 className="mb-6 text-center text-xl font-bold text-gray-900">{title}</h1>
      {topBanner}
      {children}
    </div>
  )
}
