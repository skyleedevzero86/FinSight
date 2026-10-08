import type { Metadata } from "next"
import MyFavoritesClient from "@/components/MyFavoritesClient"

export const metadata: Metadata = {
  title: "나의 즐겨찾기 | finsight",
  description: "finsight 나의 즐겨찾기",
}

export default function MyFavoritesPage() {
  return <MyFavoritesClient />
}
