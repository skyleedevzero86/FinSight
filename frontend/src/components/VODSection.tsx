"use client"

import Image from "next/image"
import Link from "next/link"
import { useState } from "react"
import { Play, Clock } from "lucide-react"

const vodData = [
  {
    id: 1,
    title: "11회 다시보기",
    program: "착한 사나이",
    thumbnail: "https://images.unsplash.com/photo-1611162616475-46b635cb6868?w=400&h=300&fit=crop",
    duration: "1:00:13",
    views: "15.2만"
  },
  {
    id: 2,
    title: "[500회 특집] 드디어 500회 완전체 수지를 향해 날아간 축하 메시지",
    program: "아는 형님",
    thumbnail: "https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?w=400&h=300&fit=crop",
    duration: "00:46",
    views: "8.5만"
  },
  {
    id: 3,
    title: "역사 이야기꾼들 1회 예고편",
    program: "역사 이야기꾼들",
    thumbnail: "https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=400&h=300&fit=crop",
    duration: "00:59",
    views: "3.2만"
  },
  {
    id: 4,
    title: "14회 다시보기",
    program: "디 엠파이어 법의 제국",
    thumbnail: "https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=400&h=300&fit=crop",
    duration: "45:29",
    views: "12.8만"
  },
  {
    id: 5,
    title: "아는 형님 497회 예고",
    program: "아는 형님",
    thumbnail: "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=400&h=300&fit=crop",
    duration: "01:22",
    views: "6.7만"
  },
  {
    id: 6,
    title: "140회 다시보기",
    program: "디 엠파이어 법의 제국",
    thumbnail: "https://images.unsplash.com/photo-1585951237318-9ea5e175b891?w=400&h=300&fit=crop",
    duration: "1:34:57",
    views: "9.1만"
  }
]

export default function VODSection() {
  const [activeTab, setActiveTab] = useState("추천순")
  const tabs = ["추천순", "인기순", "최신순"]

  return (
    <section className="py-12 px-4 md:px-8 max-w-7xl mx-auto">
      <div className="mb-8 grid grid-cols-2 items-center gap-x-4 gap-y-4 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
        <h2 className="col-span-1 row-start-1 justify-self-start text-2xl font-bold md:col-start-1 md:row-start-1 md:text-3xl">
          실시간 VOD
        </h2>
        <Link
          href="#"
          className="col-span-1 row-start-1 justify-self-end text-sm font-medium text-[#3c3e40] hover:text-finsight-primary hover:underline md:col-start-3 md:row-start-1"
        >
          더보기 →
        </Link>
        <div className="col-span-2 row-start-2 flex justify-center gap-3 sm:gap-4 md:col-span-1 md:col-start-2 md:row-start-1 md:justify-self-center">
          {tabs.map((tab) => (
            <button
              key={tab}
              type="button"
              onClick={() => setActiveTab(tab)}
              className={`inline-flex min-w-[4.25rem] justify-center px-3 py-2 text-center text-sm font-semibold transition sm:min-w-[4.5rem] sm:px-4 ${
                activeTab === tab
                  ? "text-finsight-primary border-b-2 border-finsight-primary"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {vodData.map((vod) => (
          <Link
            key={vod.id}
            href="#"
            className="group cursor-pointer"
          >
            <div className="relative aspect-video overflow-hidden rounded-lg mb-3">
              <Image
                src={vod.thumbnail}
                alt={vod.title}
                fill
                sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
                className="object-cover group-hover:scale-105 transition duration-300"
              />
              <div className="absolute inset-0 bg-black/20 group-hover:bg-black/40 transition flex items-center justify-center">
                <div className="bg-white/90 rounded-full p-3 opacity-80 group-hover:opacity-100 transition">
                  <Play className="w-8 h-8 text-finsight-primary fill-finsight-primary" />
                </div>
              </div>
              <div className="absolute bottom-2 right-2 bg-black/80 text-white text-xs px-2 py-1 rounded">
                {vod.duration}
              </div>
            </div>
            <div>
              <h3 className="font-semibold text-sm line-clamp-2 group-hover:text-finsight-primary transition mb-1">
                {vod.title}
              </h3>
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span className="font-medium">{vod.program}</span>
                <span>•</span>
                <span>조회수 {vod.views}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
