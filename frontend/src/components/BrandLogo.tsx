import Link from "next/link"
import Image from "next/image"

type BrandLogoProps = {
  className?: string
  variant?: "header" | "auth"
}

export default function BrandLogo({ className = "", variant = "header" }: BrandLogoProps) {
  if (variant === "auth") {
    return (
      <Link href="/" className={`mb-5 block w-full overflow-hidden ${className}`}>
        <Image
          src="/finsight-wordmark.png"
          alt="finsight"
          width={1200}
          height={360}
          className="h-auto w-full object-contain"
          priority
        />
      </Link>
    )
  }

  return (
    <Link href="/" className={`block shrink-0 overflow-hidden ${className}`}>
      <Image
        src="/finsight-wordmark.png"
        alt="finsight"
        width={384}
        height={112}
        className="h-12 w-auto object-contain md:h-14"
        priority
      />
    </Link>
  )
}
