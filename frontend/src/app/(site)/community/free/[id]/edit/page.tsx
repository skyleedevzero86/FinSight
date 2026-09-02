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
  return { title: `${d.title} 수정 | 포트폴리오 공유 | finsight` }
}

export default async function CommunityFreeEditPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n)
  if (!detail) notFound()
  const tags = (detail.hashtags ?? []).join(", ")
  return (
    <CommunityBoardLayout
      heading="포트폴리오 공유"
      description="투자·포트폴리오를 공유하고 이야기 나눠 보세요."
    >
      <CommunityBoardEditorForm
        mode="edit"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.free}
        basePath="/community/free"
        boardId={detail.id}
        initialTitle={detail.title}
        initialContent={detail.content}
        initialTags={tags}
      />
    </CommunityBoardLayout>
  )
}
