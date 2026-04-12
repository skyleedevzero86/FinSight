import MainSlider from "@/components/MainSlider"
import OnAirSchedule from "@/components/OnAirSchedule"
import NewsSection from "@/components/NewsSection"
import VODSection from "@/components/VODSection"
import { VODBannersBar } from "@/components/VODBannersBar"

export default function Home() {
  return (
    <>
      <MainSlider />
      <OnAirSchedule />
      <NewsSection />
      <VODSection />
      <div className="border-t border-gray-200 bg-finsight-light py-4">
        <VODBannersBar />
      </div>
    </>
  )
}
