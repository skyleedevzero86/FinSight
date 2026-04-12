"use client"

import { useState } from "react"

function formatYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}.${m}.${day}`
}

export default function NewsCategoryDateBar() {
  const [day, setDay] = useState(() => new Date())

  return (
    <div className="list_date wrapper">
      <div className="list_date_bar">
        <a href="#" className="btn_date date_prev txt_hide" aria-label="이전 날짜">
          이전
        </a>
        <div className="date_w">
          <label htmlFor="news-datepicker" className="blind">
            달력보기
          </label>
          <input
            type="button"
            id="news-datepicker"
            className="datepicker"
            value={formatYmd(day)}
            onClick={() => setDay((prev) => new Date(prev.getTime() - 86400000))}
          />
          <button type="button" className="ui-datepicker-trigger">
            달력보기
          </button>
        </div>
        <div className="layer_calendar" hidden aria-hidden />
        <a
          href="#"
          className="btn_date date_next txt_hide"
          style={{ display: "none" }}
          aria-hidden
        >
          다음
        </a>
      </div>
    </div>
  )
}
