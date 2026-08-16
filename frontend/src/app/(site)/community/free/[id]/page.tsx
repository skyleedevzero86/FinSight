import type { Metadata } from "next"
import { notFound } from "next/navigation"
import CommunityBoardLayout from "@/components/community/CommunityBoardLayout"
import CommunityBoardDetail from "@/components/community/CommunityBoardDetail"
import { fetchBoardDetailServer } from "@/lib/finsightBoardServer"

type Props = { params: Promise<{ id: string }> }

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) return { title: "게시글 | finsight" }
  const d = await fetchBoardDetailServer(n)
  if (!d) return { title: "게시글 | finsight" }
  return { title: `${d.title} | 포트폴리오 공유 | finsight` }
}

export default async function CommunityFreeDetailPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n)
  if (!detail) notFound()
  return (
    <CommunityBoardLayout
      active="free"
      heading="포트폴리오 공유"
      description="투자·포트폴리오를 공유하고 이야기 나눠 보세요."
    >
      <CommunityBoardDetail detail={detail} basePath="/community/free" />
    </CommunityBoardLayout>
  )
}
