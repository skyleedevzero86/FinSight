import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import {
  MOCK_NOTICE_ROWS,
  NOTICE_CATEGORY_TABS,
} from "@/data/communityBoardData"

export const metadata: Metadata = {
  title: "공지사항 | 커뮤니티 | finsight",
  description: "finsight 공지사항",
}

type PageProps = {
  searchParams: Promise<{ cate?: string }>
}

export default async function CommunityNoticePage({ searchParams }: PageProps) {
  const { cate } = await searchParams
  const categoryTabs = NOTICE_CATEGORY_TABS.map((tab) => {
    const isAll = tab.href === "/community/notice"
    const active =
      (!cate && isAll) ||
      (cate === "notice" && tab.label === "공지") ||
      (cate === "hire" && tab.label === "채용") ||
      (cate === "etc" && tab.label === "기타")
    return { ...tab, active }
  })

  return (
    <CommunityBoardLayout
      active="notice"
      heading="공지사항"
      description="센터 소식과 운영 안내를 전해 드립니다."
    >
      <CommunityBoardList
        boardId="bbs_notice"
        caption="공지사항"
        totalCount={MOCK_NOTICE_ROWS.length + 800}
        currentPage={1}
        totalPages={12}
        categoryTabs={categoryTabs}
        rows={MOCK_NOTICE_ROWS}
      />
    </CommunityBoardLayout>
  )
}
