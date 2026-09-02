import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"
import {
  fetchBoardListServer,
  mapListToBoardRows,
} from "@/lib/finsightBoardServer"

export const metadata: Metadata = {
  title: "Q&A | 커뮤니티 | finsight",
  description: "finsight Q&A",
}

type PageProps = {
  searchParams: Promise<{
    page?: string
    search_type?: string
    search_value?: string
  }>
}

export default async function CommunityQnaPage({ searchParams }: PageProps) {
  const sp = await searchParams
  const pageOneBased = Math.max(1, parseInt(sp.page ?? "1", 10) || 1)
  const pageIndex = pageOneBased - 1
  const searchValue =
    typeof sp.search_value === "string" ? sp.search_value : ""
  const searchType =
    typeof sp.search_type === "string" ? sp.search_type : ""
  const pagination = await fetchBoardListServer({
    boardType: COMMUNITY_SECTION_BOARD_TYPE.qna,
    page: pageIndex,
    size: 20,
    keyword: searchValue.trim() || undefined,
  })
  const basePath = "/community/qna"
  const rows = pagination
    ? mapListToBoardRows(pagination.content, basePath)
    : []
  const totalCount = pagination?.totalElements ?? 0
  const totalPages = Math.max(1, pagination?.totalPages ?? 1)
  const offline = pagination == null

  return (
    <CommunityBoardLayout
      heading="Q&A"
      description="서비스 이용 중 궁금한 점을 남겨 주세요."
    >
      {offline ? (
        <p className="mb-4 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          게시글 목록을 불러오지 못했습니다. 서버 연결(FINSIGHT_API_BASE_URL)을
          확인해 주세요.
        </p>
      ) : null}
      <CommunityBoardList
        boardId="bbs_qna"
        caption="Q&A"
        basePath={basePath}
        totalCount={Number(totalCount)}
        currentPage={pageOneBased}
        totalPages={totalPages}
        rows={rows}
        initialSearchType={searchType}
        initialSearchValue={searchValue}
      />
    </CommunityBoardLayout>
  )
}
