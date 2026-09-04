import type { Metadata } from "next"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardList from "@/components/community/CommunityBoardList"
import CommunityWriteButton from "@/components/community/CommunityWriteButton"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"
import {
  fetchBoardListServer,
  mapListToBoardRows,
} from "@/lib/finsightBoardServer"

export const metadata: Metadata = {
  title: "공지사항 | 커뮤니티 | finsight",
  description: "finsight 공지사항",
}

type PageProps = {
  searchParams: Promise<{
    page?: string
    search_type?: string
    search_value?: string
  }>
}

export default async function CommunityNoticePage({ searchParams }: PageProps) {
  const sp = await searchParams
  const pageOneBased = Math.max(1, parseInt(sp.page ?? "1", 10) || 1)
  const pageIndex = pageOneBased - 1
  const searchValue =
    typeof sp.search_value === "string" ? sp.search_value : ""
  const searchType =
    typeof sp.search_type === "string" ? sp.search_type : ""
  const pagination = await fetchBoardListServer({
    boardType: COMMUNITY_SECTION_BOARD_TYPE.notice,
    page: pageIndex,
    size: 20,
    keyword: searchValue.trim() || undefined,
  })
  const basePath = "/community/notice"
  const rows = pagination
    ? mapListToBoardRows(pagination.content, basePath)
    : []
  const totalCount = pagination?.totalElements ?? 0
  const totalPages = Math.max(1, pagination?.totalPages ?? 1)
  const offline = pagination == null

  return (
    <CommunityBoardLayout
      heading="공지사항"
      description="Finsight 소식과 운영안내를 전해드립니다."
    >
      {offline ? (
        <p className="mb-4 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          게시글 목록을 불러오지 못했습니다. 서버 연결(FINSIGHT_API_BASE_URL)을
          확인해 주세요.
        </p>
      ) : null}
      <CommunityBoardList
        boardId="bbs_notice"
        caption="공지사항"
        basePath={basePath}
        totalCount={Number(totalCount)}
        currentPage={pageOneBased}
        totalPages={totalPages}
        rows={rows}
        initialSearchType={searchType}
        initialSearchValue={searchValue}
        showWriteButton
        writeButton={
          <CommunityWriteButton
            href={`${basePath}/write`}
            requireAdmin
            label="공지 작성"
          />
        }
      />
    </CommunityBoardLayout>
  )
}
