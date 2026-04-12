"use client"

import Image from "next/image"
import Link from "next/link"
import { useState } from "react"
import { Calendar, Clock } from "lucide-react"

const programCategories = ["드라마", "예능", "교양", "시사", "보도"]

const programData = {
  드라마: [
    {
      id: 1,
      title: "finsight 뉴스룸",
      image: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&h=450&fit=crop",
      time: "평일 월~금 18:30, 토~일 18:20"
    },
    {
      id: 2,
      title: "아침&",
      image: "https://images.unsplash.com/photo-1495020689067-958852a7765e?w=800&h=450&fit=crop",
      time: "평일 월~금 07:30"
    },
    {
      id: 3,
      title: "이가혁 라이브",
      image: "https://images.unsplash.com/photo-1581094271901-8022df4466f9?w=800&h=450&fit=crop",
      time: "평일 월~금 17:00"
    }
  ],
  예능: [
    {
      id: 4,
      title: "톡파원 25시",
      image: "https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=400&h=300&fit=crop",
      time: "매주 목요일 20:50"
    },
    {
      id: 5,
      title: "K-909",
      image: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&h=300&fit=crop",
      time: "매주 토요일 16:40"
    },
    {
      id: 6,
      title: "개훌륭",
      image: "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=400&h=300&fit=crop",
      time: "매주 월요일 19:10"
    }
  ],
  교양: [
    {
      id: 7,
      title: "차이나는 클라스",
      image: "https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?w=400&h=300&fit=crop",
      time: "매주 일요일 10:50"
    },
    {
      id: 8,
      title: "한문철의 블랙박스 리뷰",
      image: "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=400&h=300&fit=crop",
      time: "매주 목요일 20:50"
    },
    {
      id: 9,
      title: "히스토리 텔러",
      image: "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=400&h=300&fit=crop",
      time: "매주 수요일 20:50"
    }
  ]
}

export default function ProgramSection() {
  const [activeCategory, setActiveCategory] = useState("드라마")

  return (
    <section className="py-12 px-4 md:px-8 max-w-7xl mx-auto bg-white">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl md:text-3xl font-bold">finsight 프로그램</h2>
        <Link href="#" className="text-finsight-primary hover:underline text-sm">
          전체 프로그램 →
        </Link>
      </div>

      <div className="flex gap-6 mb-8 border-b border-gray-200">
        {programCategories.map((category) => (
          <button
            key={category}
            onClick={() => setActiveCategory(category)}
            className={`pb-3 px-2 font-semibold transition relative ${
              activeCategory === category
                ? "text-finsight-primary"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            {category}
            {activeCategory === category && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-finsight-primary" />
            )}
          </button>
        ))}
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {(programData[activeCategory as keyof typeof programData] || programData["드라마"]).map((program) => (
          <Link
            key={program.id}
            href="#"
            className="group"
          >
            <div className="relative aspect-video overflow-hidden rounded-lg mb-3 bg-gray-100">
              <Image
                src={program.image}
                alt={program.title}
                fill
                className="object-cover group-hover:scale-105 transition duration-300"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
              <div className="absolute bottom-3 left-3 text-white">
                <h3 className="font-bold text-lg mb-1">{program.title}</h3>
                <div className="flex items-center gap-1 text-xs">
                  <Clock className="w-3 h-3" />
                  <span>{program.time}</span>
                </div>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
