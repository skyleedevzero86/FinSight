import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardEditorForm from "@/components/community/CommunityBoardEditorForm"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"

export const metadata: Metadata = {
  title: "글쓰기 | 공지사항 | finsight",
  description: "finsight 공지사항 글쓰기",
}

export default function CommunityNoticeWritePage() {
  return (
    <CommunityBoardLayout
      heading="공지사항"
      description="센터 소식과 운영 안내를 전해 드립니다."
    >
      <CommunityBoardEditorForm
        mode="create"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.notice}
        basePath="/community/notice"
        sectionLabel="공지사항"
      />
    </CommunityBoardLayout>
  )
}
