import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardEditorForm from "@/components/community/CommunityBoardEditorForm"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"

export const metadata: Metadata = {
  title: "글쓰기 | 포트폴리오 공유 | finsight",
  description: "finsight 포트폴리오 공유 글쓰기",
}

export default function CommunityFreeWritePage() {
  return (
    <CommunityBoardLayout
      active="free"
      heading="포트폴리오 공유"
      description="투자·포트폴리오를 공유하고 이야기 나눠 보세요."
    >
      <CommunityBoardEditorForm
        mode="create"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.free}
        basePath="/community/free"
      />
    </CommunityBoardLayout>
  )
}
