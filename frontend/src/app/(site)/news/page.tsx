import type { Metadata } from "next"
import { Suspense } from "react"
import StoredNewsList from "@/components/news/StoredNewsList"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/finsight-news-nav.css"
import "@/styles/finsight-news-pc-main.css"
import "@/styles/section-news.css"

export const metadata: Metadata = {
  title: "finsight 뉴스",
  description: "finsight 뉴스 메인",
}

export default function NewsPage() {
  return (
    <>
      <div className="finsight-news-root bg-white text-[#1e1e1e]">
        <div id="wrap" className="main">
          <div id="container">
            <div id="content">
              <Suspense fallback={<p className="px-4 py-10 text-center text-sm text-gray-500">뉴스를 불러오는 중입니다.</p>}>
                <StoredNewsList />
              </Suspense>
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-gray-200 bg-finsight-light py-4">
        <VODBannersBar />
      </div>
    </>
  )
}
