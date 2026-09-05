"use client"

import { useEffect, useMemo, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import {
  fetchPublicMainimgItems,
  resolveMainimgUrl,
  type MainimgItem,
} from "@/lib/mainimg"

type Slide = {
  id: string
  image: string
  title: string
  subtitle: string
  linkUrl: string
}

const FALLBACK_SLIDES: Slide[] = [
  {
    id: "fallback-1",
    image: "https://images.unsplash.com/photo-1516307365426-bea591f05011?w=1920&h=600&fit=crop",
    title: "백 년의 기억",
    subtitle: "매주 금요일 밤 10시 40분",
    linkUrl: "",
  },
  {
    id: "fallback-2",
    image: "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=1920&h=600&fit=crop",
    title: "착한 사나이",
    subtitle: "매주 목요일 밤 10시 50분",
    linkUrl: "",
  },
  {
    id: "fallback-3",
    image: "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=1920&h=600&fit=crop",
    title: "finsight 뉴스룸",
    subtitle: "평일 저녁 7시 50분",
    linkUrl: "",
  },
]

function toSlides(items: MainimgItem[]): Slide[] {
  return items
    .map((item) => {
      const image = resolveMainimgUrl(item)
      if (!image) return null
      return {
        id: item.id,
        image,
        title: item.imageName,
        subtitle: item.description?.trim() || "",
        linkUrl: item.linkUrl?.trim() || "",
      }
    })
    .filter((s): s is Slide => s != null)
}

export default function MainSlider() {
  const [cmsSlides, setCmsSlides] = useState<Slide[] | null>(null)
  const [currentSlide, setCurrentSlide] = useState(0)

  const slides = useMemo(() => {
    if (cmsSlides && cmsSlides.length > 0) return cmsSlides
    return FALLBACK_SLIDES
  }, [cmsSlides])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const result = await fetchPublicMainimgItems({ size: 20 })
      if (cancelled) return
      if (!result.ok) {
        setCmsSlides([])
        return
      }
      setCmsSlides(toSlides(result.data))
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    setCurrentSlide(0)
  }, [slides])

  useEffect(() => {
    if (slides.length <= 1) return
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slides.length)
    }, 5000)
    return () => clearInterval(timer)
  }, [slides])

  const goToSlide = (index: number) => {
    setCurrentSlide(index)
  }

  const prevSlide = () => {
    setCurrentSlide((prev) => (prev - 1 + slides.length) % slides.length)
  }

  const nextSlide = () => {
    setCurrentSlide((prev) => (prev + 1) % slides.length)
  }

  return (
    <div className="relative w-full h-[300px] md:h-[500px] lg:h-[600px] overflow-hidden bg-black">
      <div className="relative w-full h-full">
        {slides.map((slide, index) => (
          <div
            key={slide.id}
            className={`absolute inset-0 transition-opacity duration-1000 ${
              index === currentSlide ? "opacity-100" : "opacity-0"
            }`}
          >
            <div className="relative w-full h-full">
              {slide.linkUrl ? (
                <a
                  href={slide.linkUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="absolute inset-0 z-[1]"
                  aria-label={`${slide.title} 바로가기`}
                />
              ) : null}
              <img
                src={slide.image}
                alt={slide.title}
                className="absolute inset-0 h-full w-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />

              <div className="absolute bottom-10 left-10 md:bottom-20 md:left-20 text-white z-[2] pointer-events-none">
                <h2 className="text-3xl md:text-5xl font-bold mb-2">{slide.title}</h2>
                {slide.subtitle ? (
                  <p className="text-lg md:text-xl">{slide.subtitle}</p>
                ) : null}
              </div>
            </div>
          </div>
        ))}
      </div>

      {slides.length > 1 ? (
        <>
          <button
            type="button"
            onClick={prevSlide}
            className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full transition"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
          <button
            type="button"
            onClick={nextSlide}
            className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full transition"
          >
            <ChevronRight className="w-6 h-6" />
          </button>

          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
            {slides.map((slide, index) => (
              <button
                key={slide.id}
                type="button"
                onClick={() => goToSlide(index)}
                className={`w-2 h-2 md:w-3 md:h-3 rounded-full transition-all ${
                  index === currentSlide
                    ? "bg-white w-8 md:w-10"
                    : "bg-white/50 hover:bg-white/70"
                }`}
              />
            ))}
          </div>
        </>
      ) : null}
    </div>
  )
}
