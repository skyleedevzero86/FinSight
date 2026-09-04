import type { Metadata } from "next"
import MyPostsClient from "@/components/MyPostsClient"

export const metadata: Metadata = {
  title: "나의 게시글 | finsight",
  description: "내가 작성한 글·댓글·반응 기록",
}

export default function MyPostsPage() {
  return <MyPostsClient />
}
