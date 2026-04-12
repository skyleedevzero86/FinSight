import type { Metadata } from "next"
import LiveVodClient from "@/components/live-vod/LiveVodClient"
import LiveVodFinsightNav from "@/components/live-vod/LiveVodFinsightNav"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/finsight-news-nav.css"
import "@/styles/finsight-live-vod.css"

export const metadata: Metadata = {
  title: "실시간 VOD | finsight",
  description: "finsight 실시간 경제·증권 라이브 및 관련 VOD",
}

export default function LiveVodPage() {
  return (
    <>
      <div className="finsight-news-root bg-white text-[#1e1e1e]">
        <div id="wrap" className="main">
          <LiveVodFinsightNav />

          <div id="container">
            <div id="content">
              <LiveVodClient />
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
