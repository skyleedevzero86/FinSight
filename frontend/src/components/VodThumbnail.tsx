import Image from "next/image"
import { displayYoutubeThumbnail } from "@/lib/liveVod"

export default function VodThumbnail({
  videoId,
  thumbnailUrl,
  alt,
  sizes,
  className,
}: {
  videoId: string
  thumbnailUrl: string
  alt: string
  sizes: string
  className?: string
}) {
  const src = displayYoutubeThumbnail(videoId, thumbnailUrl)
  if (!src) {
    return <div className="absolute inset-0 bg-[#e6e6e6]" aria-hidden />
  }
  return <Image src={src} alt={alt} fill sizes={sizes} className={className} />
}
