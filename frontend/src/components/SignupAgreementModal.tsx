"use client"

import { X } from "lucide-react"
import { useEffect } from "react"

type Props = {
  open: boolean
  title: string
  bodyHtml: string
  onClose: () => void
}

export default function SignupAgreementModal({
  open,
  title,
  bodyHtml,
  onClose,
}: Props) {
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = "hidden"
    return () => {
      document.body.style.overflow = prev
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose()
    }
    window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[200] flex items-center justify-center p-4"
      role="presentation"
    >
      <button
        type="button"
        className="absolute inset-0 bg-black/50"
        aria-label="닫기"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="signup-agreement-modal-title"
        className="relative flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-lg bg-white shadow-xl"
      >
        <div className="flex shrink-0 items-center justify-between border-b border-gray-200 px-4 py-3 md:px-5">
          <h2
            id="signup-agreement-modal-title"
            className="pr-8 text-base font-bold text-gray-900 md:text-lg"
          >
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="absolute right-3 top-3 rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
            aria-label="닫기"
          >
            <X className="h-6 w-6" />
          </button>
        </div>
        <div
          className="signup-agreement-modal-body min-h-0 flex-1 overflow-y-auto px-4 py-4 text-[13px] leading-relaxed text-gray-800 md:px-5 md:text-sm [&_pre]:whitespace-pre-wrap [&_pre]:break-words [&_table]:w-full [&_table]:max-w-full [&_table]:border-collapse [&_table]:text-xs [&_td]:border [&_td]:border-gray-200 [&_td]:p-2 [&_th]:border [&_th]:border-gray-200 [&_th]:bg-gray-50 [&_th]:p-2"
          dangerouslySetInnerHTML={{ __html: bodyHtml }}
        />
      </div>
    </div>
  )
}
