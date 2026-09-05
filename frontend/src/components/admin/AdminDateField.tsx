"use client"

import { useEffect, useRef, useState } from "react"
import { DayPicker } from "react-day-picker"
import { format, parse } from "date-fns"
import { ko } from "date-fns/locale"
import { Calendar } from "lucide-react"
import "react-day-picker/style.css"

const inputClass =
  "w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 outline-none focus:border-finsight-secondary focus:ring-1 focus:ring-finsight-secondary/40"

type AdminDateFieldProps = {
  label: string
  value: string
  onChange: (next: string) => void
  disabled?: boolean
}

function parseYmd(value: string): Date | undefined {
  const trimmed = value.trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return undefined
  const d = parse(trimmed, "yyyy-MM-dd", new Date())
  return Number.isNaN(d.getTime()) ? undefined : d
}

export default function AdminDateField({
  label,
  value,
  onChange,
  disabled,
}: AdminDateFieldProps) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const selected = parseYmd(value)

  useEffect(() => {
    if (!open) return
    function onDoc(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener("mousedown", onDoc)
    return () => document.removeEventListener("mousedown", onDoc)
  }, [open])

  return (
    <div ref={rootRef} className="relative block text-xs text-gray-600">
      {label}
      <div className="mt-1 flex gap-1">
        <button
          type="button"
          className={`${inputClass} flex flex-1 items-center justify-between text-left`}
          disabled={disabled}
          onClick={() => setOpen((v) => !v)}
        >
          <span className={selected ? "text-gray-900" : "text-gray-400"}>
            {selected ? format(selected, "yyyy-MM-dd") : "날짜 선택"}
          </span>
          <Calendar className="h-4 w-4 shrink-0 text-gray-500" aria-hidden />
        </button>
        {value ? (
          <button
            type="button"
            className="rounded border border-gray-300 bg-white px-2 text-xs text-gray-600 hover:bg-gray-50 disabled:opacity-50"
            disabled={disabled}
            onClick={() => {
              onChange("")
              setOpen(false)
            }}
          >
            지우기
          </button>
        ) : null}
      </div>
      {open ? (
        <div className="absolute left-0 z-20 mt-1 rounded border border-gray-200 bg-white p-2 shadow-lg">
          <DayPicker
            mode="single"
            locale={ko}
            selected={selected}
            defaultMonth={selected}
            onSelect={(day) => {
              if (!day) {
                onChange("")
                return
              }
              onChange(format(day, "yyyy-MM-dd"))
              setOpen(false)
            }}
          />
        </div>
      ) : null}
    </div>
  )
}
