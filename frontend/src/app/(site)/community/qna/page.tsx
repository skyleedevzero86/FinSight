import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import { MOCK_QNA_ROWS } from "@/data/communityBoardData"

export const metadata: Metadata = {
  title: "Q&A | 커뮤니티 | finsight",
  description: "finsight Q&A",
}

export default function CommunityQnaPage() {
  return (
    <CommunityBoardLayout
      active="qna"
      heading="Q&A"
      description="서비스 이용 중 궁금한 점을 남겨 주세요."
    >
      <CommunityBoardList
        boardId="bbs_qna"
        caption="Q&A"
        totalCount={MOCK_QNA_ROWS.length + 40}
        currentPage={1}
        totalPages={5}
        rows={MOCK_QNA_ROWS}
      />
    </CommunityBoardLayout>
  )
}
