import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import { MOCK_FREE_ROWS } from "@/data/communityBoardData"

export const metadata: Metadata = {
  title: "포트폴리오 공유 | 커뮤니티 | finsight",
  description: "finsight 포트폴리오 공유",
}

export default function CommunityFreePage() {
  return (
    <CommunityBoardLayout
      active="free"
      heading="포트폴리오 공유"
      description="투자·포트폴리오를 공유하고 이야기 나눠 보세요."
    >
      <CommunityBoardList
        boardId="bbs_free"
        caption="포트폴리오 공유"
        totalCount={MOCK_FREE_ROWS.length + 90}
        currentPage={1}
        totalPages={8}
        rows={MOCK_FREE_ROWS}
      />
    </CommunityBoardLayout>
  )
}
