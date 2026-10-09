import type { Metadata } from "next"
import CommunityShareBoard from "@/components/community/CommunityShareBoard"

export const metadata: Metadata = {
  title: "포트폴리오 공유 | 커뮤니티 | finsight",
  description: "나의 포트폴리오에서 공유한 목표와 설명을 확인합니다.",
}

export default function CommunityPortfolioPage() {
  return <CommunityShareBoard />
}
