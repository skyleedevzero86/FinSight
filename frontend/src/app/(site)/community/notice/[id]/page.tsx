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
  return { title: `${d.title} | 공지사항 | finsight` }
}

export default async function CommunityNoticeDetailPage({ params }: Props) {
  const { id } = await params
  const n = parseInt(id, 10)
  if (!Number.isFinite(n)) notFound()
  const detail = await fetchBoardDetailServer(n)
  if (!detail) notFound()
  return (
    <CommunityBoardLayout
      heading="공지사항"
      description="센터 소식과 운영 안내를 전해 드립니다."
    >
      <CommunityBoardDetail detail={detail} basePath="/community/notice" />
    </CommunityBoardLayout>
  )
}
