import type { Metadata } from "next"
import LiveVodFinsightNav from "@/components/live-vod/LiveVodFinsightNav"
import LiveVodWatchClient from "@/components/live-vod/LiveVodWatchClient"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/finsight-news-nav.css"
import "@/styles/finsight-live-vod.css"

export const metadata: Metadata = {
  title: "VOD 상세 | finsight",
  description: "finsight LIVE/VOD 영상 상세 시청",
}

export default function LiveVodWatchPage() {
  return (
    <>
      <div className="finsight-news-root bg-white text-[#1e1e1e]">
        <div id="wrap" className="main">
          <LiveVodFinsightNav />

          <div id="container">
            <div id="content">
              <LiveVodWatchClient />
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
