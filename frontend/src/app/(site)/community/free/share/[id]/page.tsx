import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import PortfolioShareDetailClient from "@/components/community/PortfolioShareDetailClient"

export const metadata: Metadata = {
  title: "포트폴리오 공유 | 커뮤니티 | finsight",
  description: "공유 Snapshot, 반응, 댓글을 확인합니다.",
}

export default function PortfolioShareDetailPage() {
  return (
    <CommunityBoardLayout
      heading="포트폴리오 공유"
      description="공유한 자산 Snapshot과 댓글을 확인합니다."
    >
      <PortfolioShareDetailClient />
    </CommunityBoardLayout>
  )
}
