import type { Metadata } from "next"
import FinsightNewsMain from "@/components/news/FinsightNewsMain"
import FinsightNewsNav from "@/components/news/FinsightNewsNav"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/finsight-news-nav.css"
import "@/styles/finsight-news-pc-main.css"
import "@/styles/finsight-news-slick.css"

export const metadata: Metadata = {
  title: "finsight 뉴스",
  description: "finsight 뉴스 메인",
}

export default function NewsPage() {
  return (
    <>
      <div className="finsight-news-root bg-white text-[#1e1e1e]">
        <div id="wrap" className="main">
          <FinsightNewsNav />

          <div id="container">
            <div id="content">
              <FinsightNewsMain />
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
