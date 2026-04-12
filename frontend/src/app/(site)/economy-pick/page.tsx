import type { Metadata } from "next"
import EconomyPickBody from "@/components/economy-pick/EconomyPickBody"
import { VODBannersBar } from "@/components/VODBannersBar"
import "@/styles/economy-pick-terminal.css"

export const metadata: Metadata = {
  title: "경제 Pick | finsight",
  description: "종목·차트·티커·종목정보를 한 화면에 모은 트레이딩형 경제 Pick",
}

export default function EconomyPickPage() {
  return (
    <>
      <EconomyPickBody />
      <div className="border-t border-gray-200 bg-finsight-light py-4">
        <VODBannersBar />
      </div>
    </>
  )
}
