import type { Metadata } from "next"
import MyPortfolioClient from "@/components/MyPortfolioClient"

export const metadata: Metadata = {
  title: "나의 포트폴리오 | finsight",
  description: "finsight 나의 포트폴리오",
}

export default function MyPortfolioPage() {
  return <MyPortfolioClient />
}
