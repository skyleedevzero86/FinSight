import type { Metadata } from "next"
import { notFound } from "next/navigation"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardEditorForm from "@/components/community/CommunityBoardEditorForm"
import { COMMUNITY_SECTION_BOARD_TYPE } from "@/data/communityBoardConfig"
import { fetchBoardDetailServer } from "@/lib/finsightBoardServer"

type Props = { params: Promise<{ id: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) return { title: "글 수정 | finsight" }
  const d = await fetchBoardDetailServer(n)
  if (!d) return { title: "글 수정 | finsight" }
  return { title: `${d.title} 수정 | Q&A | finsight` }
}

export default async function CommunityQnaEditPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n)
  if (!detail) notFound()
  const tags = (detail.hashtags ?? []).join(", ")
  return (
    <CommunityBoardLayout
      active="qna"
      heading="Q&A"
      description="서비스 이용 중 궁금한 점을 남겨 주세요."
    >
      <CommunityBoardEditorForm
        mode="edit"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.qna}
        basePath="/community/qna"
        boardId={detail.id}
        initialTitle={detail.title}
        initialContent={detail.content}
        initialTags={tags}
      />
    </CommunityBoardLayout>
  )
}
