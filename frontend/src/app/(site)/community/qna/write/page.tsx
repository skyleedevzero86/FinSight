import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardEditorForm from "@/components/community/CommunityBoardEditorForm"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"

export const metadata: Metadata = {
  title: "글쓰기 | Q&A | finsight",
  description: "finsight Q&A 글쓰기",
}

export default function CommunityQnaWritePage() {
  return (
    <CommunityBoardLayout
      heading="Q&A"
      description="서비스 이용 중 궁금한 점을 남겨 주세요."
    >
      <CommunityBoardEditorForm
        mode="create"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.qna}
        basePath="/community/qna"
        sectionLabel="Q&A"
        enableVisibility
      />
    </CommunityBoardLayout>
  )
}
