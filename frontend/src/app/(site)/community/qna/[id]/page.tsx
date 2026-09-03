import type { Metadata } from "next"
import { notFound } from "next/navigation"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardDetailGate from "@/components/community/CommunityBoardDetailGate"
import { fetchBoardDetailServer } from "@/lib/finsightBoardServer"

type Props = { params: Promise<{ id: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) return { title: "게시글 | finsight" }
  const d = await fetchBoardDetailServer(n, { silent: true })
  if (!d) return { title: "Q&A | finsight" }
  return { title: `${d.title} | Q&A | finsight` }
}

export default async function CommunityQnaDetailPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n, { silent: true })
  return (
    <CommunityBoardLayout
      heading="Q&A"
      description="서비스 이용 중 궁금한 점을 남겨 주세요."
    >
      <CommunityBoardDetailGate
        boardId={n}
        basePath="/community/qna"
        initialDetail={detail}
        enableComments
      />
    </CommunityBoardLayout>
  )
}
