"use client"

import type { ReactNode } from "react"
import { Apple } from "lucide-react"

type SocialButton = {
  label: string
  bgClass: string
  content: ReactNode
  showPreparingAlert?: boolean
}

const socialButtons: SocialButton[] = [
  {
    label: "카카오 로그인",
    bgClass: "bg-[#FEE500] text-[#3C1E1E]",
    content: <span className="text-base font-black">톡</span>,
  },
  {
    label: "네이버 로그인",
    bgClass: "bg-[#03C75A] text-white",
    content: <span className="text-2xl font-black leading-none">N</span>,
  },
  {
    label: "페이스북 로그인",
    bgClass: "bg-[#1877F2] text-white",
    content: <span className="text-[2rem] font-black leading-none">f</span>,
    showPreparingAlert: true,
  },
  {
    label: "애플 로그인",
    bgClass: "bg-black text-white",
    content: <Apple className="h-7 w-7 fill-current" strokeWidth={2.3} />,
    showPreparingAlert: true,
  },
]

export default function SocialLoginRow() {
  function handleClick(showPreparingAlert?: boolean) {
    if (!showPreparingAlert) return
    window.alert("서비스 준비중입니다.")
  }

  return (
    <div className="mt-10">
      <div className="relative flex items-center justify-center py-2">
        <div className="absolute left-0 right-0 top-1/2 h-px bg-gray-200" aria-hidden />
        <span className="relative bg-white px-4 text-sm text-gray-500">
          또는 간편로그인
        </span>
      </div>

      <div className="mt-5 flex items-center justify-center gap-5">
        {socialButtons.map((button) => (
          <button
            key={button.label}
            type="button"
            aria-label={button.label}
            title={button.label}
            onClick={() => handleClick(button.showPreparingAlert)}
            className={`flex h-13 w-13 items-center justify-center rounded-full shadow-sm transition hover:scale-[1.03] ${button.bgClass}`}
          >
            {button.content}
          </button>
        ))}
      </div>
    </div>
  )
}
