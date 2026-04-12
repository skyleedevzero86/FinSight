"use client"

import { useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"

const scheduleData = [
  {
    id: 1,
    time: "지금 5시 -",
    endTime: "밤 6시 30분",
    title: "착한 사나이 13회",
    isLive: false
  },
  {
    id: 2,
    time: "밤 6시 30분 -",
    endTime: "밤 7시 50분",
    title: "finsight 뉴스룸",
    isLive: false
  },
  {
    id: 3,
    time: "밤 7시 50분 -",
    endTime: "밤 8시 50분",
    title: "사건반장",
    isLive: false
  },
  {
    id: 4,
    time: "밤 8시 50분 -",
    endTime: "밤 10시",
    title: "착한 사나이 13회",
    isLive: false
  },
  {
    id: 5,
    time: "밤 10시 -",
    endTime: "밤 11시 20분",
    title: "착한 사나이 14회(최종회)",
    isOnAir: true,
    isLive: true
  },
  {
    id: 6,
    time: "밤 11시 20분 -",
    endTime: "밤 0시 40분",
    title: "이혼숙려캠프 52회 (재)",
    isLive: false
  },
  {
    id: 7,
    time: "밤 0시 40분 -",
    endTime: "새벽 1시 30분",
    title: "중독자들 50회",
    isLive: false
  },
  {
    id: 8,
    time: "새벽 1시 30분 -",
    endTime: "새벽 2시 50분",
    title: "비욘드 더 바 7회",
    isLive: false
  }
]

export default function OnAirSchedule() {
  const [cardsPerView, setCardsPerView] = useState(4)
  const [currentPage, setCurrentPage] = useState(0)

  useEffect(() => {
    const updateCardsPerView = () => {
      if (window.innerWidth < 768) {
        setCardsPerView(1)
        return
      }
      if (window.innerWidth < 1200) {
        setCardsPerView(2)
        return
      }
      setCardsPerView(4)
    }

    updateCardsPerView()
    window.addEventListener("resize", updateCardsPerView)
    return () => window.removeEventListener("resize", updateCardsPerView)
  }, [])

  const pages = useMemo(() => {
    const grouped: typeof scheduleData[] = []
    for (let i = 0; i < scheduleData.length; i += cardsPerView) {
      grouped.push(scheduleData.slice(i, i + cardsPerView))
    }
    return grouped
  }, [cardsPerView])

  useEffect(() => {
    setCurrentPage((prev) => Math.min(prev, Math.max(pages.length - 1, 0)))
  }, [pages.length])

  useEffect(() => {
    if (pages.length <= 1) return

    const timer = setInterval(() => {
      setCurrentPage((prev) => (prev + 1) % pages.length)
    }, 4500)

    return () => clearInterval(timer)
  }, [pages.length])

  const goPrev = () => {
    setCurrentPage((prev) => (prev - 1 + pages.length) % pages.length)
  }

  const goNext = () => {
    setCurrentPage((prev) => (prev + 1) % pages.length)
  }

  return (
    <section className="bg-gray-50 py-8">
      <div className="max-w-7xl mx-auto px-4 md:px-8">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-2xl md:text-3xl font-bold">인기뉴스</h3>
          <div className="flex gap-2">
            <button
              onClick={goPrev}
              className="p-1 rounded-full transition bg-white hover:bg-gray-100 text-gray-700"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              onClick={goNext}
              className="p-1 rounded-full transition bg-white hover:bg-gray-100 text-gray-700"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="relative">
          <div className="overflow-hidden">
            <div
              className="flex transition-transform duration-700 ease-in-out"
              style={{ transform: `translateX(-${currentPage * 100}%)` }}
            >
              {pages.map((page, pageIndex) => (
                <div key={pageIndex} className="w-full shrink-0">
                  <div className="grid gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-4">
                    {page.map((program) => (
                      <div
                        key={program.id}
                        className={`p-4 rounded-lg transition ${
                          program.isOnAir
                            ? "bg-finsight-primary text-white"
                            : "bg-white hover:shadow-md"
                        }`}
                      >
                        {program.isOnAir && (
                          <div className="inline-flex items-center gap-2 mb-2">
                            <span className="bg-red-500 text-white text-xs px-2 py-1 rounded-full font-semibold">
                              ON AIR
                            </span>
                          </div>
                        )}
                        <div className="text-sm mb-1">
                          <span className="font-medium">{program.time}</span>
                          <span className={program.isOnAir ? "text-white/80" : "text-gray-500"}>
                            {" "}{program.endTime}
                          </span>
                        </div>
                        <h4 className={`font-semibold ${program.isOnAir ? "" : "text-gray-900"}`}>
                          {program.title}
                        </h4>
                        {program.isLive && !program.isOnAir && (
                          <span className="inline-block mt-2 text-xs text-finsight-primary font-semibold">
                            생방송
                          </span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {pages.length > 1 && (
            <div className="mt-4 flex items-center justify-center gap-2">
              {pages.map((_, index) => (
                <button
                  key={index}
                  onClick={() => setCurrentPage(index)}
                  className={`h-2 rounded-full transition-all ${
                    index === currentPage ? "w-6 bg-finsight-primary" : "w-2 bg-gray-300"
                  }`}
                  aria-label={`${index + 1}번 편성표 페이지로 이동`}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
