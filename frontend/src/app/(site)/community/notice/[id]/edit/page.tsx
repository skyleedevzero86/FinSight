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
  const d = await fetchBoardDetailServer(n, { silent: true, trackView: false })
  if (!d) return { title: "글 수정 | finsight" }
  return { title: `${d.title} 수정 | 공지사항 | finsight` }
}

export default async function CommunityNoticeEditPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n, { silent: true })
  if (!detail) notFound()
  const tags = (detail.hashtags ?? []).join(", ")
  return (
    <CommunityBoardLayout
      heading="공지사항"
      description="센터 소식과 운영 안내를 전해 드립니다."
    >
      <CommunityBoardEditorForm
        mode="edit"
        boardType={COMMUNITY_SECTION_BOARD_TYPE.notice}
        basePath="/community/notice"
        boardId={detail.id}
        authorEmail={detail.authorEmail}
        initialTitle={detail.title}
        initialContent={detail.content}
        initialTags={tags}
      />
    </CommunityBoardLayout>
  )
}
